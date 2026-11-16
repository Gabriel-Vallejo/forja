package es.forja.cuenta.servicio;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import es.forja.cuenta.modelo.ReglasCuenta;
import es.forja.cuenta.modelo.TipoTokenUnUso;
import es.forja.cuenta.repositorio.UsuarioRepositorio;

/** Alta de cuenta y verificación del correo (ADR 0009 §1). */
@Service
public class ServicioRegistro {

	public static final Duration VIDA_TOKEN_VERIFICACION = Duration.ofHours(24);
	private static final Logger LOG = LoggerFactory.getLogger(ServicioRegistro.class);
	private static final String ACCION = "registro";

	private final UsuarioRepositorio usuarios;
	private final ServicioTokensUnUso tokens;
	private final PoliticaContrasena politica;
	private final PasswordEncoder codificador;
	private final EnvioCorreo correo;
	private final LimitadorIntentos limitador;
	private final TransactionTemplate transaccion;
	private final Clock reloj;

	public ServicioRegistro(UsuarioRepositorio usuarios, ServicioTokensUnUso tokens, PoliticaContrasena politica,
			PasswordEncoder codificador, EnvioCorreo correo, LimitadorIntentos limitador,
			TransactionTemplate transaccion, Clock reloj) {
		this.usuarios = usuarios;
		this.tokens = tokens;
		this.politica = politica;
		this.codificador = codificador;
		this.correo = correo;
		this.limitador = limitador;
		this.transaccion = transaccion;
		this.reloj = reloj;
	}

	/**
	 * Crea la cuenta. Si el correo ya existe no hace nada y el resultado es idéntico (también en tiempo,
	 * porque la contraseña se codifica igualmente), para no revelar qué correos están registrados.
	 * La fecha de nacimiento se valida aquí y se descarta: solo se guardan el año y {@code mayor_de_edad}.
	 */
	public void registrar(String correoSinNormalizar, String contrasena, LocalDate nacimiento, String ip) {
		String direccion = ReglasCuenta.normalizarCorreo(correoSinNormalizar);
		limitador.exigir(ACCION, ip, direccion);
		politica.exigir(contrasena);
		Instant ahora = reloj.instant();
		if (!ReglasCuenta.esMayorDeEdad(nacimiento, LocalDate.ofInstant(ahora, ZoneOffset.UTC))) {
			throw ErrorCuenta.datosNoValidos("Forja es solo para mayores de " + ReglasCuenta.EDAD_MINIMA + " años.");
		}
		String hash = codificador.encode(contrasena);
		String tokenVerificacion = transaccion.execute(estado -> crearCuenta(direccion, hash, nacimiento, ahora));
		if (tokenVerificacion != null) {
			enviarSinFallar(direccion, tokenVerificacion);
		}
	}

	private String crearCuenta(String direccion, String hash, LocalDate nacimiento, Instant ahora) {
		try {
			long id = usuarios.insertar(UUID.randomUUID(), direccion, hash, nacimiento.getYear(), true, ahora);
			return tokens.crear(id, TipoTokenUnUso.VERIFICACION, VIDA_TOKEN_VERIFICACION);
		} catch (DuplicateKeyException e) {
			return null;
		}
	}

	private void enviarSinFallar(String direccion, String token) {
		try {
			correo.enviarVerificacion(direccion, token);
		} catch (RuntimeException e) {
			LOG.error("fallo al enviar el correo de verificación ({})", e.getClass().getSimpleName());
		}
	}

	public void verificar(String token) {
		transaccion.executeWithoutResult(estado -> usuarios
				.marcarVerificado(tokens.consumir(token, TipoTokenUnUso.VERIFICACION)));
	}
}
