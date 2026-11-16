package es.forja.cuenta.servicio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/** Tokens opacos de 256 bits: se entregan en claro una vez y en la base solo vive su SHA-256. */
final class Tokens {

	private static final int BYTES_DE_AZAR = 32;
	private static final SecureRandom AZAR = new SecureRandom();

	private Tokens() {
	}

	static String nuevo() {
		byte[] bytes = new byte[BYTES_DE_AZAR];
		AZAR.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	static byte[] hash(String token) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 no disponible", e);
		}
	}
}
