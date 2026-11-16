package es.forja;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Signature;
import java.util.Base64;
import java.util.UUID;

/** Fabrica a mano JWT de otros (o rotos) con la API de firma del JDK, para comprobar que se rechazan. */
public final class JwtDePrueba {

	private JwtDePrueba() {
	}

	public static String base64Url(String texto) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
	}

	public static String cabecera(String alg, String kid) {
		return "{\"alg\":\"" + alg + "\",\"typ\":\"JWT\",\"kid\":\"" + kid + "\"}";
	}

	public static String carga(UUID usuario, UUID dispositivo, long iat, long exp) {
		return "{\"sub\":\"" + usuario + "\",\"sid\":\"" + dispositivo + "\",\"iat\":" + iat + ",\"exp\":" + exp + "}";
	}

	public static String forjar(String cabecera, String carga, KeyPair firmante) {
		try {
			String firmado = base64Url(cabecera) + "." + base64Url(carga);
			Signature firma = Signature.getInstance("Ed25519");
			firma.initSign(firmante.getPrivate());
			firma.update(firmado.getBytes(StandardCharsets.US_ASCII));
			return firmado + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(firma.sign());
		} catch (java.security.GeneralSecurityException e) {
			throw new IllegalStateException(e);
		}
	}
}
