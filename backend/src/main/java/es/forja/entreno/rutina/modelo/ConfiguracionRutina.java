package es.forja.entreno.rutina.modelo;

import java.util.List;
import java.util.Map;

/**
 * Parámetros del generador. Salen de {@code forja.rutina.*} en {@code application.yml}: los pesos y los rangos no
 * se tocan en el código.
 */
public record ConfiguracionRutina(Pesos pesos, Volumen volumen, Tiempos tiempos, Sesion sesion, Solver solver) {

	/** Pesos de cada término del objetivo; movilidad y palancas, a 0 hasta que existan sus datos. */
	public record Pesos(int coberturaPrioritarios, int posicionEstirada, int variedad, int fatiga, int series,
			int holguraVolumen, int holguraFrecuencia, int movilidad, int palancas) {
	}

	/** Series semanales efectivas por músculo; los secundarios cuentan 0,5. */
	public record Rango(int minimo, int maximo) {
	}

	/**
	 * @param minimo y {@code maximo}: el rango por defecto (heurística de entrenadores, RP, nivel H)
	 * @param musculosConMinimo músculos con meta de volumen y de frecuencia; los demás solo tienen máximo
	 * @param porMusculo rangos que cambian el de por defecto para un músculo
	 */
	public record Volumen(int minimo, int maximo, List<String> musculosConMinimo, Map<String, Rango> porMusculo) {

		public Volumen {
			musculosConMinimo = List.copyOf(musculosConMinimo);
			porMusculo = porMusculo == null ? Map.of() : Map.copyOf(porMusculo);
		}

		public Rango rango(String musculo) {
			return porMusculo.getOrDefault(musculo, new Rango(minimo, maximo));
		}
	}

	public record Tiempos(int segundosPorSerie, int segundosDeDescanso, int segundosDeTransicion) {

		public int segundosPorSerieConDescanso() {
			return segundosPorSerie + segundosDeDescanso;
		}
	}

	public record Sesion(int seriesMinimasPorEjercicio, int seriesMaximasPorEjercicio, int ejerciciosMinimos,
			int ejerciciosMaximos, int maximoPorPatron) {
	}

	/** Reproducible (ADR 0002, enmienda): límite determinista, workers y semilla fijos. */
	public record Solver(double tiempoDeterminista, int workers, int semilla) {
	}
}
