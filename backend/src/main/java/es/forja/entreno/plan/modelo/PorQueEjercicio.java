package es.forja.entreno.plan.modelo;

import java.util.List;

import es.forja.entreno.rutina.solver.ReglasExplicadas.Razon;

/**
 * «Por qué este ejercicio» de un ejercicio del plan actual, la razón que más pesó primero. Se calcula al pedirlo y no
 * se guarda: puede citar las limitaciones o la movilidad del usuario (ADR 0004).
 */
public record PorQueEjercicio(String idExterno, int dia, List<Razon> porQue) {

	public PorQueEjercicio {
		porQue = List.copyOf(porQue);
	}
}
