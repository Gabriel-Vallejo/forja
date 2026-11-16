package es.forja.restricciones.modelo;

/** Lado del cuerpo al que afecta la restricción. */
public enum Lado implements ConCodigo {
	IZQUIERDO, DERECHO, AMBOS, NO_APLICA;

	public static Lado desdeCodigo(String codigo) {
		return ConCodigo.buscar(Lado.class, codigo);
	}

}
