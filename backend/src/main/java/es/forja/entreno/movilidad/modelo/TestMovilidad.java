package es.forja.entreno.movilidad.modelo;

import java.util.Locale;

/** Los tres tests caseros de movilidad (RF-08, ADR 0007). Los códigos coinciden con el ENUM de la tabla. */
public enum TestMovilidad {
	/** Rodilla a la pared: dorsiflexión de tobillo. */
	TOBILLO,
	/** Rotación de cadera sentado. */
	CADERA,
	/** Flexión de hombro tumbado. */
	HOMBRO;

	public String codigo() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static TestMovilidad desdeCodigo(String codigo) {
		return valueOf(codigo.toUpperCase(Locale.ROOT));
	}
}
