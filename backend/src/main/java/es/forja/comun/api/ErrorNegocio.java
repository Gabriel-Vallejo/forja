package es.forja.comun.api;

import org.springframework.http.HttpStatus;

/** Fallo de negocio con el estado HTTP que le toca. El mensaje es apto para el usuario y nunca lleva datos de salud. */
public class ErrorNegocio extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final HttpStatus estado;
	private final String codigo;

	private ErrorNegocio(HttpStatus estado, String mensaje) {
		this(estado, mensaje, null);
	}

	private ErrorNegocio(HttpStatus estado, String mensaje, String codigo) {
		super(mensaje);
		this.estado = estado;
		this.codigo = codigo;
	}

	public static ErrorNegocio datosNoValidos(String mensaje) {
		return new ErrorNegocio(HttpStatus.BAD_REQUEST, mensaje);
	}

	public static ErrorNegocio prohibido(String mensaje) {
		return new ErrorNegocio(HttpStatus.FORBIDDEN, mensaje);
	}

	public static ErrorNegocio noEncontrado(String mensaje) {
		return new ErrorNegocio(HttpStatus.NOT_FOUND, mensaje);
	}

	public static ErrorNegocio conflicto(String mensaje) {
		return new ErrorNegocio(HttpStatus.CONFLICT, mensaje);
	}

	/** 409 con un código estable que el cliente puede reconocer sin leer el mensaje. */
	public static ErrorNegocio conflicto(String mensaje, String codigo) {
		return new ErrorNegocio(HttpStatus.CONFLICT, mensaje, codigo);
	}

	/** 422: la petición está bien formada, pero apunta a algo que no se puede usar (una serie o un plan ajenos). */
	public static ErrorNegocio noProcesable(String mensaje) {
		return new ErrorNegocio(HttpStatus.UNPROCESSABLE_CONTENT, mensaje);
	}

	public static ErrorNegocio demasiadoGrande(String mensaje) {
		return new ErrorNegocio(HttpStatus.CONTENT_TOO_LARGE, mensaje);
	}

	public static ErrorNegocio noAutenticado(String mensaje) {
		return new ErrorNegocio(HttpStatus.UNAUTHORIZED, mensaje);
	}

	/** Código estable para el cliente, o nulo. */
	public String codigo() {
		return codigo;
	}

	public HttpStatus estado() {
		return estado;
	}
}
