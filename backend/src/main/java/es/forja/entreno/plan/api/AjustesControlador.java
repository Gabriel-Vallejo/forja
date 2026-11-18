package es.forja.entreno.plan.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.forja.comun.seguridad.UsuarioAutenticado;
import es.forja.entreno.plan.modelo.Ajuste;
import es.forja.entreno.plan.modelo.PlanGenerado;
import es.forja.entreno.plan.modelo.PreferenciasGuardadas;
import es.forja.entreno.plan.modelo.PreferenciasPlan;
import es.forja.entreno.plan.modelo.VistaPrevia;
import es.forja.entreno.plan.servicio.ServicioAjustes;
import jakarta.validation.Valid;

/**
 * Ajustes del plan (PRD v0.1): vista previa sin guardar, aplicar como versión nueva y las preferencias guardadas que
 * respeta cada «Regenerar». Resolver tarda segundos: la vista previa y aplicar gastan un intento cada una.
 */
@RestController
@RequestMapping("/api/v1/planes")
public class AjustesControlador {

	private final ServicioAjustes ajustes;

	public AjustesControlador(ServicioAjustes ajustes) {
		this.ajustes = ajustes;
	}

	/** El plan con el ajuste y lo que cambia frente al actual. No guarda nada. 409 con bandera roja activa. */
	@PostMapping("/actual/ajustes/vista-previa")
	public VistaPrevia vistaPrevia(@AuthenticationPrincipal UsuarioAutenticado actual,
			@Valid @RequestBody Ajuste ajuste) {
		return ajustes.vistaPrevia(actual.usuario(), ajuste);
	}

	/** Guarda las preferencias y el plan de la vista previa como versión nueva. 409 si el plan ha cambiado. */
	@PostMapping("/actual/ajustes")
	public PlanGenerado aplicar(@AuthenticationPrincipal UsuarioAutenticado actual,
			@Valid @RequestBody Ajuste ajuste) {
		return ajustes.aplicar(actual.usuario(), ajuste);
	}

	@GetMapping("/preferencias")
	public PreferenciasGuardadas preferencias(@AuthenticationPrincipal UsuarioAutenticado actual) {
		return ajustes.preferencias(actual.usuario());
	}

	/** Reemplaza las preferencias (por ejemplo, para quitar una). No cambia el plan: cuenta al regenerar. */
	@PutMapping("/preferencias")
	public PreferenciasGuardadas guardar(@AuthenticationPrincipal UsuarioAutenticado actual,
			@Valid @RequestBody PreferenciasPlan preferencias) {
		return ajustes.guardarPreferencias(actual.usuario(), preferencias);
	}
}
