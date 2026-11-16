package es.forja.comun.cifrado;

/** El valor no se pudo descifrar. El mensaje nunca lleva el valor, la clave ni la fila. */
public class ErrorDeCifrado extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ErrorDeCifrado() {
		super("No se pudo descifrar el valor.");
	}
}
