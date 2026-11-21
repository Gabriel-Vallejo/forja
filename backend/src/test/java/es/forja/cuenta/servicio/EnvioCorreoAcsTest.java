package es.forja.cuenta.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** La firma HMAC de ACS y la petición que sale, contra un servidor HTTP local. */
class EnvioCorreoAcsTest {

	private static final byte[] CLAVE = "clave-de-prueba".getBytes(StandardCharsets.UTF_8);
	private static final JsonMapper JSON = JsonMapper.builder().build();

	private record Recibida(String ruta, Headers cabeceras, byte[] cuerpo) {
	}

	@Test
	void laFirmaCasaConLaDeOpenssl() {
		EnvioCorreoAcs envio = new EnvioCorreoAcs(java.net.URI.create("https://h.example"), CLAVE, "r@x", "https://a",
				JSON, Clock.fixed(Instant.parse("2026-11-21T20:00:00Z"), ZoneOffset.UTC));

		// printf '<texto>' | openssl dgst -sha256 -mac HMAC -macopt hexkey:<clave en hex> -binary | base64
		assertThat(envio.firmar("POST\n/emails:send?api-version=2023-03-31\nSat, 21 Nov 2026 20:00:00 GMT;h.example;abc"))
				.isEqualTo("ADzvUEZ07rXEaa7tDyNZgilp7pSR6JZM7uID3Z7Sv9s=");
	}

	@Test
	void mandaLaVerificacionFirmadaConElEnlaceYElTokenEnElFragmento() throws Exception {
		CompletableFuture<Recibida> recibida = new CompletableFuture<>();
		HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		servidor.createContext("/", intercambio -> {
			recibida.complete(new Recibida(intercambio.getRequestURI().toString(), intercambio.getRequestHeaders(),
					intercambio.getRequestBody().readAllBytes()));
			intercambio.sendResponseHeaders(202, -1);
			intercambio.close();
		});
		servidor.start();
		String host = "127.0.0.1:" + servidor.getAddress().getPort();
		EnvioCorreoAcs envio = EnvioCorreoAcs.desde(new PropiedadesCorreo(
				"endpoint=http://" + host + "/;accesskey=" + Base64.getEncoder().encodeToString(CLAVE),
				"DoNotReply@forja.test", "https://api.forja.test/"), JSON);
		try {
			envio.enviarVerificacion("ana@forja.test", "tok-123");
			Recibida peticion = recibida.get(5, TimeUnit.SECONDS);

			assertThat(peticion.ruta()).isEqualTo(EnvioCorreoAcs.RUTA);
			String hash = Base64.getEncoder()
					.encodeToString(MessageDigest.getInstance("SHA-256").digest(peticion.cuerpo()));
			assertThat(peticion.cabeceras().getFirst("x-ms-content-sha256")).isEqualTo(hash);
			String fecha = peticion.cabeceras().getFirst("x-ms-date");
			assertThat(peticion.cabeceras().getFirst("Authorization"))
					.isEqualTo("HMAC-SHA256 SignedHeaders=x-ms-date;host;x-ms-content-sha256&Signature="
							+ envio.firmar("POST\n" + EnvioCorreoAcs.RUTA + "\n" + fecha + ";" + host + ";" + hash));
			JsonNode cuerpo = JSON.readTree(peticion.cuerpo());
			assertThat(cuerpo.get("senderAddress").asString()).isEqualTo("DoNotReply@forja.test");
			assertThat(cuerpo.at("/recipients/to/0/address").asString()).isEqualTo("ana@forja.test");
			assertThat(cuerpo.at("/content/plainText").asString())
					.contains("https://api.forja.test/cuenta/verificar.html#tok-123");
		} finally {
			envio.destroy();
			servidor.stop(0);
		}
	}

	@Test
	void unaCadenaDeConexionSinClaveNoArranca() {
		assertThatThrownBy(() -> EnvioCorreoAcs.desde(
				new PropiedadesCorreo("endpoint=https://x.communication.azure.com/", "r@x", "https://a"), JSON))
				.isInstanceOf(IllegalStateException.class);
	}
}
