package es.forja.cuenta.modelo;

import java.util.UUID;

/** Fila de {@code usuario}. El hash de la contraseña no sale nunca en {@code toString()}. */
public record Usuario(long id, UUID uuid, String correo, String hashContrasena, boolean verificado,
		int anioNacimiento, boolean mayorDeEdad) {

	@Override
	public String toString() {
		return "Usuario[id=" + id + "]";
	}
}
