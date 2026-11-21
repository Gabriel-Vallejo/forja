package es.forja.entreno.plan.modelo;

import java.math.BigDecimal;
import java.util.List;

import org.jspecify.annotations.Nullable;

/** Respuesta de {@code GET /api/v1/progresion/{idExterno}}: estado, historial de e1RM y lo que toca la próxima vez. */
public record ProgresionEjercicio(String idExterno, Estado estado, @Nullable BigDecimal cargaSiguienteKg, int series,
		int repeticionesMin, int repeticionesMax, String motivo, List<PuntoE1rm> historial) {

	public ProgresionEjercicio {
		historial = List.copyOf(historial);
	}
}
