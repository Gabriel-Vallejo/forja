package es.forja.entreno.plan.modelo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * La regla de la siguiente sesión (ADR 0008 §5): si haces al menos las {@code seriesPrevistas} y en todas tus
 * series de trabajo completas {@code repeticionesMax} con {@code rirObjetivo} o menos, la próxima vez
 * +{@code incrementoKg}; si no, repite.
 * Es la misma regla que viaja en el plan para que la app la aplique sin red.
 */
public final class ReglaProgresion {

	private ReglaProgresion() {
	}

	/** La carga de la sesión: la más alta de sus series de trabajo. Vacío si no hubo ninguna. */
	public static Optional<BigDecimal> cargaBase(List<SerieTrabajo> series) {
		return series.stream().map(SerieTrabajo::pesoKg).max(BigDecimal::compareTo);
	}

	/** Carga de la próxima vez a partir de las series de trabajo vigentes de la última sesión. */
	public static Optional<BigDecimal> siguienteCarga(List<SerieTrabajo> series, int seriesPrevistas,
			int repeticionesMax, int rirObjetivo, BigDecimal incrementoKg) {
		boolean cumplida = cumplida(series, seriesPrevistas, repeticionesMax, rirObjetivo);
		return cargaBase(series).map(base -> cumplida ? base.add(incrementoKg) : base);
	}

	/**
	 * Hubo al menos {@code seriesPrevistas} (y alguna) y todas llegaron a {@code repeticionesMax} con
	 * {@code rirObjetivo} o menos.
	 */
	public static boolean cumplida(List<SerieTrabajo> series, int seriesPrevistas, int repeticionesMax,
			int rirObjetivo) {
		return !series.isEmpty() && series.size() >= seriesPrevistas && series.stream().allMatch(
				s -> s.repeticiones() >= repeticionesMax && s.rir() != null && s.rir() <= rirObjetivo);
	}
}
