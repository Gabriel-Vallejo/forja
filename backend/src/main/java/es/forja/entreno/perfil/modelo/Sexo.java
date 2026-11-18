package es.forja.entreno.perfil.modelo;

import java.util.Arrays;
import java.util.Locale;

/**
 * Sexo para la fórmula de % de grasa y los estándares de fuerza. {@code NO_INDICA} es «prefiero no decirlo»: sin él no
 * hay estimación. Los códigos coinciden con el ENUM de la tabla.
 */
public enum Sexo {
	HOMBRE, MUJER, NO_INDICA;

	public String codigo() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static Sexo desdeCodigo(String codigo) {
		return Arrays.stream(values()).filter(s -> s.codigo().equals(codigo)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Sexo desconocido"));
	}
}
