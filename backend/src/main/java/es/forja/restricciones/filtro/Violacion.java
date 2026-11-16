package es.forja.restricciones.filtro;

import es.forja.restricciones.modelo.Restriccion;

/** Restricción que un ejercicio incumple; {@code dura} si hay que excluirlo en vez de sustituirlo. */
record Violacion(Restriccion restriccion, boolean dura) {
}
