package es.forja.cuenta.modelo;

import java.time.Instant;

/** Fila de {@code token_un_uso} (sin el hash). */
public record TokenUnUso(long id, long usuarioId, TipoTokenUnUso tipo, Instant caducaEn, Instant usadoEn) {
}
