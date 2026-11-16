package es.forja.entreno.rutina.solver;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import es.forja.entreno.rutina.modelo.ConfiguracionRutina.Pesos;
import es.forja.entreno.rutina.modelo.EjercicioRutina;

/**
 * Términos del objetivo que dependen de las series de un ejercicio. El modelo y la traza usan esta misma cuenta,
 * así que la contribución que enseña la traza es la que maximizó el solver.
 */
final class Terminos {

	static final String COBERTURA_PRIORITARIOS = "cobertura_prioritarios";
	static final String POSICION_ESTIRADA = "posicion_estirada";
	static final String VARIEDAD = "variedad";
	static final String FATIGA = "fatiga";
	static final String SERIES = "series";
	static final String MOVILIDAD = "movilidad";
	static final String PALANCAS = "palancas";

	private Terminos() {
	}

	/** Puntos por cada serie del ejercicio, término a término, sin la variedad (que va por días). */
	static Map<String, Long> porTermino(Pesos p, EjercicioRutina e, List<String> prioritarios,
			Map<String, Integer> movilidad, Map<String, Integer> palancas) {
		long cobertura = prioritarios.stream().mapToLong(e::pesoDoble).sum();
		Map<String, Long> terminos = new LinkedHashMap<>();
		terminos.put(COBERTURA_PRIORITARIOS, (long) p.coberturaPrioritarios() * cobertura);
		terminos.put(POSICION_ESTIRADA, e.posicionEstirada() ? p.posicionEstirada() : 0L);
		terminos.put(FATIGA, e.basicoPesado() ? -(long) p.fatiga() : 0L);
		terminos.put(SERIES, -(long) p.series());
		terminos.put(MOVILIDAD, (long) p.movilidad() * movilidad.getOrDefault(e.idExterno(), 0));
		terminos.put(PALANCAS, (long) p.palancas() * palancas.getOrDefault(e.idExterno(), 0));
		return terminos;
	}

	static long porSerie(Pesos p, EjercicioRutina e, List<String> prioritarios, Map<String, Integer> movilidad,
			Map<String, Integer> palancas) {
		return porTermino(p, e, prioritarios, movilidad, palancas).values().stream().mapToLong(Long::longValue).sum();
	}

	/** Contribución del ejercicio en el plan: cada término por sus series, y la variedad por los días de más. */
	static Map<String, Long> contribucion(Pesos p, EjercicioRutina e, List<String> prioritarios, int series,
			int diasDeMas) {
		Map<String, Long> contribucion = new LinkedHashMap<>();
		porTermino(p, e, prioritarios, Map.of(), Map.of()).forEach((t, v) -> contribucion.put(t, v * series));
		contribucion.put(VARIEDAD, -(long) p.variedad() * diasDeMas);
		return contribucion;
	}
}
