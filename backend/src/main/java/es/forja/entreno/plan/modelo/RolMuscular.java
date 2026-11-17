package es.forja.entreno.plan.modelo;

import com.fasterxml.jackson.annotation.JsonValue;

/** Cuánto trabaja un músculo en un ejercicio; coincide con {@code ejercicio_musculo.rol}. */
public enum RolMuscular {
	PRIMARIO("primario"),
	SECUNDARIO("secundario");

	private final String codigo;

	RolMuscular(String codigo) {
		this.codigo = codigo;
	}

	@JsonValue
	public String codigo() {
		return codigo;
	}

	public static RolMuscular deCodigo(String codigo) {
		for (RolMuscular rol : values()) {
			if (rol.codigo.equals(codigo)) {
				return rol;
			}
		}
		throw new IllegalArgumentException("Rol de músculo desconocido: " + codigo);
	}
}
