package es.forja.entreno.plan.modelo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Las series de trabajo vigentes de un ejercicio en una sesión. {@code sesion} desempata fechas iguales.
 *
 * @param seriesPrevistas las que pedía el plan con el que se hizo (su versión y su día); null si no se sabe
 */
public record SesionEjercicio(UUID sesion, Instant realizadaEn, List<SerieTrabajo> series, Integer seriesPrevistas) {

	public SesionEjercicio {
		series = List.copyOf(series);
	}

	public SesionEjercicio(UUID sesion, Instant realizadaEn, List<SerieTrabajo> series) {
		this(sesion, realizadaEn, series, null);
	}

	public SesionEjercicio conSeriesPrevistas(Integer previstas) {
		return new SesionEjercicio(sesion, realizadaEn, series, previstas);
	}

	/** La carga de la sesión: la más alta de sus series de trabajo. */
	public BigDecimal cargaMaxima() {
		return ReglaProgresion.cargaBase(series).orElse(BigDecimal.ZERO);
	}
}
