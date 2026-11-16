package es.forja.comun.seguridad;

import java.util.UUID;

/** Lo que la seguridad necesita saber de las cuentas, sin tocar sus tablas (la implementa {@code cuenta}). */
public interface DispositivosActivos {

	/** Cierto si el dispositivo existe, es de ese usuario y no está revocado. */
	boolean estaActivo(UUID dispositivo, UUID usuario);
}
