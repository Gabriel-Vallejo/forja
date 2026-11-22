package es.forja.cuenta.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import es.forja.cuenta.api.Solicitudes;
import es.forja.cuenta.modelo.Plataforma;
import es.forja.cuenta.modelo.Usuario;

/** Ni el correo, ni las contraseñas, ni los tokens ni los hashes salen en logs ni en {@code toString()}. */
class PrivacidadTest {

	private static final String CORREO = "secreto@forja.test";
	private static final String TOKEN = "token-secreto-123";
	private static final String CONTRASENA = "contrasena-secreta-123";

	@Test
	void elEnvioSinProveedorSoloDejaUnAvisoSinCorreoNiToken() {
		Logger logger = (Logger) LoggerFactory.getLogger(EnvioCorreoSinProveedor.class);
		ListAppender<ILoggingEvent> mensajes = new ListAppender<>();
		mensajes.start();
		logger.addAppender(mensajes);
		try {
			var envio = new EnvioCorreoSinProveedor();
			envio.enviarVerificacion(CORREO, TOKEN);
			envio.enviarRecuperacion(CORREO, TOKEN);
		} finally {
			logger.detachAppender(mensajes);
		}

		assertThat(mensajes.list).extracting(ILoggingEvent::getFormattedMessage).containsExactly(
				"correo de verificación no enviado: proveedor sin configurar",
				"correo de recuperación no enviado: proveedor sin configurar");
	}

	@Test
	void ningunDtoNiEntidadImprimeSusSecretos() {
		String todo = String.join(" ",
				new Usuario(1, UUID.randomUUID(), CORREO, "$argon2id$hash", false, 1998, true).toString(),
				new Sesion(TOKEN, TOKEN, Instant.now(), UUID.randomUUID()).toString(),
				new Solicitudes.Registro(CORREO, CONTRASENA, LocalDate.of(1998, 4, 12)).toString(),
				new Solicitudes.Login(CORREO, CONTRASENA, null, "P", Plataforma.MOVIL).toString(),
				new Solicitudes.Refresco(TOKEN).toString(), new Solicitudes.Token(TOKEN).toString(),
				new Solicitudes.Recuperar(CORREO).toString(),
				new Solicitudes.Restablecer(TOKEN, CONTRASENA).toString(),
				new Solicitudes.BorrarCuenta(CONTRASENA).toString());

		assertThat(todo).doesNotContain(CORREO, TOKEN, CONTRASENA, "argon2id", "1998");
	}
}
