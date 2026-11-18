package es.forja.entreno.plan.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.forja.comun.seguridad.UsuarioAutenticado;
import es.forja.entreno.plan.modelo.PlanActual;
import es.forja.entreno.plan.modelo.PorQueEjercicio;
import es.forja.entreno.plan.servicio.ServicioPlan;
import es.forja.entreno.plan.servicio.ServicioPorQue;

/** El plan versionado del usuario autenticado (ADR 0008 §5 y §6). */
@RestController
@RequestMapping("/api/v1/planes")
public class PlanesControlador {

	private final ServicioPlan planes;
	private final ServicioPorQue porQue;

	public PlanesControlador(ServicioPlan planes, ServicioPorQue porQue) {
		this.planes = planes;
		this.porQue = porQue;
	}

	/** El último plan, con lo necesario para entrenar sin red. 404 si el usuario no ha generado ninguno. */
	@GetMapping("/actual")
	public PlanActual actual(@AuthenticationPrincipal UsuarioAutenticado actual) {
		return planes.actual(actual.usuario());
	}

	/**
	 * «Por qué este ejercicio» de un ejercicio del último plan en ese día, como mucho 4 razones con su fuente y su nivel.
	 * Va aparte de {@code /actual} para no guardarlo en el móvil con el plan: puede citar limitaciones o movilidad, y
	 * así deja de salir en cuanto se retira el consentimiento. 404 si no hay plan o el ejercicio no está ese día.
	 */
	@GetMapping("/actual/ejercicios/{dia}/{idExterno}/por-que")
	public PorQueEjercicio porQue(@AuthenticationPrincipal UsuarioAutenticado actual, @PathVariable int dia,
			@PathVariable String idExterno) {
		return porQue.calcular(actual.usuario(), dia, idExterno);
	}
}
