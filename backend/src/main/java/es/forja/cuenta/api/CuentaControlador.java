package es.forja.cuenta.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import es.forja.comun.seguridad.UsuarioAutenticado;
import es.forja.cuenta.servicio.ServicioCuenta;
import es.forja.cuenta.servicio.ServicioDispositivos;
import jakarta.validation.Valid;

/** Rutas autenticadas: dispositivos de la cuenta y borrado de la cuenta. */
@RestController
@RequestMapping("/api/v1")
public class CuentaControlador {

	private final ServicioDispositivos dispositivos;
	private final ServicioCuenta cuenta;

	public CuentaControlador(ServicioDispositivos dispositivos, ServicioCuenta cuenta) {
		this.dispositivos = dispositivos;
		this.cuenta = cuenta;
	}

	@GetMapping("/dispositivos")
	public List<Respuestas.DatosDispositivo> listar(@AuthenticationPrincipal UsuarioAutenticado actual) {
		return dispositivos.listar(actual.usuario(), actual.dispositivo()).stream()
				.map(Respuestas.DatosDispositivo::de).toList();
	}

	@DeleteMapping("/dispositivos/{uuid}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public ResponseEntity<Void> revocar(@AuthenticationPrincipal UsuarioAutenticado actual,
			@PathVariable UUID uuid) {
		dispositivos.revocar(actual.usuario(), uuid);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/cuenta")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public ResponseEntity<Void> borrar(@AuthenticationPrincipal UsuarioAutenticado actual,
			@Valid @RequestBody Solicitudes.BorrarCuenta cuerpo) {
		cuenta.borrar(actual.usuario(), cuerpo.contrasena());
		return ResponseEntity.noContent().build();
	}
}
