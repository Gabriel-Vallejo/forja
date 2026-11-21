package es.forja.cuenta.servicio;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import es.forja.comun.seguridad.DispositivosActivos;
import es.forja.cuenta.modelo.Usuario;
import es.forja.cuenta.repositorio.DispositivoRepositorio;
import es.forja.cuenta.repositorio.TokenRefrescoRepositorio;
import es.forja.cuenta.repositorio.UsuarioRepositorio;

/** Borrado de la cuenta (ADR 0009 §9) y la consulta que usa el filtro JWT. */
@Service
public class ServicioCuenta implements DispositivosActivos {

	private static final String ACCION_BORRAR = "borrar";

	/** Lo que el usuario dio al registrarse y cuándo; sin el hash de la contraseña ni los contadores de intentos. */
	public record DatosCuenta(String correo, boolean verificado, int anioNacimiento, Instant creadoEn) {
	}

	private final UsuarioRepositorio usuarios;
	private final DispositivoRepositorio dispositivos;
	private final PasswordEncoder codificador;
	private final LimitadorIntentos limitador;
	private final TokenRefrescoRepositorio refrescos;
	private final TransactionTemplate transaccion;
	private final Clock reloj;

	public ServicioCuenta(UsuarioRepositorio usuarios, DispositivoRepositorio dispositivos,
			PasswordEncoder codificador, LimitadorIntentos limitador, TokenRefrescoRepositorio refrescos,
			TransactionTemplate transaccion, Clock reloj) {
		this.usuarios = usuarios;
		this.dispositivos = dispositivos;
		this.codificador = codificador;
		this.limitador = limitador;
		this.refrescos = refrescos;
		this.transaccion = transaccion;
		this.reloj = reloj;
	}

	@Override
	public boolean estaActivo(UUID dispositivo, UUID usuario) {
		return dispositivos.estaVivo(dispositivo, usuario);
	}

	public DatosCuenta datos(UUID usuarioUuid) {
		Usuario usuario = usuarios.porUuid(usuarioUuid)
				.orElseThrow(() -> ErrorCuenta.noAutenticado("La sesión ya no es válida."));
		return new DatosCuenta(usuario.correo(), usuario.verificado(), usuario.anioNacimiento(),
				usuarios.creadoEn(usuario.id()).orElseThrow());
	}

	/**
	 * Pide la contraseña otra vez y borra la cuenta; las claves foráneas en cascada se llevan el resto. Antes aparta los
	 * hashes de sus refrescos vivos para que los otros dispositivos reciban 410 y borren lo que guardan.
	 */
	public void borrar(UUID usuarioUuid, String contrasena) {
		limitador.exigirPorClave(ACCION_BORRAR, usuarioUuid.toString());
		Usuario usuario = usuarios.porUuid(usuarioUuid)
				.orElseThrow(() -> ErrorCuenta.noAutenticado("La sesión ya no es válida."));
		if (!codificador.matches(contrasena, usuario.hashContrasena())) {
			throw ErrorCuenta.prohibido("La contraseña no es correcta.");
		}
		transaccion.executeWithoutResult(estado -> {
			refrescos.apartarDeCuentaBorrada(usuario.id(), reloj.instant());
			usuarios.borrar(usuario.id());
		});
	}
}
