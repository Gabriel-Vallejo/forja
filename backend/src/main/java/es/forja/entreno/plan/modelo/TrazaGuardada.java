package es.forja.entreno.plan.modelo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import es.forja.entreno.rutina.modelo.PlanRutina;
import es.forja.entreno.rutina.modelo.PlanRutina.ContribucionEjercicio;
import es.forja.entreno.rutina.modelo.PlanRutina.Descarte;
import es.forja.entreno.rutina.modelo.PlanRutina.VolumenMusculo;
import es.forja.entreno.rutina.solver.ReglasExplicadas;

/**
 * La parte de la traza que se guarda con cada versión del plan. Se deja fuera todo texto que cite las limitaciones
 * del usuario (descartes por restricción o sustitución, anotaciones, holguras, alternativas y notas): son datos de
 * salud (ADR 0004) y la columna es JSON en claro. Lo que queda es numérico o habla de material y exclusiones. El
 * término de movilidad también se quita: su signo y su tamaño delatan el tramo de cada test.
 */
public record TrazaGuardada(long objetivo, List<VolumenMusculo> volumen, List<ContribucionEjercicio> contribuciones,
		List<Descarte> descartes) {

	private static final List<String> DESCARTES_SIN_SALUD = List.of("material", "exclusion");

	public static TrazaGuardada de(PlanRutina plan) {
		return new TrazaGuardada(plan.objetivo(), plan.volumen(),
				plan.contribuciones().stream().map(TrazaGuardada::sinMovilidad).toList(), plan.descartes().stream()
						.filter(d -> DESCARTES_SIN_SALUD.contains(d.tipo())).toList());
	}

	private static ContribucionEjercicio sinMovilidad(ContribucionEjercicio c) {
		Map<String, Long> terminos = c.terminos().entrySet().stream()
				.filter(t -> !t.getKey().equals(ReglasExplicadas.MOVILIDAD))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
		return new ContribucionEjercicio(c.idExterno(), c.nombre(), terminos);
	}
}
