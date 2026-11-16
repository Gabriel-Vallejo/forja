package es.forja.entreno.plan.modelo;

import java.math.BigDecimal;

/**
 * Parámetros de la máquina de estados (RF-26); salen de {@code forja.progresion.*}.
 *
 * @param e1rmRepeticionesMaximas Epley no es fiable por encima: esas series no cuentan para el e1RM
 * @param pesoCorporalTopeRepeticiones con peso corporal el rango sube +1 rep hasta este tope
 */
public record ParametrosProgresion(Deload deload, int e1rmRepeticionesMaximas, int pesoCorporalTopeRepeticiones) {

	/**
	 * @param semanas ventana de estancamiento, en semanas
	 * @param sesionesMinimas sesiones que hacen falta en la ventana para poder hablar de estancamiento
	 * @param mejoraMinimaPorcentaje el mejor e1RM de la ventana tiene que superar en más de esto al anterior
	 * @param factorCarga carga del deload respecto a la de antes (0,9 = 90 %)
	 * @param factorSeries series del deload respecto a las del plan (0,5 = la mitad, redondeando hacia arriba)
	 * @param redondeoKg la carga del deload se redondea al múltiplo más cercano de esto
	 */
	public record Deload(int semanas, int sesionesMinimas, BigDecimal mejoraMinimaPorcentaje,
			BigDecimal factorCarga, BigDecimal factorSeries, BigDecimal redondeoKg) {
	}
}
