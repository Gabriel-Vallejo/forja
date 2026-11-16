package es.forja.cuenta.servicio;

import java.time.Duration;

import org.springframework.http.HttpStatus;

/** Fallo de negocio de la cuenta con el estado HTTP que le toca. El mensaje es apto para el usuario. */
public class ErrorCuenta extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final HttpStatus estado;
	private final transient Duration esperar;

	private ErrorCuenta(HttpStatus estado, String mensaje, Duration esperar) {
		super(mensaje);
		this.estado = estado;
		this.esperar = esperar;
	}

	public static ErrorCuenta datosNoValidos(String mensaje) {
		return new ErrorCuenta(HttpStatus.BAD_REQUEST, mensaje, null);
	}

	public static ErrorCuenta noAutenticado(String mensaje) {
		return new ErrorCuenta(HttpStatus.UNAUTHORIZED, mensaje, null);
	}

	public static ErrorCuenta prohibido(String mensaje) {
		return new ErrorCuenta(HttpStatus.FORBIDDEN, mensaje, null);
	}

	public static ErrorCuenta noEncontrado(String mensaje) {
		return new ErrorCuenta(HttpStatus.NOT_FOUND, mensaje, null);
	}

	public static ErrorCuenta demasiadasPeticiones(Duration esperar) {
		return new ErrorCuenta(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos. Espera un poco y vuelve a probar.",
				esperar);
	}

	public HttpStatus estado() {
		return estado;
	}

	/** Segundos que debe esperar el cliente (cabecera Retry-After), o 0 si no aplica. */
	public long segundosDeEspera() {
		return esperar == null ? 0 : Math.max(1, (esperar.toMillis() + 999) / 1000);
	}
}
