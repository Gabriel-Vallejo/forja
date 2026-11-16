package es.forja.cuenta.modelo;

import com.fasterxml.jackson.annotation.JsonValue;

/** Tipo de cliente del dispositivo; coincide con el ENUM de la tabla {@code dispositivo}. */
public enum Plataforma {
	MOVIL("movil"), PC("pc");

	private final String codigo;

	Plataforma(String codigo) {
		this.codigo = codigo;
	}

	@JsonValue
	public String codigo() {
		return codigo;
	}

	public static Plataforma deCodigo(String codigo) {
		for (Plataforma p : values()) {
			if (p.codigo.equals(codigo)) {
				return p;
			}
		}
		throw new IllegalArgumentException("plataforma desconocida");
	}
}
