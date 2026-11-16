package es.forja.entreno.plan.modelo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Las series de trabajo vigentes de un ejercicio en una sesión. {@code sesion} desempata fechas iguales. */
public record SesionEjercicio(UUID sesion, Instant realizadaEn, List<SerieTrabajo> series) {

	public SesionEjercicio {
		series = List.copyOf(series);
	}

	/** La carga de la sesión: la más alta de sus series de trabajo. */
	public BigDecimal cargaMaxima() {
		return ReglaProgresion.cargaBase(series).orElse(BigDecimal.ZERO);
	}
}
