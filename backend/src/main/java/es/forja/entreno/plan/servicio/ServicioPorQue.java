package es.forja.entreno.plan.servicio;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import es.forja.comun.api.ErrorNegocio;
import es.forja.cuenta.repositorio.UsuarioRepositorio;
import es.forja.entreno.movilidad.servicio.ServicioMovilidad;
import es.forja.entreno.plan.modelo.PorQueEjercicio;
import es.forja.entreno.plan.modelo.TrazaGuardada;
import es.forja.entreno.plan.repositorio.PlanRepositorio;
import es.forja.entreno.plan.repositorio.PlanRepositorio.CabeceraPlan;
import es.forja.entreno.plan.repositorio.PlanRepositorio.FilaLeida;
import es.forja.entreno.rutina.modelo.ConfiguracionRutina;
import es.forja.entreno.rutina.modelo.EjercicioRutina;
import es.forja.entreno.rutina.modelo.PlanRutina.ContribucionEjercicio;
import es.forja.entreno.rutina.servicio.PropiedadesRutina;
import es.forja.entreno.rutina.servicio.ServicioRutina;
import es.forja.entreno.rutina.solver.ReglasExplicadas;
import es.forja.entreno.rutina.solver.ReglasExplicadas.Contexto;
import es.forja.restricciones.filtro.Decision;
import es.forja.restricciones.filtro.FiltroRestricciones;
import es.forja.restricciones.modelo.Restriccion;
import es.forja.restricciones.servicio.ServicioRestricciones;
import tools.jackson.databind.json.JsonMapper;

/**
 * «Por qué este ejercicio» al leer el plan (PRD v0.1). Lo universal sale de la traza guardada y del catálogo; lo de
 * salud (sustitución por una limitación y tramo de movilidad) se calcula aquí con los datos cifrados del usuario, solo
 * si el consentimiento sigue vigente, y nunca se guarda (ADR 0004).
 */
@Service
public class ServicioPorQue {

	private static final FiltroRestricciones FILTRO = new FiltroRestricciones();

	private final PlanRepositorio planes;
	private final UsuarioRepositorio usuarios;
	private final ServicioRutina rutina;
	private final ServicioRestricciones restricciones;
	private final ServicioMovilidad movilidad;
	private final ConfiguracionRutina config;
	private final JsonMapper json;

	public ServicioPorQue(PlanRepositorio planes, UsuarioRepositorio usuarios, ServicioRutina rutina,
			ServicioRestricciones restricciones, ServicioMovilidad movilidad, PropiedadesRutina propiedades,
			JsonMapper json) {
		this.planes = planes;
		this.usuarios = usuarios;
		this.rutina = rutina;
		this.restricciones = restricciones;
		this.movilidad = movilidad;
		this.config = propiedades.aConfiguracion();
		this.json = json;
	}

	public PorQueEjercicio calcular(UUID usuario, int dia, String idExterno) {
		long usuarioId = usuarios.porUuid(usuario)
				.orElseThrow(() -> ErrorNegocio.noAutenticado("La sesión ya no es válida.")).id();
		CabeceraPlan cabecera = planes.ultimo(usuarioId)
				.orElseThrow(() -> ErrorNegocio.noEncontrado("Todavía no has generado ningún plan."));
		List<FilaLeida> filas = planes.ejercicios(cabecera.id());
		if (filas.stream().noneMatch(f -> f.fila().dia() == dia && f.idExterno().equals(idExterno))) {
			throw ErrorNegocio.noEncontrado("Ese ejercicio no está en ese día de tu plan.");
		}
		Map<String, EjercicioRutina> catalogo = rutina.catalogo().ejercicios().stream()
				.collect(Collectors.toMap(EjercicioRutina::idExterno, Function.identity()));
		EjercicioRutina ejercicio = catalogo.get(idExterno);
		if (ejercicio == null) {
			// Ya no es un curado del catálogo: no hay reglas con las que explicarlo.
			return new PorQueEjercicio(idExterno, dia, List.of());
		}
		TrazaGuardada traza = json.readValue(planes.traza(cabecera.id()), TrazaGuardada.class);
		Map<String, Long> terminos = traza.contribuciones().stream().filter(c -> c.idExterno().equals(idExterno))
				.findFirst().map(ContribucionEjercicio::terminos).orElse(Map.of());
		List<FilaLeida> suyas = filas.stream().filter(f -> f.idExterno().equals(idExterno)).toList();
		int seriesPatronDia = filas.stream().filter(f -> f.fila().dia() == dia)
				.filter(f -> catalogo.containsKey(f.idExterno())
						&& catalogo.get(f.idExterno()).patron() == ejercicio.patron())
				.mapToInt(f -> f.fila().series()).sum();
		Contexto contexto = new Contexto(ejercicio, terminos, traza.volumen(),
				suyas.stream().mapToInt(f -> f.fila().series()).sum(),
				(int) suyas.stream().map(f -> f.fila().dia()).distinct().count(), seriesPatronDia,
				sustituye(usuario, idExterno, catalogo), movilidad.paraLaRutina(usuario));
		return new PorQueEjercicio(idExterno, dia, ReglasExplicadas.porQue(config, contexto));
	}

	/**
	 * Los ejercicios a los que sustituye por las limitaciones vigentes; vacío sin consentimiento.
	 *
	 * <p>shortcut: se aplica el filtro sobre todos los curados, sin el material ni las exclusiones de cuando se generó
	 * (no se guardan con el plan); si un día se guardan, filtrar antes por ellos como hace {@code FiltroPrevio}.
	 */
	private List<String> sustituye(UUID usuario, String idExterno, Map<String, EjercicioRutina> catalogo) {
		List<Restriccion> activas = restricciones.paraLaRutina(usuario);
		if (activas.isEmpty()) {
			return List.of();
		}
		List<EjercicioRutina> curados = catalogo.values().stream().filter(e -> e.candidato().curado() && e.aptoFuerza())
				.sorted(Comparator.comparing(EjercicioRutina::idExterno)).toList();
		return FILTRO.aplicar(curados.stream().map(EjercicioRutina::candidato).toList(), activas).entrySet().stream()
				.filter(d -> d.getValue() instanceof Decision.Sustituido s && s.porIdExterno().equals(idExterno))
				.map(d -> catalogo.get(d.getKey()).nombre()).sorted().toList();
	}
}
