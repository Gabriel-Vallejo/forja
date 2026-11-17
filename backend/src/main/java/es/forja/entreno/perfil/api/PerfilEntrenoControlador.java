package es.forja.entreno.perfil.api;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.forja.comun.seguridad.UsuarioAutenticado;
import es.forja.entreno.perfil.modelo.AjustesEntreno;
import es.forja.entreno.perfil.modelo.PerfilEntreno;
import es.forja.entreno.perfil.repositorio.PerfilEntrenoRepositorio.Opcion;
import es.forja.entreno.perfil.servicio.ServicioPerfilEntreno;
import jakarta.validation.Valid;

/** Ajustes del plan del usuario autenticado y el catálogo de material que la app ofrece. No son datos de salud. */
@RestController
@RequestMapping("/api/v1")
public class PerfilEntrenoControlador {

	private final ServicioPerfilEntreno perfil;

	public PerfilEntrenoControlador(ServicioPerfilEntreno perfil) {
		this.perfil = perfil;
	}

	/** Sin ajustes guardados devuelve los de por defecto, con {@code porDefecto: true}. */
	@GetMapping("/perfil/entreno")
	public PerfilEntreno obtener(@AuthenticationPrincipal UsuarioAutenticado actual) {
		return perfil.obtener(actual.usuario());
	}

	@PutMapping("/perfil/entreno")
	public PerfilEntreno guardar(@AuthenticationPrincipal UsuarioAutenticado actual,
			@Valid @RequestBody AjustesEntreno ajustes) {
		return perfil.guardar(actual.usuario(), ajustes);
	}

	@GetMapping("/catalogo/materiales")
	public List<Opcion> materiales() {
		return perfil.materiales();
	}
}
