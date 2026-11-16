package es.forja.cuenta.modelo;

import java.time.Instant;
import java.util.UUID;

/** Fila de {@code token_refresco} (sin el hash, que no hace falta fuera del repositorio). */
public record TokenRefresco(long id, long dispositivoId, UUID familia, Instant caducaEn, Instant usadoEn,
		Instant revocadoEn) {

	public boolean gastado() {
		return usadoEn != null || revocadoEn != null;
	}
}
