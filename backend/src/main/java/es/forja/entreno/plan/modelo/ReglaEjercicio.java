package es.forja.entreno.plan.modelo;

import java.math.BigDecimal;

/**
 * La regla de un ejercicio en el plan: objetivo y regla de la siguiente.
 *
 * @param incrementoKg 0 con peso corporal: la progresión es por repeticiones
 * @param cargaInicial la que guardó el plan; nula hasta la primera sesión
 */
public record ReglaEjercicio(int series, int repeticionesMin, int repeticionesMax, int rirObjetivo,
		BigDecimal incrementoKg, BigDecimal cargaInicial) {

	public boolean pesoCorporal() {
		return incrementoKg.signum() == 0;
	}
}
