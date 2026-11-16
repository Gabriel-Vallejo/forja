package es.forja.cuenta.servicio;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Service;

import es.forja.cuenta.modelo.TipoTokenUnUso;
import es.forja.cuenta.modelo.TokenUnUso;
import es.forja.cuenta.repositorio.TokenUnUsoRepositorio;

/** Crea y gasta los enlaces de un solo uso de verificación y de recuperación. */
@Service
public class ServicioTokensUnUso {

	private static final String ENLACE_NO_VALIDO = "El enlace no es válido o ha caducado.";

	private final TokenUnUsoRepositorio tokens;
	private final Clock reloj;

	public ServicioTokensUnUso(TokenUnUsoRepositorio tokens, Clock reloj) {
		this.tokens = tokens;
		this.reloj = reloj;
	}

	/**
	 * Guarda el hash y devuelve el token en claro, que solo existe en el correo. Los anteriores del mismo
	 * tipo sin usar se gastan: solo vale el último.
	 */
	public String crear(long usuarioId, TipoTokenUnUso tipo, Duration vida) {
		Instant ahora = reloj.instant();
		tokens.gastarPendientes(usuarioId, tipo, ahora);
		String token = Tokens.nuevo();
		tokens.insertar(usuarioId, tipo, Tokens.hash(token), ahora.plus(vida));
		return token;
	}

	/** Gasta los tokens pendientes de ese tipo (p. ej. al restablecer, los demás enlaces de recuperación). */
	public void gastarPendientes(long usuarioId, TipoTokenUnUso tipo) {
		tokens.gastarPendientes(usuarioId, tipo, reloj.instant());
	}

	/** Gasta el token y devuelve el usuario al que pertenece; 400 si no existe, es de otro tipo, caducó o ya se usó. */
	public long consumir(String token, TipoTokenUnUso tipo) {
		Instant ahora = reloj.instant();
		TokenUnUso guardado = tokens.porHash(Tokens.hash(token)).orElseThrow(
				() -> ErrorCuenta.datosNoValidos(ENLACE_NO_VALIDO));
		boolean vigente = guardado.tipo() == tipo && guardado.usadoEn() == null && ahora.isBefore(guardado.caducaEn());
		if (!vigente || !tokens.marcarUsado(guardado.id(), ahora)) {
			throw ErrorCuenta.datosNoValidos(ENLACE_NO_VALIDO);
		}
		return guardado.usuarioId();
	}
}
