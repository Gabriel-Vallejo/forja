package es.forja.cuenta.servicio;

/**
 * Salida de correo de la cuenta. El proveedor transaccional está sin elegir (ADR 0009 §5): quien lo
 * implemente recibe el token en claro y construye el enlace.
 */
public interface EnvioCorreo {

	void enviarVerificacion(String correo, String token);

	void enviarRecuperacion(String correo, String token);
}
