package es.forja.entreno.plan.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/** e1RM con Epley: {@code peso × (1 + repeticiones / 30)}, con dos decimales. */
public final class E1rm {

	private static final int DIVISOR_EPLEY = 30;
	private static final int DECIMALES = 2;

	private E1rm() {
	}

	/** De una serie; vacío si no cuenta (sin repeticiones, sin carga o por encima de {@code maximoRepeticiones}). */
	public static Optional<BigDecimal> deSerie(SerieTrabajo serie, int maximoRepeticiones) {
		if (serie.repeticiones() < 1 || serie.repeticiones() > maximoRepeticiones || serie.pesoKg().signum() <= 0) {
			return Optional.empty();
		}
		// peso × (30 + repeticiones) / 30: se redondea una sola vez, al final.
		return Optional.of(serie.pesoKg().multiply(BigDecimal.valueOf(DIVISOR_EPLEY + serie.repeticiones()))
				.divide(BigDecimal.valueOf(DIVISOR_EPLEY), DECIMALES, RoundingMode.HALF_UP));
	}

	/** El máximo de las series que cuentan. */
	public static Optional<BigDecimal> deSesion(List<SerieTrabajo> series, int maximoRepeticiones) {
		return series.stream().map(s -> deSerie(s, maximoRepeticiones)).flatMap(Optional::stream)
				.max(BigDecimal::compareTo);
	}
}
