package es.forja.cuenta.modelo;

import java.time.Instant;
import java.util.UUID;

/** Fila de {@code dispositivo}; {@code uuid} es el {@code sid} del token de acceso. */
public record Dispositivo(long id, UUID uuid, long usuarioId, String nombre, Plataforma plataforma,
		Instant ultimaActividad, Instant revocadoEn) {
}
