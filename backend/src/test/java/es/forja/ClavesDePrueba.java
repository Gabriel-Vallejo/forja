package es.forja;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

/**
 * Claves Ed25519 generadas al vuelo en cada ejecución de los tests: nunca hay claves fijas en el repo.
 * También es el inicializador que las pone en el entorno de la aplicación bajo prueba.
 */
public final class ClavesDePrueba implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	public static final String KID_ACTUAL = "kid-actual-prueba";
	public static final String KID_ANTERIOR = "kid-anterior-prueba";
	public static final KeyPair ACTUAL = generar();
	public static final KeyPair ANTERIOR = generar();
	/** La única contraseña de la lista de filtradas que se usa en los tests. */
	public static final String CONTRASENA_FILTRADA = "contrasena-filtrada-de-prueba";

	private static KeyPair generar() {
		try {
			return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	public static String privadaBase64(KeyPair par) {
		return Base64.getEncoder().encodeToString(par.getPrivate().getEncoded());
	}

	public static String publicaBase64(KeyPair par) {
		return Base64.getEncoder().encodeToString(par.getPublic().getEncoded());
	}

	private static String archivoDeFiltradas() {
		try {
			Path archivo = Files.createTempFile("filtradas-prueba", ".txt");
			archivo.toFile().deleteOnExit();
			return Files.writeString(archivo, CONTRASENA_FILTRADA + "\n").toString();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void initialize(ConfigurableApplicationContext contexto) {
		contexto.getEnvironment().getPropertySources().addFirst(new MapPropertySource("clavesDePrueba", Map.of(
				"forja.jwt.kid-actual", KID_ACTUAL,
				"forja.jwt.clave-privada-actual", privadaBase64(ACTUAL),
				"forja.jwt.kid-anterior", KID_ANTERIOR,
				"forja.jwt.clave-publica-anterior", publicaBase64(ANTERIOR),
				"forja.contrasenas-filtradas", archivoDeFiltradas(),
				"forja.origen-web", "https://forja.test")));
	}
}
