package es.forja.restricciones.bloqueo;

/** Cuestionario que puso el bloqueo; cada uno gestiona solo el suyo. */
public enum OrigenBloqueo {
	CRIBADO, BANDERAS;

	String columna() {
		return name().toLowerCase(java.util.Locale.ROOT);
	}
}
