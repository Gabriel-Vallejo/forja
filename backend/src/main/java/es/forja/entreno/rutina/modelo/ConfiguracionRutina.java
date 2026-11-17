package es.forja.entreno.rutina.modelo;

import java.util.List;
import java.util.Map;

/**
 * Parámetros del generador. Salen de {@code forja.rutina.*} en {@code application.yml}: los pesos, los rangos y los
 * objetivos no se tocan en el código.
 */
public record ConfiguracionRutina(Pesos pesos, Volumen volumen, Basicos basicos, Tiempos tiempos, Sesion sesion,
		Solver solver) {

	/**
	 * Pesos de cada término del objetivo; movilidad y palancas, a 0 hasta que existan sus datos.
	 *
	 * @param serieHastaObjetivo premio por serie doble de un músculo hasta su volumen objetivo
	 * @param serieSobreObjetivo penalización suave por serie doble por encima del objetivo (o de un músculo sin meta)
	 * @param serieCompuesto premio por serie de un compuesto; mayor que {@code fatiga}
	 * @param holguraBasicos por cada patrón principal que falta en la semana
	 * @param desequilibrioSesiones por minuto de diferencia entre la sesión más larga y la más corta
	 */
	public record Pesos(int coberturaPrioritarios, int posicionEstirada, int variedad, int fatiga,
			int serieHastaObjetivo, int serieSobreObjetivo, int serieCompuesto, int holguraVolumen,
			int holguraFrecuencia, int holguraBasicos, int desequilibrioSesiones, int movilidad, int palancas) {
	}

	/** Series semanales efectivas por músculo; los secundarios cuentan 0,5. */
	public record Rango(int minimo, int objetivo, int maximo) {
	}

	/**
	 * @param minimo {@code objetivo} y {@code maximo}: el rango por defecto (heurística de entrenadores, RP,
	 *            nivel H)
	 * @param musculosConMinimo músculos con meta de volumen y de frecuencia; los demás solo tienen máximo, y su
	 *            objetivo es 0
	 * @param porMusculo rangos que cambian el de por defecto para un músculo
	 */
	public record Volumen(int minimo, int objetivo, int maximo, List<String> musculosConMinimo,
			Map<String, Rango> porMusculo) {

		public Volumen {
			musculosConMinimo = List.copyOf(musculosConMinimo);
			porMusculo = porMusculo == null ? Map.of() : Map.copyOf(porMusculo);
		}

		public Rango rango(String musculo) {
			return porMusculo.getOrDefault(musculo, new Rango(minimo, objetivo, maximo));
		}

		/** Series objetivo del músculo: las de su rango si tiene meta, 0 si no. */
		public int objetivoDe(String musculo) {
			return musculosConMinimo.contains(musculo) ? rango(musculo).objetivo() : 0;
		}
	}

	/** Patrones principales (códigos de {@code patron_movimiento}) que la semana debe tener con un compuesto. */
	public record Basicos(List<String> patrones) {

		public Basicos {
			patrones = List.copyOf(patrones);
		}
	}

	public record Tiempos(int segundosPorSerie, int segundosDeDescanso, int segundosDeTransicion) {

		public int segundosPorSerieConDescanso() {
			return segundosPorSerie + segundosDeDescanso;
		}
	}

	/**
	 * @param porcentajeMinimoDelTiempo ninguna sesión por debajo de este porcentaje del tiempo disponible
	 * @param seriesMaximasPorPatronYDia tope de series de un mismo patrón en una sesión (salvo los de aislamiento)
	 */
	public record Sesion(int seriesMinimasPorEjercicio, int seriesMaximasPorEjercicio, int ejerciciosMinimos,
			int ejerciciosMaximos, int maximoPorPatron, int porcentajeMinimoDelTiempo, int seriesMaximasPorPatronYDia) {
	}

	/**
	 * Reproducible (ADR 0002, enmienda): límite determinista, workers y semilla fijos.
	 *
	 * @param tiempoDeterministaAlternativas límite de cada re-resolución del «por qué este y no aquel»
	 */
	public record Solver(double tiempoDeterminista, double tiempoDeterministaAlternativas, int workers, int semilla) {
	}
}
