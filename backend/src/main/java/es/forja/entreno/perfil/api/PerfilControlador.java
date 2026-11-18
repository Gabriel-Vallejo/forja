package es.forja.entreno.perfil.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.forja.comun.seguridad.UsuarioAutenticado;
import es.forja.entreno.perfil.modelo.DatosPerfil;
import es.forja.entreno.perfil.modelo.MedidasCuerpo;
import es.forja.entreno.perfil.modelo.MedidasPerfil;
import es.forja.entreno.perfil.servicio.ServicioMedidas;
import es.forja.entreno.perfil.servicio.ServicioPerfilDatos;
import jakarta.validation.Valid;

/**
 * Perfil manual del usuario autenticado. Dos recursos porque tienen reglas distintas: {@code datos} son
 * preferencias en claro; {@code medidas} son datos de salud, con consentimiento, cifradas y sin nada en el log.
 */
@RestController
@RequestMapping("/api/v1/perfil")
public class PerfilControlador {

	private final ServicioPerfilDatos datos;
	private final ServicioMedidas medidas;

	public PerfilControlador(ServicioPerfilDatos datos, ServicioMedidas medidas) {
		this.datos = datos;
		this.medidas = medidas;
	}

	@GetMapping("/datos")
	public DatosPerfil obtenerDatos(@AuthenticationPrincipal UsuarioAutenticado actual) {
		return datos.obtener(actual.usuario());
	}

	@PutMapping("/datos")
	public DatosPerfil guardarDatos(@AuthenticationPrincipal UsuarioAutenticado actual,
			@Valid @RequestBody DatosPerfil cuerpo) {
		return datos.guardar(actual.usuario(), cuerpo);
	}

	/** Con el consentimiento de {@code medidas_salud}; sin él, 403. Sin medidas guardadas, todo a {@code null}. */
	@GetMapping("/medidas")
	public MedidasPerfil obtenerMedidas(@AuthenticationPrincipal UsuarioAutenticado actual) {
		return medidas.obtener(actual.usuario());
	}

	/** Reemplaza las cinco medidas: un {@code null} borra esa medida. */
	@PutMapping("/medidas")
	public MedidasPerfil guardarMedidas(@AuthenticationPrincipal UsuarioAutenticado actual,
			@Valid @RequestBody MedidasCuerpo cuerpo) {
		return medidas.guardar(actual.usuario(), cuerpo);
	}
}
