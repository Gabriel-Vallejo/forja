package es.forja.comun.cifrado;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cifrado por columna con AES-256-GCM (ADR 0004, enmienda 8).
 *
 * <p>Formato del valor: {@code versión (1 byte) | longitud del kid (1 byte) | kid | nonce (12 bytes) |
 * cifrado + tag de 128 bits}. La tabla, la columna y la fila van como datos asociados: un valor copiado
 * a otro sitio no se descifra. Se cifra siempre con la clave actual y se descifra con la que diga el kid.
 */
public final class Cifrador {

	public static final int BYTES_CLAVE = 32;

	private static final byte VERSION = 1;
	private static final int BYTES_NONCE = 12;
	private static final int BITS_TAG = 128;
	private static final int BYTES_TAG = BITS_TAG / 8;
	private static final int MAXIMO_KID = 255;
	private static final String ALGORITMO = "AES/GCM/NoPadding";
	private static final SecureRandom AZAR = new SecureRandom();

	private final String kidActual;
	private final Map<String, SecretKeySpec> claves = new HashMap<>();

	/** Falla (la aplicación no arranca) si una clave no mide 32 bytes o un kid está vacío o repetido. */
	public Cifrador(String kidActual, byte[] claveActual, Map<String, byte[]> anteriores) {
		this.kidActual = kidActual;
		registrar(kidActual, claveActual);
		anteriores.forEach(this::registrar);
	}

	private void registrar(String kid, byte[] clave) {
		if (kid == null || kid.isBlank() || kid.getBytes(StandardCharsets.UTF_8).length > MAXIMO_KID) {
			throw new IllegalArgumentException("El identificador de clave de cifrado no es válido.");
		}
		if (clave == null || clave.length != BYTES_CLAVE) {
			throw new IllegalArgumentException("Cada clave de cifrado debe medir " + BYTES_CLAVE + " bytes.");
		}
		if (claves.put(kid, new SecretKeySpec(clave, "AES")) != null) {
			throw new IllegalArgumentException("Hay un identificador de clave de cifrado repetido.");
		}
	}

	public byte[] cifrar(String tabla, String columna, UUID fila, byte[] claro) {
		byte[] kid = kidActual.getBytes(StandardCharsets.UTF_8);
		byte[] nonce = new byte[BYTES_NONCE];
		AZAR.nextBytes(nonce);
		try {
			Cipher cipher = Cipher.getInstance(ALGORITMO);
			cipher.init(Cipher.ENCRYPT_MODE, claves.get(kidActual), new GCMParameterSpec(BITS_TAG, nonce));
			cipher.updateAAD(asociados(tabla, columna, fila));
			byte[] cifrado = cipher.doFinal(claro);
			return ByteBuffer.allocate(2 + kid.length + BYTES_NONCE + cifrado.length).put(VERSION)
					.put((byte) kid.length).put(kid).put(nonce).put(cifrado).array();
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("No se pudo cifrar el valor.");
		}
	}

	public byte[] descifrar(String tabla, String columna, UUID fila, byte[] valor) {
		try {
			ByteBuffer entrada = ByteBuffer.wrap(valor);
			if (entrada.get() != VERSION) {
				throw new ErrorDeCifrado();
			}
			byte[] kid = new byte[Byte.toUnsignedInt(entrada.get())];
			entrada.get(kid);
			SecretKeySpec clave = claves.get(new String(kid, StandardCharsets.UTF_8));
			if (clave == null || entrada.remaining() < BYTES_NONCE + BYTES_TAG) {
				throw new ErrorDeCifrado();
			}
			byte[] nonce = new byte[BYTES_NONCE];
			entrada.get(nonce);
			byte[] cifrado = new byte[entrada.remaining()];
			entrada.get(cifrado);
			Cipher cipher = Cipher.getInstance(ALGORITMO);
			cipher.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(BITS_TAG, nonce));
			cipher.updateAAD(asociados(tabla, columna, fila));
			return cipher.doFinal(cifrado);
		} catch (GeneralSecurityException | java.nio.BufferUnderflowException e) {
			throw new ErrorDeCifrado();
		}
	}

	/** Sin ambigüedad entre campos: el NUL no puede aparecer en un nombre de tabla o columna. */
	private static byte[] asociados(String tabla, String columna, UUID fila) {
		return (tabla + '\0' + columna + '\0' + fila).getBytes(StandardCharsets.UTF_8);
	}
}
