package es.forja.entreno.plan.modelo;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import es.forja.entreno.rutina.modelo.PlanRutina;

/** Respuesta de generar la rutina: el plan de siempre más el uuid de la versión que se ha guardado. */
public record PlanGenerado(UUID uuid, @JsonUnwrapped PlanRutina plan) {
}
