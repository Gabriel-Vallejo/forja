package es.forja.entreno.plan.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.forja.comun.seguridad.UsuarioAutenticado;
import es.forja.entreno.plan.modelo.ProgresionEjercicio;
import es.forja.entreno.plan.servicio.ServicioPlan;

/** Progresión de un ejercicio del usuario autenticado (RF-26). */
@RestController
@RequestMapping("/api/v1/progresion")
public class ProgresionControlador {

	private final ServicioPlan planes;

	public ProgresionControlador(ServicioPlan planes) {
		this.planes = planes;
	}

	/** Estado, historial de e1RM por sesión y carga siguiente. Sin sesiones: 200 con el historial vacío. */
	@GetMapping("/{idExterno}")
	public ProgresionEjercicio progresion(@AuthenticationPrincipal UsuarioAutenticado actual,
			@PathVariable String idExterno) {
		return planes.progresion(actual.usuario(), idExterno);
	}
}
