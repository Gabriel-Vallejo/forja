package es.forja.cuenta.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import es.forja.comun.seguridad.UsuarioAutenticado;
import es.forja.cuenta.servicio.ServicioRecuperacion;
import es.forja.cuenta.servicio.ServicioRegistro;
import es.forja.cuenta.servicio.ServicioSesion;
import es.forja.cuenta.servicio.ServicioSesion.DatosDispositivo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/** Rutas abiertas de autenticación, salvo {@code cerrar-sesion}, que pide el token de acceso. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthControlador {

	private static final Respuestas.Mensaje ALTA_ACEPTADA = new Respuestas.Mensaje(
			"Si el correo es válido, te hemos enviado un enlace para verificar la cuenta.");
	private static final Respuestas.Mensaje RECUPERACION_ACEPTADA = new Respuestas.Mensaje(
			"Si existe una cuenta con ese correo, te hemos enviado un enlace para cambiar la contraseña.");

	private final ServicioRegistro registro;
	private final ServicioSesion sesiones;
	private final ServicioRecuperacion recuperacion;

	public AuthControlador(ServicioRegistro registro, ServicioSesion sesiones, ServicioRecuperacion recuperacion) {
		this.registro = registro;
		this.sesiones = sesiones;
		this.recuperacion = recuperacion;
	}

	@PostMapping("/registro")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public ResponseEntity<Respuestas.Mensaje> registrar(@Valid @RequestBody Solicitudes.Registro cuerpo,
			HttpServletRequest peticion) {
		// shortcut: la IP sale de getRemoteAddr(); detrás de Caddy habrá que configurar las cabeceras
		// reenviadas (server.forward-headers-strategy) para no limitar a todos como si fueran el proxy.
		registro.registrar(cuerpo.correo(), cuerpo.contrasena(), cuerpo.fechaNacimiento(),
				peticion.getRemoteAddr());
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(ALTA_ACEPTADA);
	}

	@PostMapping("/verificar")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public ResponseEntity<Void> verificar(@Valid @RequestBody Solicitudes.Token cuerpo) {
		registro.verificar(cuerpo.token());
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/login")
	public Respuestas.DatosSesion entrar(@Valid @RequestBody Solicitudes.Login cuerpo, HttpServletRequest peticion) {
		var dispositivo = new DatosDispositivo(cuerpo.dispositivoId(), cuerpo.nombreDispositivo().strip(),
				cuerpo.plataforma());
		return Respuestas.DatosSesion.de(
				sesiones.entrar(cuerpo.correo(), cuerpo.contrasena(), dispositivo, peticion.getRemoteAddr()));
	}

	@PostMapping("/refresco")
	public Respuestas.DatosSesion refrescar(@Valid @RequestBody Solicitudes.Refresco cuerpo) {
		return Respuestas.DatosSesion.de(sesiones.refrescar(cuerpo.tokenRefresco()));
	}

	@PostMapping("/recuperar")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public ResponseEntity<Respuestas.Mensaje> recuperar(@Valid @RequestBody Solicitudes.Recuperar cuerpo,
			HttpServletRequest peticion) {
		recuperacion.recuperar(cuerpo.correo(), peticion.getRemoteAddr());
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(RECUPERACION_ACEPTADA);
	}

	@PostMapping("/restablecer")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public ResponseEntity<Void> restablecer(@Valid @RequestBody Solicitudes.Restablecer cuerpo) {
		recuperacion.restablecer(cuerpo.token(), cuerpo.contrasenaNueva());
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/cerrar-sesion")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public ResponseEntity<Void> cerrarSesion(@AuthenticationPrincipal UsuarioAutenticado actual) {
		sesiones.cerrar(actual.usuario(), actual.dispositivo());
		return ResponseEntity.noContent().build();
	}
}
