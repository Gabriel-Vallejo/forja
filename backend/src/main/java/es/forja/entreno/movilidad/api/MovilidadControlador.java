package es.forja.entreno.movilidad.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.forja.comun.seguridad.UsuarioAutenticado;
import es.forja.entreno.movilidad.modelo.Movilidad;
import es.forja.entreno.movilidad.modelo.TramosMovilidad;
import es.forja.entreno.movilidad.servicio.ServicioMovilidad;

/**
 * Tests de movilidad del usuario autenticado (RF-08), con el consentimiento de {@code medidas_salud}. Sin datos, los
 * tres tests van a {@code null}. Nada de esto se escribe en el log.
 */
@RestController
@RequestMapping("/api/v1/movilidad")
public class MovilidadControlador {

	private final ServicioMovilidad movilidad;

	public MovilidadControlador(ServicioMovilidad movilidad) {
		this.movilidad = movilidad;
	}

	@GetMapping
	public Movilidad obtener(@AuthenticationPrincipal UsuarioAutenticado actual) {
		return movilidad.obtener(actual.usuario());
	}

	/** Reemplaza los tres: un tramo nulo deja ese test sin hacer. */
	@PutMapping
	public Movilidad guardar(@AuthenticationPrincipal UsuarioAutenticado actual, @RequestBody TramosMovilidad cuerpo) {
		return movilidad.guardar(actual.usuario(), cuerpo);
	}
}
