package es.forja.entreno.rutina.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.forja.comun.seguridad.UsuarioAutenticado;
import es.forja.entreno.plan.modelo.PlanGenerado;
import es.forja.entreno.plan.servicio.ServicioPlan;
import es.forja.entreno.rutina.modelo.SolicitudRutina;
import es.forja.entreno.rutina.servicio.ServicioRutina;
import jakarta.validation.Valid;

/**
 * Rutina del usuario autenticado con CP-SAT (ADR 0002). Devuelve el plan y su traza, y guarda el plan como una
 * versión nueva (ADR 0008 §6). Las limitaciones son dato de salud: nada de esto se escribe en el log.
 */
@RestController
@RequestMapping("/api/v1/rutinas")
public class RutinasControlador {

	private final ServicioRutina rutinas;
	private final ServicioPlan planes;

	public RutinasControlador(ServicioRutina rutinas, ServicioPlan planes) {
		this.rutinas = rutinas;
		this.planes = planes;
	}

	@PostMapping("/generar")
	public PlanGenerado generar(@AuthenticationPrincipal UsuarioAutenticado actual,
			@Valid @RequestBody SolicitudRutina solicitud) {
		return planes.guardar(actual.usuario(), rutinas.generar(actual.usuario(), solicitud));
	}
}
