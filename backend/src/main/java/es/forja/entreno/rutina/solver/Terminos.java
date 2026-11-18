package es.forja.entreno.rutina.solver;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import es.forja.entreno.rutina.modelo.ConfiguracionRutina;
import es.forja.entreno.rutina.modelo.ConfiguracionRutina.Pesos;
import es.forja.restricciones.modelo.Patron;
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
	static final String COMPUESTO = "compuesto";
	static final String VOLUMEN = "volumen";
	static final String MOVILIDAD = "movilidad";
	static final String PALANCAS = "palancas";

	private Terminos() {
	}

	/** Puntos por cada serie del ejercicio, término a término, sin la variedad (que va por días). */
	static Map<String, Long> porTermino(ConfiguracionRutina c, EjercicioRutina e, List<String> prioritarios,
			Map<String, Integer> movilidad, Map<String, Integer> palancas) {
		Pesos p = c.pesos();
		long cobertura = prioritarios.stream().mapToLong(e::pesoDoble).sum();
		Map<String, Long> terminos = new LinkedHashMap<>();
		terminos.put(COBERTURA_PRIORITARIOS, (long) p.coberturaPrioritarios() * cobertura);
		terminos.put(POSICION_ESTIRADA, e.posicionEstirada() ? p.posicionEstirada() : 0L);
		terminos.put(FATIGA, e.basicoPesado() ? -(long) p.fatiga() : 0L);
		terminos.put(COMPUESTO, basicoPrincipal(c, e) ? p.serieCompuesto() : 0L);
		terminos.put(MOVILIDAD, (long) p.movilidad() * movilidad.getOrDefault(e.idExterno(), 0));
		terminos.put(PALANCAS, (long) p.palancas() * palancas.getOrDefault(e.idExterno(), 0));
		return terminos;
	}

	/** Compuesto de un patrón principal: el que se premia. Un compuesto de core o un acarreo no cuenta. */
	static boolean basicoPrincipal(ConfiguracionRutina c, EjercicioRutina e) {
		return e.compuesto() && c.basicos().patrones().stream().anyMatch(p -> Patron.desdeCodigo(p) == e.patron());
	}

	static long porSerie(ConfiguracionRutina c, EjercicioRutina e, List<String> prioritarios, Map<String, Integer> movilidad,
			Map<String, Integer> palancas) {
		return porTermino(c, e, prioritarios, movilidad, palancas).values().stream().mapToLong(Long::longValue).sum();
	}

	/**
	 * Contribución del ejercicio en el plan: cada término por sus series, la variedad por los días de más y su parte
	 * del volumen objetivo de cada músculo, repartida según las series dobles que aporta.
	 */
	static Map<String, Long> contribucion(ConfiguracionRutina c, EjercicioRutina e, List<String> prioritarios,
			Map<String, Integer> movilidad, int series, int diasDeMas, long volumen) {
		Pesos p = c.pesos();
		Map<String, Long> contribucion = new LinkedHashMap<>();
		porTermino(c, e, prioritarios, movilidad, Map.of()).forEach((t, v) -> contribucion.put(t, v * series));
		contribucion.put(VARIEDAD, -(long) p.variedad() * diasDeMas);
		contribucion.put(VOLUMEN, volumen);
		return contribucion;
	}
}
