package es.forja.cuenta.servicio;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import es.forja.comun.seguridad.ServicioJwt;
import es.forja.cuenta.modelo.Dispositivo;
import es.forja.cuenta.modelo.Plataforma;
import es.forja.cuenta.modelo.ReglasCuenta;
import es.forja.cuenta.modelo.TokenRefresco;
import es.forja.cuenta.modelo.Usuario;
import es.forja.cuenta.repositorio.DispositivoRepositorio;
import es.forja.cuenta.repositorio.TokenRefrescoRepositorio;
import es.forja.cuenta.repositorio.UsuarioRepositorio;

/** Login, rotación del refresco y cierre de sesión por dispositivo (ADR 0009 §2-4). */
@Service
public class ServicioSesion {

	public static final Duration VIDA_TOKEN_REFRESCO = Duration.ofDays(30);
	private static final String ACCION = "login";
	private static final String CREDENCIALES_MALAS = "Correo o contraseña incorrectos.";
	private static final String SESION_NO_VALIDA = "La sesión ya no es válida. Inicia sesión otra vez.";

	private final UsuarioRepositorio usuarios;
	private final DispositivoRepositorio dispositivos;
	private final TokenRefrescoRepositorio refrescos;
	private final ServicioJwt jwt;
	private final PasswordEncoder codificador;
	private final LimitadorIntentos limitador;
	private final TransactionTemplate transaccion;
	private final Clock reloj;
	private final boolean exigirVerificacion;
	/** Hash de una contraseña que nadie tiene: se compara cuando el correo no existe, para gastar el mismo tiempo. */
	private final String hashSenuelo;

	public ServicioSesion(UsuarioRepositorio usuarios, DispositivoRepositorio dispositivos,
			TokenRefrescoRepositorio refrescos, ServicioJwt jwt, PasswordEncoder codificador,
			LimitadorIntentos limitador, TransactionTemplate transaccion, Clock reloj,
			@Value("${forja.cuenta.exigir-verificacion:true}") String exigirVerificacion) {
		this.usuarios = usuarios;
		this.dispositivos = dispositivos;
		this.refrescos = refrescos;
		this.jwt = jwt;
		this.codificador = codificador;
		this.limitador = limitador;
		this.transaccion = transaccion;
		this.reloj = reloj;
		// A prueba de fallos: solo un «false» explícito desactiva la exigencia; vacío o con erratas, se exige.
		this.exigirVerificacion = !"false".equalsIgnoreCase(exigirVerificacion.strip());
		this.hashSenuelo = codificador.encode(Tokens.nuevo());
	}

	/** Datos del dispositivo que entra; {@code dispositivoId} es el {@code sid} si el cliente ya tenía uno. */
	public record DatosDispositivo(UUID dispositivoId, String nombre, Plataforma plataforma) {
	}

	public Sesion entrar(String correoSinNormalizar, String contrasena, DatosDispositivo datos, String ip) {
		String direccion = ReglasCuenta.normalizarCorreo(correoSinNormalizar);
		limitador.exigir(ACCION, ip, direccion);
		Usuario usuario = usuarios.porCorreo(direccion).orElse(null);
		Instant ahora = reloj.instant();
		// Sin cuenta o con la cuenta en espera se compara contra el hash señuelo: mismo tiempo y mismo 401,
		// para que ni la respuesta ni la duración delaten qué correos existen o están bloqueados.
		boolean comparable = usuario != null && !enEspera(usuario, ahora);
		boolean coincide = codificador.matches(contrasena, comparable ? usuario.hashContrasena() : hashSenuelo);
		if (!comparable) {
			throw ErrorCuenta.noAutenticado(CREDENCIALES_MALAS);
		}
		if (!coincide) {
			registrarFallo(usuario, ahora);
			throw ErrorCuenta.noAutenticado(CREDENCIALES_MALAS);
		}
		if (exigirVerificacion && !usuario.verificado()) {
			throw ErrorCuenta.noAutenticado(CREDENCIALES_MALAS);
		}
		if (usuario.fallosSeguidos() > 0 || usuario.bloqueadoHasta() != null) {
			usuarios.guardarFallos(usuario.id(), 0, null);
		}
		return transaccion.execute(estado -> abrirSesion(usuario, datos, ahora));
	}

	private static boolean enEspera(Usuario usuario, Instant ahora) {
		return usuario.bloqueadoHasta() != null && ahora.isBefore(usuario.bloqueadoHasta());
	}

	/** Suma el fallo de forma atómica y, si toca, fija la espera según el contador resultante. */
	private void registrarFallo(Usuario usuario, Instant ahora) {
		transaccion.executeWithoutResult(estado -> {
			int fallos = usuarios.sumarFallo(usuario.id());
			Duration espera = ReglasCuenta.esperaTrasFallos(fallos);
			if (!espera.isZero()) {
				usuarios.bloquearHasta(usuario.id(), ahora.plus(espera));
			}
		});
	}

	private Sesion abrirSesion(Usuario usuario, DatosDispositivo datos, Instant ahora) {
		Optional<Dispositivo> existente = datos.dispositivoId() == null ? Optional.empty()
				: dispositivos.vivoDeUsuario(datos.dispositivoId(), usuario.id());
		long dispositivoId;
		UUID sid;
		if (existente.isPresent()) {
			dispositivoId = existente.get().id();
			sid = existente.get().uuid();
			dispositivos.actualizarActividad(dispositivoId, datos.nombre(), datos.plataforma(), ahora);
			refrescos.revocarDeDispositivo(dispositivoId, ahora);
		} else {
			sid = UUID.randomUUID();
			dispositivoId = dispositivos.insertar(sid, usuario.id(), datos.nombre(), datos.plataforma(), ahora);
		}
		return emitir(usuario.uuid(), sid, dispositivoId, UUID.randomUUID(), ahora);
	}

	private Sesion emitir(UUID usuario, UUID sid, long dispositivoId, UUID familia, Instant ahora) {
		String refresco = Tokens.nuevo();
		refrescos.insertar(dispositivoId, familia, Tokens.hash(refresco), ahora.plus(VIDA_TOKEN_REFRESCO));
		return new Sesion(jwt.emitir(usuario, sid), refresco, jwt.caducidadDeUnTokenNuevo(), sid);
	}

	/**
	 * Cambia un refresco por otro de la misma familia. Si llega uno ya usado o revocado se da por robado:
	 * se revoca la familia y el dispositivo y se responde 401. Esa revocación se confirma antes de
	 * lanzar el error, por eso el error sale fuera de la transacción.
	 */
	public Sesion refrescar(String tokenRefresco) {
		Instant ahora = reloj.instant();
		Sesion nueva = transaccion.execute(estado -> rotar(tokenRefresco, ahora));
		if (nueva == null) {
			if (refrescos.esDeCuentaBorrada(Tokens.hash(tokenRefresco), ahora)) {
				throw ErrorCuenta.cuentaBorrada();
			}
			throw ErrorCuenta.noAutenticado(SESION_NO_VALIDA);
		}
		return nueva;
	}

	/** Devuelve la sesión nueva, o null si el refresco no sirve (con la revocación ya hecha si hacía falta). */
	private Sesion rotar(String tokenRefresco, Instant ahora) {
		Optional<TokenRefresco> guardado = refrescos.porHash(Tokens.hash(tokenRefresco));
		if (guardado.isEmpty()) {
			return null;
		}
		TokenRefresco actual = guardado.get();
		if (actual.gastado() || !refrescos.marcarUsado(actual.id(), ahora)) {
			refrescos.revocarFamilia(actual.familia(), ahora);
			dispositivos.revocar(actual.dispositivoId(), ahora);
			return null;
		}
		Dispositivo dispositivo = dispositivos.porId(actual.dispositivoId()).orElseThrow();
		if (!ahora.isBefore(actual.caducaEn()) || dispositivo.revocadoEn() != null) {
			return null;
		}
		Usuario usuario = usuarios.porId(dispositivo.usuarioId()).orElseThrow();
		dispositivos.tocar(dispositivo.id(), ahora);
		return emitir(usuario.uuid(), dispositivo.uuid(), dispositivo.id(), actual.familia(), ahora);
	}

	/** Cierra el dispositivo del token de acceso: su refresco deja de valer al momento. */
	public void cerrar(UUID usuarioUuid, UUID dispositivoUuid) {
		Usuario usuario = usuarios.porUuid(usuarioUuid).orElseThrow(() -> ErrorCuenta.noAutenticado(SESION_NO_VALIDA));
		dispositivos.vivoDeUsuario(dispositivoUuid, usuario.id()).ifPresent(d -> transaccion
				.executeWithoutResult(estado -> revocarDispositivo(d, reloj.instant())));
	}

	void revocarDispositivo(Dispositivo dispositivo, Instant ahora) {
		refrescos.revocarDeDispositivo(dispositivo.id(), ahora);
		dispositivos.revocar(dispositivo.id(), ahora);
	}
}
