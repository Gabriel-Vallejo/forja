package es.forja.restricciones.filtro;

/** Rango de movimiento de la variante; el orden va de más a menos rango. */
public enum RangoMovimiento {

	COMPLETO, REDUCIDO;

	/** Esta variante recorre menos rango que {@code otro}. */
	boolean esMasCortoQue(RangoMovimiento otro) {
		return ordinal() > otro.ordinal();
	}

}
