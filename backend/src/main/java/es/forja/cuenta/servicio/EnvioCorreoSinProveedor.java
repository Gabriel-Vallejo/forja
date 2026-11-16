package es.forja.cuenta.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Implementación por defecto: no envía nada y no deja en el log ni el correo ni el token. */
@Component
public class EnvioCorreoSinProveedor implements EnvioCorreo {

	private static final Logger LOG = LoggerFactory.getLogger(EnvioCorreoSinProveedor.class);

	@Override
	public void enviarVerificacion(String correo, String token) {
		LOG.warn("correo de verificación no enviado: proveedor sin configurar");
	}

	@Override
	public void enviarRecuperacion(String correo, String token) {
		LOG.warn("correo de recuperación no enviado: proveedor sin configurar");
	}
}
