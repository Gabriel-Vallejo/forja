package es.forja.entreno.movilidad.modelo;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Resultado de un test de movilidad en tres tramos (ADR 0007). Los umbrales de cada tramo están en la app, que hace
 * la pregunta cerrada; aquí solo llega el tramo. Los códigos coinciden con el ENUM de la tabla.
 */
public enum Tramo {
	LIMITADO, MEDIO, BUENO;

	public String codigo() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static Optional<Tramo> desdeCodigo(String codigo) {
		return Arrays.stream(values()).filter(t -> t.codigo().equals(codigo)).findFirst();
	}
}
