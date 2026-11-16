package es.forja.entreno.rutina.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.forja.comun.seguridad.UsuarioAutenticado;
import es.forja.entreno.rutina.modelo.PlanRutina;
import es.forja.entreno.rutina.modelo.SolicitudRutina;
import es.forja.entreno.rutina.servicio.ServicioRutina;
import jakarta.validation.Valid;

/**
 * Rutina del usuario autenticado con CP-SAT (ADR 0002). Devuelve el plan y su traza; no guarda nada. Las
 * limitaciones son dato de salud: nada de esto se escribe en el log.
 */
@RestController
@RequestMapping("/api/v1/rutinas")
public class RutinasControlador {

	private final ServicioRutina rutinas;

	public RutinasControlador(ServicioRutina rutinas) {
		this.rutinas = rutinas;
	}

	@PostMapping("/generar")
	public PlanRutina generar(@AuthenticationPrincipal UsuarioAutenticado actual,
			@Valid @RequestBody SolicitudRutina solicitud) {
		return rutinas.generar(actual.usuario(), solicitud);
	}
}
