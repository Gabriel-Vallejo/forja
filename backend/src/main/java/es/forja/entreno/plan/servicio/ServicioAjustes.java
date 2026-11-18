package es.forja.entreno.plan.servicio;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import es.forja.comun.api.ErrorNegocio;
import es.forja.cuenta.repositorio.UsuarioRepositorio;
import es.forja.entreno.perfil.modelo.PerfilEntreno;
import es.forja.entreno.perfil.servicio.ServicioPerfilEntreno;
import es.forja.entreno.plan.modelo.Ajuste;
import es.forja.entreno.plan.modelo.PlanGenerado;
import es.forja.entreno.plan.modelo.PreferenciasGuardadas;
import es.forja.entreno.plan.modelo.PreferenciasPlan;
import es.forja.entreno.plan.modelo.TrazaGuardada;
import es.forja.entreno.plan.modelo.VistaPrevia;
import es.forja.entreno.plan.repositorio.PlanRepositorio;
import es.forja.entreno.plan.repositorio.PlanRepositorio.CabeceraPlan;
import es.forja.entreno.plan.repositorio.PlanRepositorio.FilaLeida;
import es.forja.entreno.plan.repositorio.PreferenciasRepositorio;
import es.forja.entreno.rutina.modelo.CatalogoRutina;
import es.forja.entreno.rutina.modelo.ConfiguracionRutina;
import es.forja.entreno.rutina.modelo.EjercicioRutina;
import es.forja.entreno.rutina.modelo.PlanRutina;
import es.forja.entreno.rutina.modelo.PlanRutina.VolumenMusculo;
import es.forja.entreno.rutina.modelo.Preferencias;
import es.forja.entreno.rutina.modelo.SolicitudRutina;
import es.forja.entreno.rutina.servicio.PropiedadesRutina;
import es.forja.entreno.rutina.servicio.ServicioRutina;
import tools.jackson.databind.json.JsonMapper;

/**
 * Ajustes del plan (PRD v0.1): cada ajuste cambia las preferencias guardadas, vuelve a resolver con los ajustes del
 * perfil y enseña la vista previa sin guardar nada; aplicarlo guarda las preferencias y el plan como versión nueva
 * (ADR 0008 §6). El bloqueo por bandera roja y el límite de intentos son los de generar.
 */
@Service
public class ServicioAjustes {

	private final PlanRepositorio planes;
	private final PreferenciasRepositorio preferencias;
	private final UsuarioRepositorio usuarios;
	private final ServicioRutina rutina;
	private final ServicioPlan servicioPlan;
	private final ServicioPerfilEntreno perfil;
	private final ConfiguracionRutina config;
	private final JsonMapper json;
	private final Clock reloj;
	private final TransactionTemplate transaccion;

	public ServicioAjustes(PlanRepositorio planes, PreferenciasRepositorio preferencias, UsuarioRepositorio usuarios,
			ServicioRutina rutina, ServicioPlan servicioPlan, ServicioPerfilEntreno perfil,
			PropiedadesRutina propiedades, JsonMapper json, Clock reloj, PlatformTransactionManager transacciones) {
		this.planes = planes;
		this.preferencias = preferencias;
		this.usuarios = usuarios;
		this.rutina = rutina;
		this.servicioPlan = servicioPlan;
		this.perfil = perfil;
		this.config = propiedades.aConfiguracion();
		this.json = json;
		this.reloj = reloj;
		this.transaccion = new TransactionTemplate(transacciones);
	}

	/** El plan actual, las preferencias que quedarían y el plan resuelto con ellas. */
	private record Propuesta(long usuarioId, CabeceraPlan cabecera, List<FilaLeida> filas, PreferenciasPlan nuevas,
			PlanRutina plan) {
	}

	public PreferenciasGuardadas preferencias(UUID usuario) {
		return preferencias.de(idDe(usuario));
	}

	@Transactional
	public PreferenciasGuardadas guardarPreferencias(UUID usuario, PreferenciasPlan nuevas) {
		long usuarioId = idDe(usuario);
		validar(nuevas);
		preferencias.guardar(usuarioId, nuevas, reloj.instant());
		return preferencias.de(usuarioId);
	}

	/** Las guardadas, listas para el generador: con «más variedad», los ejercicios del último plan pierden puntos. */
	public Preferencias paraGenerar(UUID usuario) {
		long usuarioId = idDe(usuario);
		return paraElGenerador(preferencias.de(usuarioId).aCodigos(),
				planes.ultimo(usuarioId).map(c -> planes.ejercicios(c.id())).orElse(List.of()));
	}

	/** Resuelve con el ajuste y enseña qué cambia; no guarda nada. */
	public VistaPrevia vistaPrevia(UUID usuario, Ajuste ajuste) {
		Propuesta p = proponer(usuario, ajuste);
		Map<String, String> nombres = new TreeMap<>();
		p.filas().forEach(f -> nombres.put(f.idExterno(), f.nombre()));
		List<VolumenMusculo> antes = json.readValue(planes.traza(p.cabecera().id()), TrazaGuardada.class).volumen();
		return new VistaPrevia(p.cabecera().uuid(), p.plan(),
				VistaPrevia.cambiosDeEjercicios(seriesSemana(p.filas()), nombres, p.plan()),
				VistaPrevia.cambiosDeMusculos(antes, p.plan().volumen()), p.nuevas());
	}

	/**
	 * Vuelve a resolver (el solver es determinista: sale el plan de la vista previa) y guarda las preferencias y la
	 * versión nueva juntas. 409 si el plan ha cambiado desde la vista previa.
	 */
	public PlanGenerado aplicar(UUID usuario, Ajuste ajuste) {
		if (ajuste.version() == null) {
			throw ErrorNegocio.datosNoValidos("Falta la versión del plan de la vista previa.");
		}
		Propuesta p = proponer(usuario, ajuste);
		return transaccion.execute(estado -> {
			preferencias.guardar(p.usuarioId(), p.nuevas(), reloj.instant());
			return servicioPlan.guardar(usuario, p.plan());
		});
	}

	private Propuesta proponer(UUID usuario, Ajuste ajuste) {
		long usuarioId = idDe(usuario);
		CabeceraPlan cabecera = planes.ultimo(usuarioId)
				.orElseThrow(() -> ErrorNegocio.noEncontrado("Todavía no has generado ningún plan."));
		if (ajuste.version() != null && !ajuste.version().equals(cabecera.uuid())) {
			throw ErrorNegocio.conflicto("Tu plan ha cambiado desde la vista previa: vuelve a verla antes de aplicar.");
		}
		List<FilaLeida> filas = planes.ejercicios(cabecera.id());
		Map<String, Integer> actuales = seriesSemana(filas);
		PerfilEntreno ajustes = perfil.obtener(usuario);
		PreferenciasPlan nuevas = conAjuste(preferencias.de(usuarioId).aCodigos(), ajuste, actuales,
				ajustes.diasPorSemana());
		validar(nuevas);
		SolicitudRutina solicitud = new SolicitudRutina(ajustes.diasPorSemana(), ajustes.minutosPorSesion(),
				ajustes.material(), List.of(), List.of(), null);
		PlanRutina plan = rutina.generar(usuario, solicitud, paraElGenerador(nuevas, filas));
		if (Ajuste.MAS.equals(ajuste.tipo()) || Ajuste.MENOS.equals(ajuste.tipo())) {
			exigirSeries(ajuste, nuevas.series().get(ajuste.idExterno()), plan, cabecera, filas);
		}
		return new Propuesta(usuarioId, cabecera, filas, nuevas, plan);
	}

	/** Las preferencias con el ajuste aplicado. 404 si el ejercicio no está en el plan; 422 si choca con un tope. */
	private PreferenciasPlan conAjuste(PreferenciasPlan p, Ajuste ajuste, Map<String, Integer> actuales, int dias) {
		List<String> exclusiones = new ArrayList<>(p.exclusiones());
		Map<String, Integer> series = new HashMap<>(p.series());
		List<String> prioritarios = p.prioritarios();
		boolean variedad = p.variedad();
		String id = ajuste.idExterno();
		if (ajuste.deEjercicio() && (id == null || !actuales.containsKey(id))) {
			throw ErrorNegocio.noEncontrado("Ese ejercicio no está en tu plan.");
		}
		switch (ajuste.tipo()) {
			case Ajuste.MAS, Ajuste.MENOS -> series.put(id, objetivoDeSeries(ajuste, actuales.get(id), dias));
			case Ajuste.NO_RECOMENDAR, Ajuste.REEMPLAZAR -> {
				if (exclusiones.size() >= PreferenciasPlan.MAXIMO_EXCLUSIONES) {
					throw ErrorNegocio.noProcesable("Ya has quitado 4 ejercicios, el máximo. Recupera alguno en "
							+ "Ajustes del plan para quitar otro.");
				}
				exclusiones.add(id);
				series.remove(id);
			}
			case Ajuste.VARIEDAD -> variedad = true;
			case Ajuste.ENFOCAR -> prioritarios = Objects.requireNonNullElse(ajuste.musculos(), List.of());
			default -> throw ErrorNegocio.datosNoValidos("Ese ajuste no existe.");
		}
		return new PreferenciasPlan(exclusiones, prioritarios, series, variedad);
	}

	/** Una serie más o menos a la semana, dentro de los topes duros; si no cabe, 422 con el porqué. */
	private int objetivoDeSeries(Ajuste ajuste, int actuales, int dias) {
		ConfiguracionRutina.Sesion s = config.sesion();
		String nombre = ejercicio(ajuste.idExterno()).map(EjercicioRutina::nombre).orElse(ajuste.idExterno());
		if (Ajuste.MENOS.equals(ajuste.tipo())) {
			if (actuales - 1 < s.seriesMinimasPorEjercicio()) {
				throw ErrorNegocio.noProcesable(("%s ya está en el mínimo de %d series a la semana: con menos sale del "
						+ "plan. Si no lo quieres, usa «No recomendar».").formatted(nombre,
								s.seriesMinimasPorEjercicio()));
			}
			return actuales - 1;
		}
		int tope = Math.min(PreferenciasPlan.SERIES_MAXIMAS, dias * s.seriesMaximasPorEjercicio());
		if (actuales + 1 > tope) {
			throw ErrorNegocio.noProcesable("%s ya tiene %d series a la semana, el tope de %d por sesión en tus días."
					.formatted(nombre, actuales, s.seriesMaximasPorEjercicio()));
		}
		return actuales + 1;
	}

	/** Si el solver no pudo con las series fijadas (el generador las quita y lo anota), 422 con el porqué. */
	private void exigirSeries(Ajuste ajuste, int objetivo, PlanRutina plan, CabeceraPlan cabecera,
			List<FilaLeida> filas) {
		if (VistaPrevia.seriesSemana(plan).getOrDefault(ajuste.idExterno(), 0) == objetivo) {
			return;
		}
		Optional<EjercicioRutina> ejercicio = ejercicio(ajuste.idExterno());
		String nombre = ejercicio.map(EjercicioRutina::nombre).orElse(ajuste.idExterno());
		if (Ajuste.MENOS.equals(ajuste.tipo())) {
			throw ErrorNegocio.noProcesable(("Con una serie menos de %s el plan no llega al mínimo de ejercicios y de "
					+ "tiempo por sesión.").formatted(nombre));
		}
		List<VolumenMusculo> volumen = json.readValue(planes.traza(cabecera.id()), TrazaGuardada.class).volumen();
		for (VolumenMusculo v : volumen) {
			int peso = ejercicio.map(e -> e.pesoDoble(v.musculo())).orElse(0);
			if (peso > 0 && v.series() + peso / 2.0 > v.maximo()) {
				throw ErrorNegocio.noProcesable("Con una serie más de %s, %s pasaría de %d series a la semana, su máximo."
						.formatted(nombre, v.nombre().toLowerCase(Locale.ROOT), v.maximo()));
			}
		}
		throw ErrorNegocio.noProcesable("Una serie más de %s no cabe en el tiempo de tus sesiones con el resto del plan."
				.formatted(nombre));
	}

	/** Ejercicios y músculos que existen en el catálogo, y nada repetido entre quitados y series fijadas. */
	private void validar(PreferenciasPlan p) {
		CatalogoRutina catalogo = rutina.catalogo();
		Set<String> ids = catalogo.ejercicios().stream().map(EjercicioRutina::idExterno).collect(Collectors.toSet());
		if (!ids.containsAll(p.exclusiones()) || !ids.containsAll(p.series().keySet())) {
			throw ErrorNegocio.datosNoValidos("Hay ejercicios que no existen.");
		}
		if (!catalogo.nombresMusculo().keySet().containsAll(p.prioritarios())) {
			throw ErrorNegocio.datosNoValidos("Hay músculos que no existen.");
		}
		if (p.exclusiones().stream().anyMatch(p.series()::containsKey)
				|| Set.copyOf(p.exclusiones()).size() != p.exclusiones().size()
				|| Set.copyOf(p.prioritarios()).size() != p.prioritarios().size()) {
			throw ErrorNegocio.datosNoValidos("Hay ejercicios o músculos repetidos.");
		}
	}

	private Preferencias paraElGenerador(PreferenciasPlan p, List<FilaLeida> planAnterior) {
		Set<String> evitar = p.variedad() ? planAnterior.stream().map(FilaLeida::idExterno).collect(Collectors.toSet())
				: Set.of();
		return new Preferencias(Set.copyOf(p.exclusiones()), p.prioritarios(), p.series(), evitar);
	}

	private Optional<EjercicioRutina> ejercicio(String idExterno) {
		return rutina.catalogo().ejercicios().stream().filter(e -> e.idExterno().equals(idExterno)).findFirst();
	}

	private static Map<String, Integer> seriesSemana(List<FilaLeida> filas) {
		Map<String, Integer> series = new LinkedHashMap<>();
		filas.forEach(f -> series.merge(f.idExterno(), f.fila().series(), Integer::sum));
		return series;
	}

	private long idDe(UUID usuario) {
		return usuarios.porUuid(usuario).orElseThrow(() -> ErrorNegocio.noAutenticado("La sesión ya no es válida."))
				.id();
	}
}
