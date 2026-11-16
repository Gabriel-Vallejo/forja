package es.forja.restricciones.servicio;

import java.util.UUID;

import es.forja.restricciones.modelo.Restriccion;

/** Una restricción del usuario con el identificador público con el que la API la nombra. */
public record RestriccionGuardada(UUID uuid, Restriccion restriccion) {
}
