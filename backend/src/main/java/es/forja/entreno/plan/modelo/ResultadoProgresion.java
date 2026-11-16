package es.forja.entreno.plan.modelo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Qué toca la próxima vez en un ejercicio.
 *
 * @param cargaKg nula si nunca se ha entrenado y el plan no trae una carga inicial
 * @param series las del plan, o las del deload si toca
 * @param motivo explicación en español, de tú
 * @param historial e1RM de cada sesión, de la más antigua a la más reciente
 */
public record ResultadoProgresion(Estado estado, BigDecimal cargaKg, int series, int repeticionesMin,
		int repeticionesMax, String motivo, List<PuntoE1rm> historial) {

	public ResultadoProgresion {
		historial = List.copyOf(historial);
	}
}
