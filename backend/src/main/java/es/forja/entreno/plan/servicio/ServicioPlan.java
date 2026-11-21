package es.forja.entreno.plan.servicio;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.forja.comun.api.ErrorNegocio;
import es.forja.cuenta.repositorio.UsuarioRepositorio;
import es.forja.entreno.plan.modelo.GrupoMuscular;
import es.forja.entreno.plan.modelo.GrupoMuscular.MusculoEjercicio;
import es.forja.entreno.plan.modelo.HistorialSesiones;
import es.forja.entreno.plan.modelo.PlanActual;
import es.forja.entreno.plan.modelo.PlanActual.DiaEntreno;
import es.forja.entreno.plan.modelo.PlanActual.ObjetivoEjercicio;
import es.forja.entreno.plan.modelo.PlanGenerado;
import es.forja.entreno.plan.modelo.MaquinaProgresion;
import es.forja.entreno.plan.modelo.ProgresionEjercicio;
import es.forja.entreno.plan.modelo.ReglaEjercicio;
import es.forja.entreno.plan.modelo.ReglasDia;
import es.forja.entreno.plan.modelo.ResultadoProgresion;
import es.forja.entreno.plan.modelo.SesionEjercicio;
import es.forja.entreno.plan.modelo.TrazaGuardada;
import es.forja.entreno.plan.modelo.VersionPlan;
import es.forja.entreno.plan.modelo.VersionPlan.EjercicioVersion;
import es.forja.entreno.plan.repositorio.PlanRepositorio;
import es.forja.entreno.plan.repositorio.PlanRepositorio.CabeceraPlan;
import es.forja.entreno.plan.repositorio.PlanRepositorio.FichaEjercicio;
import es.forja.entreno.plan.repositorio.PlanRepositorio.FilaEjercicio;
import es.forja.entreno.plan.repositorio.PlanRepositorio.FilaLeida;
import es.forja.entreno.rutina.modelo.PlanRutina;
import es.forja.entreno.rutina.modelo.PlanRutina.DiaPlan;
import es.forja.entreno.rutina.modelo.PlanRutina.EjercicioPlan;
import tools.jackson.databind.json.JsonMapper;

/**
 * Planes versionados (ADR 0008 §5 y §6): guarda cada plan generado como una versión inmutable y sirve el último con
 * el objetivo de carga que corresponde a lo ya entrenado.
 */
@Service
public class ServicioPlan {

	private final PlanRepositorio planes;
	private final UsuarioRepositorio usuarios;
	private final HistorialSesiones historial;
	private final PropiedadesProgresion progresion;
	private final JsonMapper json;
	private final Clock reloj;

	public ServicioPlan(PlanRepositorio planes, UsuarioRepositorio usuarios, HistorialSesiones historial,
			PropiedadesProgresion progresion, JsonMapper json, Clock reloj) {
		this.planes = planes;
		this.usuarios = usuarios;
		this.historial = historial;
		this.progresion = progresion;
		this.json = json;
		this.reloj = reloj;
	}

	/** Guarda el plan como versión nueva y lo devuelve con su uuid. */
	@Transactional
	public PlanGenerado guardar(UUID usuario, PlanRutina plan) {
		long usuarioId = idDe(usuario);
		List<EjercicioPlan> elegidos = plan.dias().stream().flatMap(d -> d.ejercicios().stream()).toList();
		if (elegidos.isEmpty()) {
			// Pasa con material que no tiene ejercicios curados (banda, fitball…) o si todo queda excluido.
			throw ErrorNegocio.noProcesable("No hay ejercicios compatibles con tu material y tus exclusiones: "
					+ "añade material o quita alguna exclusión.");
		}
		Map<String, FichaEjercicio> fichas = planes.fichas(elegidos.stream().map(EjercicioPlan::idExterno).distinct()
				.toList());
		UUID uuid = UUID.randomUUID();
		long planId = planes.insertarVersion(uuid, usuarioId, reloj.instant(), estado(plan),
				json.writeValueAsString(TrazaGuardada.de(plan)));
		for (DiaPlan dia : plan.dias()) {
			for (int orden = 0; orden < dia.ejercicios().size(); orden++) {
				EjercicioPlan e = dia.ejercicios().get(orden);
				FichaEjercicio ficha = fichas.get(e.idExterno());
				if (ficha == null) {
					throw new IllegalStateException("El generador eligió un ejercicio que no está en el catálogo.");
				}
				planes.insertarEjercicio(planId, filaDe(dia.dia(), orden, e.series(), ficha,
						cargaDeLaUltimaSesion(usuarioId, ficha.id())));
			}
		}
		return new PlanGenerado(uuid, plan);
	}

	/**
	 * El último plan del usuario. Cada ejercicio trae lo que dice la máquina de estados (RF-26) sobre su historial: el
	 * servidor manda sobre el móvil; sin sesiones, la carga inicial que se guardó con el plan.
	 */
	public PlanActual actual(UUID usuario) {
		long usuarioId = idDe(usuario);
		CabeceraPlan cabecera = planes.ultimo(usuarioId)
				.orElseThrow(() -> ErrorNegocio.noEncontrado("Todavía no has generado ningún plan."));
		List<FilaLeida> filas = planes.ejercicios(cabecera.id());
		Map<Long, List<MusculoEjercicio>> musculos = musculosDe(filas);
		Map<Integer, List<ObjetivoEjercicio>> porDia = new LinkedHashMap<>();
		for (FilaLeida leida : filas) {
			porDia.computeIfAbsent(leida.fila().dia(), d -> new ArrayList<>())
					.add(objetivo(usuarioId, leida, musculos.getOrDefault(leida.fila().ejercicioId(), List.of())));
		}
		List<DiaEntreno> dias = porDia.entrySet().stream()
				.map(e -> new DiaEntreno(e.getKey(), ReglasDia.nombre(e.getValue()), e.getValue())).toList();
		int diaDeHoy = ReglasDia.diaDeHoy(dias.stream().map(DiaEntreno::dia).toList(),
				historial.ultimoDiaDePlan(usuarioId, cabecera.id()));
		return new PlanActual(cabecera.uuid(), cabecera.creadoEn(), cabecera.estado(), diaDeHoy, dias);
	}

	/**
	 * Todas las versiones del plan con lo que se guardó de cada una, para la exportación.
	 *
	 * <p>shortcut: una consulta de ejercicios por versión; si un usuario acumula cientos, leerlas de una vez.
	 */
	public List<VersionPlan> versiones(UUID usuario) {
		List<CabeceraPlan> cabeceras = planes.versiones(idDe(usuario));
		List<VersionPlan> versiones = new ArrayList<>();
		for (int i = 0; i < cabeceras.size(); i++) {
			CabeceraPlan c = cabeceras.get(i);
			List<EjercicioVersion> ejercicios = planes.ejercicios(c.id()).stream().map(l -> new EjercicioVersion(
					l.fila().dia(), l.fila().orden(), l.idExterno(), l.nombre(), l.fila().series(),
					l.fila().repeticionesMin(), l.fila().repeticionesMax(), l.fila().rirObjetivo(),
					l.fila().cargaKg(), l.fila().incrementoKg())).toList();
			versiones.add(new VersionPlan(c.uuid(), c.creadoEn(), c.estado(), i == cabeceras.size() - 1, ejercicios,
					json.readValue(planes.traza(c.id()), TrazaGuardada.class)));
		}
		return versiones;
	}

	/** Los grupos del cuerpo de cada ejercicio del plan, con una sola consulta. */
	private Map<Long, List<MusculoEjercicio>> musculosDe(List<FilaLeida> filas) {
		Map<Long, List<MusculoEjercicio>> musculos = new HashMap<>();
		planes.musculos(filas.stream().map(f -> f.fila().ejercicioId()).distinct().toList())
				.forEach((ejercicio, lista) -> musculos.put(ejercicio, GrupoMuscular.agrupar(lista)));
		return musculos;
	}

	/** El id interno de una versión del plan, si es de este usuario. */
	public Optional<Long> idDeVersion(long usuarioId, UUID version) {
		return planes.idDeVersion(usuarioId, version);
	}

	/**
	 * Estado, historial de e1RM y carga siguiente de un ejercicio. Con el ejercicio en el último plan se usan sus
	 * objetivos; si no, los de {@code forja.progresion}. 404 si el ejercicio no existe.
	 */
	public ProgresionEjercicio progresion(UUID usuario, String idExterno) {
		long usuarioId = idDe(usuario);
		FichaEjercicio ficha = planes.fichas(List.of(idExterno)).get(idExterno);
		if (ficha == null) {
			throw ErrorNegocio.noEncontrado("No existe ese ejercicio.");
		}
		FilaEjercicio fila = planes.ultimo(usuarioId).flatMap(c -> planes.ejercicios(c.id()).stream()
				.map(FilaLeida::fila).filter(f -> f.ejercicioId() == ficha.id()).findFirst())
				.orElseGet(() -> filaPorDefecto(ficha));
		ResultadoProgresion r = evaluar(usuarioId, fila);
		return new ProgresionEjercicio(idExterno, r.estado(), r.cargaKg(), r.series(), r.repeticionesMin(),
				r.repeticionesMax(), r.motivo(), r.historial());
	}

	private ResultadoProgresion evaluar(long usuarioId, FilaEjercicio f) {
		return evaluar(f, historial.sesionesDeTrabajo(usuarioId, f.ejercicioId()));
	}

	private ResultadoProgresion evaluar(FilaEjercicio f, List<SesionEjercicio> sesiones) {
		ReglaEjercicio regla = new ReglaEjercicio(f.series(), f.repeticionesMin(), f.repeticionesMax(),
				f.rirObjetivo(), f.incrementoKg(), f.cargaKg());
		return MaquinaProgresion.evaluar(sesiones, regla, progresion.parametros());
	}

	private ObjetivoEjercicio objetivo(long usuarioId, FilaLeida leida, List<MusculoEjercicio> musculos) {
		FilaEjercicio f = leida.fila();
		List<SesionEjercicio> sesiones = historial.sesionesDeTrabajo(usuarioId, f.ejercicioId());
		ResultadoProgresion r = evaluar(f, sesiones);
		return new ObjetivoEjercicio(leida.idExterno(), leida.nombre(), f.orden(), r.series(), r.repeticionesMin(),
				r.repeticionesMax(), f.rirObjetivo(), r.cargaKg(), f.incrementoKg(), r.estado(), r.motivo(),
				musculos, sesiones.isEmpty());
	}

	private FilaEjercicio filaPorDefecto(FichaEjercicio ficha) {
		return filaDe(0, 0, progresion.seriesPorDefecto(), ficha, null);
	}

	/** La carga inicial es la de la última sesión del ejercicio (por {@code realizada_en}); nula si no hay. */
	private FilaEjercicio filaDe(int dia, int orden, int series, FichaEjercicio ficha, BigDecimal cargaInicial) {
		PropiedadesProgresion.Objetivo objetivo = ficha.compuesto() ? progresion.compuestos()
				: progresion.aislados();
		return new FilaEjercicio(dia, orden, ficha.id(), series, objetivo.repeticionesMin(),
				objetivo.repeticionesMax(), objetivo.rir(), cargaInicial, progresion.incremento(ficha.material()));
	}

	private BigDecimal cargaDeLaUltimaSesion(long usuarioId, long ejercicioId) {
		return historial.sesionesDeTrabajo(usuarioId, ejercicioId).stream()
				.max(Comparator.comparing(SesionEjercicio::realizadaEn).thenComparing(SesionEjercicio::sesion))
				.map(SesionEjercicio::cargaMaxima).orElse(null);
	}

	private static String estado(PlanRutina plan) {
		return switch (plan.estado()) {
			case PlanRutina.OPTIMO -> "optimo";
			case PlanRutina.FACTIBLE -> "factible";
			default -> throw new IllegalStateException("El solver devolvió un estado que no se puede guardar.");
		};
	}

	private long idDe(UUID usuario) {
		return usuarios.porUuid(usuario).orElseThrow(() -> ErrorNegocio.noAutenticado("La sesión ya no es válida."))
				.id();
	}
}
