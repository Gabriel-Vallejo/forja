package es.forja.cuenta.servicio;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import tools.jackson.databind.json.JsonMapper;

/**
 * Envía los correos de la cuenta por la API REST de Azure Communication Services, firmada con HMAC-SHA256
 * (https://learn.microsoft.com/azure/communication-services/tutorials/hmac-header-tutorial).
 *
 * <p>El envío va en segundo plano: si fuera en la petición, recuperar la contraseña tardaría más cuando la cuenta
 * existe y eso la delataría (ADR 0009 §5). En el log no quedan ni el correo ni el token.
 */
public final class EnvioCorreoAcs implements EnvioCorreo, DisposableBean {

	static final String RUTA = "/emails:send?api-version=2023-03-31";
	private static final Logger LOG = LoggerFactory.getLogger(EnvioCorreoAcs.class);
	private static final DateTimeFormatter FECHA_HTTP = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC);
	private static final Duration TIEMPO_MAXIMO = Duration.ofSeconds(10);
	private static final int ACEPTADO = 202;

	private final URI endpoint;
	private final byte[] clave;
	private final String remitente;
	private final String urlEnlaces;
	private final JsonMapper json;
	private final Clock reloj;
	private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIEMPO_MAXIMO).build();
	private final ExecutorService envios = Executors.newVirtualThreadPerTaskExecutor();

	EnvioCorreoAcs(URI endpoint, byte[] clave, String remitente, String urlEnlaces, JsonMapper json, Clock reloj) {
		this.endpoint = endpoint;
		this.clave = clave.clone();
		this.remitente = remitente;
		this.urlEnlaces = urlEnlaces.replaceAll("/+$", "");
		this.json = json;
		this.reloj = reloj;
	}

	static EnvioCorreoAcs desde(PropiedadesCorreo p, JsonMapper json) {
		String endpoint = null;
		String clave = null;
		for (String parte : p.cadenaConexion().split(";")) {
			int igual = parte.indexOf('=');
			if (igual < 0) {
				continue;
			}
			String nombre = parte.substring(0, igual).strip().toLowerCase(java.util.Locale.ROOT);
			String valor = parte.substring(igual + 1).strip();
			if (nombre.equals("endpoint")) {
				endpoint = valor.replaceAll("/+$", "");
			} else if (nombre.equals("accesskey")) {
				clave = valor;
			}
		}
		if (endpoint == null || clave == null || p.remitente() == null || p.urlEnlaces() == null) {
			throw new IllegalStateException("forja.correo: faltan endpoint, accesskey, remitente o url-enlaces");
		}
		return new EnvioCorreoAcs(URI.create(endpoint), Base64.getDecoder().decode(clave), p.remitente(),
				p.urlEnlaces(), json, Clock.systemUTC());
	}

	@Override
	public void enviarVerificacion(String correo, String token) {
		String enlace = urlEnlaces + "/cuenta/verificar.html#" + token;
		enviarEnSegundoPlano(correo, "Verifica tu cuenta de Forja", """
				Abre este enlace para verificar tu cuenta de Forja. Caduca en 24 horas:

				%s

				Si no te has registrado en Forja, ignora este correo.""".formatted(enlace));
	}

	@Override
	public void enviarRecuperacion(String correo, String token) {
		String enlace = urlEnlaces + "/cuenta/restablecer.html#" + token;
		enviarEnSegundoPlano(correo, "Cambia tu contraseña de Forja", """
				Abre este enlace para elegir una contraseña nueva. Caduca en 30 minutos y al cambiarla se cierra la \
				sesión en todos tus dispositivos:

				%s

				Si no lo has pedido tú, ignora este correo: tu contraseña sigue igual.""".formatted(enlace));
	}

	private void enviarEnSegundoPlano(String correo, String asunto, String texto) {
		envios.execute(() -> {
			try {
				int estado = http.send(peticion(correo, asunto, texto), HttpResponse.BodyHandlers.discarding())
						.statusCode();
				if (estado != ACEPTADO) {
					LOG.error("ACS rechazó el correo «{}»: HTTP {}", asunto, estado);
				}
			} catch (java.io.IOException | RuntimeException e) {
				LOG.error("fallo al enviar el correo «{}» ({})", asunto, e.getClass().getSimpleName());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
	}

	HttpRequest peticion(String correo, String asunto, String texto) {
		byte[] cuerpo = json.writeValueAsBytes(Map.of("senderAddress", remitente,
				"recipients", Map.of("to", List.of(Map.of("address", correo))),
				"content", Map.of("subject", asunto, "plainText", texto)));
		String fecha = FECHA_HTTP.format(reloj.instant());
		String hash = Base64.getEncoder().encodeToString(sha256(cuerpo));
		String host = endpoint.getAuthority();
		String firma = firmar("POST\n" + RUTA + "\n" + fecha + ";" + host + ";" + hash);
		return HttpRequest.newBuilder(URI.create(endpoint + RUTA)).timeout(TIEMPO_MAXIMO)
				.header("Content-Type", "application/json").header("x-ms-date", fecha)
				.header("x-ms-content-sha256", hash)
				.header("Authorization", "HMAC-SHA256 SignedHeaders=x-ms-date;host;x-ms-content-sha256&Signature=" + firma)
				.POST(HttpRequest.BodyPublishers.ofByteArray(cuerpo)).build();
	}

	String firmar(String texto) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(clave, "HmacSHA256"));
			return Base64.getEncoder().encodeToString(mac.doFinal(texto.getBytes(StandardCharsets.UTF_8)));
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException(e);
		}
	}

	private static byte[] sha256(byte[] datos) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(datos);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void destroy() {
		envios.close();
	}
}
