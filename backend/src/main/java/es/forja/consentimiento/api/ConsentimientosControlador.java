package es.forja.consentimiento.api;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import es.forja.comun.api.ErrorNegocio;
import es.forja.comun.seguridad.UsuarioAutenticado;
import es.forja.consentimiento.modelo.Consentimiento;
import es.forja.consentimiento.modelo.TipoConsentimiento;
import es.forja.consentimiento.servicio.ServicioConsentimientos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/** Consentimientos del usuario autenticado (ADR 0004 §3): ver los vivos, darlos y revocarlos. */
@RestController
@RequestMapping("/api/v1/consentimientos")
public class ConsentimientosControlador {

	/** Lo que el cliente manda al dar un consentimiento: el tipo y la versión del texto que enseñó. */
	public record SolicitudConsentimiento(@NotBlank String tipo, @NotBlank String versionTexto) {
	}

	public record RespuestaConsentimiento(String tipo, String versionTexto, Instant dadoEn) {
		static RespuestaConsentimiento de(Consentimiento c) {
			return new RespuestaConsentimiento(c.tipo().codigo(), c.versionTexto(), c.dadoEn());
		}
	}

	private final ServicioConsentimientos consentimientos;

	public ConsentimientosControlador(ServicioConsentimientos consentimientos) {
		this.consentimientos = consentimientos;
	}

	@GetMapping
	public List<RespuestaConsentimiento> listar(@AuthenticationPrincipal UsuarioAutenticado actual) {
		return consentimientos.listar(actual.usuario()).stream().map(RespuestaConsentimiento::de).toList();
	}

	@PostMapping
	public RespuestaConsentimiento dar(@AuthenticationPrincipal UsuarioAutenticado actual,
			@Valid @RequestBody SolicitudConsentimiento cuerpo) {
		return RespuestaConsentimiento
				.de(consentimientos.dar(actual.usuario(), tipo(cuerpo.tipo()), cuerpo.versionTexto()));
	}

	@DeleteMapping("/{tipo}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public ResponseEntity<Void> revocar(@AuthenticationPrincipal UsuarioAutenticado actual,
			@PathVariable String tipo) {
		consentimientos.revocar(actual.usuario(), tipo(tipo));
		return ResponseEntity.noContent().build();
	}

	private static TipoConsentimiento tipo(String codigo) {
		try {
			return TipoConsentimiento.desdeCodigo(codigo);
		} catch (IllegalArgumentException e) {
			throw ErrorNegocio.datosNoValidos(e.getMessage());
		}
	}
}
