package es.forja.cuenta.servicio;

import java.time.Instant;
import java.util.UUID;

/** Lo que recibe el cliente al entrar o refrescar. Los tokens no salen nunca en {@code toString()}. */
public record Sesion(String tokenAcceso, String tokenRefresco, Instant expiraEn, UUID dispositivo) {

	@Override
	public String toString() {
		return "Sesion[dispositivo=" + dispositivo + "]";
	}
}
