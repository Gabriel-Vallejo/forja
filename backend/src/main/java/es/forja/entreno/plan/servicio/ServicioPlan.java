package es.forja.entreno.plan.servicio;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.forja.comun.api.ErrorNegocio;
import es.forja.cuenta.repositorio.UsuarioRepositorio;
import es.forja.entreno.plan.modelo.HistorialSesiones;
import es.forja.entreno.plan.modelo.PlanActual;
import es.forja.entreno.plan.modelo.PlanActual.DiaEntreno;
import es.forja.entreno.plan.modelo.PlanActual.ObjetivoEjercicio;
import es.forja.entreno.plan.modelo.PlanGenerado;
import es.forja.entreno.plan.modelo.ReglaProgresion;
import es.forja.entreno.plan.modelo.SerieTrabajo;
import es.forja.entreno.plan.modelo.TrazaGuardada;
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
				planes.insertarEjercicio(planId, filaDe(usuarioId, dia.dia(), orden, e, ficha));
			}
		}
		return new PlanGenerado(uuid, plan);
	}

	/**
	 * El último plan del usuario. La carga de cada ejercicio es la que sale de aplicar la regla de la siguiente a su
	 * última sesión (el servidor manda sobre el móvil); sin sesiones, la carga inicial que se guardó con el plan.
	 */
	public PlanActual actual(UUID usuario) {
		long usuarioId = idDe(usuario);
		CabeceraPlan cabecera = planes.ultimo(usuarioId)
				.orElseThrow(() -> ErrorNegocio.noEncontrado("Todavía no has generado ningún plan."));
		Map<Integer, List<ObjetivoEjercicio>> porDia = new LinkedHashMap<>();
		for (FilaLeida leida : planes.ejercicios(cabecera.id())) {
			porDia.computeIfAbsent(leida.fila().dia(), d -> new ArrayList<>()).add(objetivo(usuarioId, leida));
		}
		List<DiaEntreno> dias = porDia.entrySet().stream().map(e -> new DiaEntreno(e.getKey(), e.getValue()))
				.toList();
		return new PlanActual(cabecera.uuid(), cabecera.creadoEn(), cabecera.estado(), dias);
	}

	/** El id interno de una versión del plan, si es de este usuario. */
	public Optional<Long> idDeVersion(long usuarioId, UUID version) {
		return planes.idDeVersion(usuarioId, version);
	}

	private ObjetivoEjercicio objetivo(long usuarioId, FilaLeida leida) {
		FilaEjercicio f = leida.fila();
		List<SerieTrabajo> ultimas = historial.ultimasSeriesDeTrabajo(usuarioId, f.ejercicioId());
		BigDecimal carga = ReglaProgresion
				.siguienteCarga(ultimas, f.repeticionesMax(), f.rirObjetivo(), f.incrementoKg())
				.orElse(f.cargaKg());
		return new ObjetivoEjercicio(leida.idExterno(), leida.nombre(), f.orden(), f.series(), f.repeticionesMin(),
				f.repeticionesMax(), f.rirObjetivo(), carga, f.incrementoKg());
	}

	private FilaEjercicio filaDe(long usuarioId, int dia, int orden, EjercicioPlan e, FichaEjercicio ficha) {
		PropiedadesProgresion.Objetivo objetivo = ficha.compuesto() ? progresion.compuestos()
				: progresion.aislados();
		BigDecimal cargaInicial = ReglaProgresion
				.cargaBase(historial.ultimasSeriesDeTrabajo(usuarioId, ficha.id())).orElse(null);
		return new FilaEjercicio(dia, orden, ficha.id(), e.series(), objetivo.repeticionesMin(),
				objetivo.repeticionesMax(), objetivo.rir(), cargaInicial, progresion.incremento(ficha.material()));
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
