package es.forja.cuenta.servicio;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import es.forja.cuenta.modelo.TipoTokenUnUso;
import es.forja.cuenta.modelo.Usuario;
import es.forja.cuenta.modelo.ReglasCuenta;
import es.forja.cuenta.repositorio.DispositivoRepositorio;
import es.forja.cuenta.repositorio.TokenRefrescoRepositorio;
import es.forja.cuenta.repositorio.UsuarioRepositorio;

/** Recuperar y restablecer la contraseña (ADR 0009 §5). */
@Service
public class ServicioRecuperacion {

	public static final Duration VIDA_TOKEN_RECUPERACION = Duration.ofMinutes(30);
	private static final Logger LOG = LoggerFactory.getLogger(ServicioRecuperacion.class);
	private static final String ACCION = "recuperar";

	private final UsuarioRepositorio usuarios;
	private final DispositivoRepositorio dispositivos;
	private final TokenRefrescoRepositorio refrescos;
	private final ServicioTokensUnUso tokens;
	private final PoliticaContrasena politica;
	private final PasswordEncoder codificador;
	private final EnvioCorreo correo;
	private final LimitadorIntentos limitador;
	private final TransactionTemplate transaccion;
	private final Clock reloj;

	public ServicioRecuperacion(UsuarioRepositorio usuarios, DispositivoRepositorio dispositivos,
			TokenRefrescoRepositorio refrescos, ServicioTokensUnUso tokens, PoliticaContrasena politica,
			PasswordEncoder codificador, EnvioCorreo correo, LimitadorIntentos limitador,
			TransactionTemplate transaccion, Clock reloj) {
		this.usuarios = usuarios;
		this.dispositivos = dispositivos;
		this.refrescos = refrescos;
		this.tokens = tokens;
		this.politica = politica;
		this.codificador = codificador;
		this.correo = correo;
		this.limitador = limitador;
		this.transaccion = transaccion;
		this.reloj = reloj;
	}

	/** No dice nunca si la cuenta existe: el resultado es el mismo en los dos casos. */
	public void recuperar(String correoSinNormalizar, String ip) {
		String direccion = ReglasCuenta.normalizarCorreo(correoSinNormalizar);
		limitador.exigir(ACCION, ip, direccion);
		usuarios.porCorreo(direccion).ifPresent(usuario -> {
			String token = tokens.crear(usuario.id(), TipoTokenUnUso.RECUPERACION, VIDA_TOKEN_RECUPERACION);
			enviarSinFallar(usuario, token);
		});
	}

	private void enviarSinFallar(Usuario usuario, String token) {
		try {
			correo.enviarRecuperacion(usuario.correo(), token);
		} catch (RuntimeException e) {
			LOG.error("fallo al enviar el correo de recuperación ({})", e.getClass().getSimpleName());
		}
	}

	/** Cambia la contraseña con el enlace y cierra todas las sesiones de la cuenta. */
	public void restablecer(String token, String contrasenaNueva) {
		politica.exigir(contrasenaNueva);
		String hash = codificador.encode(contrasenaNueva);
		Instant ahora = reloj.instant();
		transaccion.executeWithoutResult(estado -> {
			long usuarioId = tokens.consumir(token, TipoTokenUnUso.RECUPERACION);
			usuarios.cambiarContrasena(usuarioId, hash);
			refrescos.revocarDeUsuario(usuarioId, ahora);
			dispositivos.revocarTodosDeUsuario(usuarioId, ahora);
		});
	}
}
