package es.forja.entreno.plan.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.forja.comun.seguridad.UsuarioAutenticado;
import es.forja.entreno.plan.modelo.PlanActual;
import es.forja.entreno.plan.servicio.ServicioPlan;

/** El plan versionado del usuario autenticado (ADR 0008 §5 y §6). */
@RestController
@RequestMapping("/api/v1/planes")
public class PlanesControlador {

	private final ServicioPlan planes;

	public PlanesControlador(ServicioPlan planes) {
		this.planes = planes;
	}

	/** El último plan, con lo necesario para entrenar sin red. 404 si el usuario no ha generado ninguno. */
	@GetMapping("/actual")
	public PlanActual actual(@AuthenticationPrincipal UsuarioAutenticado actual) {
		return planes.actual(actual.usuario());
	}
}
