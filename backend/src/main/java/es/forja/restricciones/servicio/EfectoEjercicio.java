package es.forja.restricciones.servicio;

import es.forja.restricciones.filtro.Decision;
import es.forja.restricciones.filtro.EjercicioCandidato;

/**
 * Qué le pasa a un ejercicio con las restricciones del usuario.
 *
 * @param sustituto el ejercicio que ocupa su lugar, solo si la decisión es una sustitución
 */
public record EfectoEjercicio(EjercicioCandidato ejercicio, Decision decision, EjercicioCandidato sustituto) {
}
