package es.forja.cuenta.servicio;

import java.time.Duration;

import org.springframework.http.HttpStatus;

/** Fallo de negocio de la cuenta con el estado HTTP que le toca. El mensaje es apto para el usuario. */
public class ErrorCuenta extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** Código estable de los 429, para que el cliente lo reconozca sin leer el mensaje. */
	public static final String CODIGO_DEMASIADAS_PETICIONES = "demasiadas_peticiones";

	private final HttpStatus estado;
	private final transient Duration esperar;
	private final String codigo;

	private ErrorCuenta(HttpStatus estado, String mensaje, Duration esperar) {
		this(estado, mensaje, esperar, null);
	}

	private ErrorCuenta(HttpStatus estado, String mensaje, Duration esperar, String codigo) {
		super(mensaje);
		this.estado = estado;
		this.esperar = esperar;
		this.codigo = codigo;
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
				esperar, CODIGO_DEMASIADAS_PETICIONES);
	}

	/** Código estable para el cliente, o nulo. */
	public String codigo() {
		return codigo;
	}

	public HttpStatus estado() {
		return estado;
	}

	/** Segundos que debe esperar el cliente (cabecera Retry-After), o 0 si no aplica. */
	public long segundosDeEspera() {
		return esperar == null ? 0 : Math.max(1, (esperar.toMillis() + 999) / 1000);
	}
}
