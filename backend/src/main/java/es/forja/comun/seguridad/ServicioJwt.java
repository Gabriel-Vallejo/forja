package es.forja.comun.seguridad;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Emite y valida el JWT de acceso, firmado con Ed25519 (ADR 0009 §2).
 *
 * <p>Solo acepta {@code alg: EdDSA}, un {@code kid} conocido (la clave actual o la anterior) y una firma
 * y una caducidad correctas. Que el dispositivo ({@code sid}) siga vivo lo comprueba el filtro, que sí
 * tiene acceso a la base. El {@code kid} va en la cabecera, que es donde lo define el estándar.
 */
public class ServicioJwt {

	public static final Duration VIDA_TOKEN_ACCESO = Duration.ofMinutes(15);
	private static final String ALGORITMO = "EdDSA";
	private static final int PARTES = 3;
	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final Clock reloj;
	private final String kidActual;
	private final Ed25519PrivateKeyParameters privadaActual;
	private final Map<String, Ed25519PublicKeyParameters> publicasPorKid;

	public ServicioJwt(Clock reloj, String kidActual, Ed25519PrivateKeyParameters privadaActual,
			String kidAnterior, Ed25519PublicKeyParameters publicaAnterior) {
		this.reloj = reloj;
		this.kidActual = kidActual;
		this.privadaActual = privadaActual;
		Ed25519PublicKeyParameters publicaActual = privadaActual.generatePublicKey();
		this.publicasPorKid = kidAnterior == null || publicaAnterior == null || kidAnterior.equals(kidActual)
				? Map.of(kidActual, publicaActual)
				: Map.of(kidActual, publicaActual, kidAnterior, publicaAnterior);
	}

	/** Construye el servicio desde las variables de entorno ya vinculadas (claves en base64). */
	public static ServicioJwt desdePropiedades(Clock reloj, PropiedadesJwt p) {
		Ed25519PrivateKeyParameters privada = (Ed25519PrivateKeyParameters) decodificarPrivada(
				p.clavePrivadaActual());
		boolean hayAnterior = hayTexto(p.kidAnterior()) && hayTexto(p.clavePublicaAnterior());
		Ed25519PublicKeyParameters anterior = hayAnterior ? decodificarPublica(p.clavePublicaAnterior()) : null;
		return new ServicioJwt(reloj, p.kidActual(), privada, hayAnterior ? p.kidAnterior() : null, anterior);
	}

	private static boolean hayTexto(String texto) {
		return texto != null && !texto.isBlank();
	}

	private static AsymmetricKeyParameter decodificarPrivada(String base64) {
		try {
			AsymmetricKeyParameter clave = PrivateKeyFactory.createKey(Base64.getDecoder().decode(base64.strip()));
			if (clave instanceof Ed25519PrivateKeyParameters) {
				return clave;
			}
		} catch (Exception e) {
			throw new IllegalStateException("FORJA_JWT_CLAVE_PRIVADA_ACTUAL no es una clave Ed25519 PKCS#8 en base64");
		}
		throw new IllegalStateException("FORJA_JWT_CLAVE_PRIVADA_ACTUAL no es una clave Ed25519");
	}

	private static Ed25519PublicKeyParameters decodificarPublica(String base64) {
		try {
			AsymmetricKeyParameter clave = PublicKeyFactory.createKey(Base64.getDecoder().decode(base64.strip()));
			if (clave instanceof Ed25519PublicKeyParameters publica) {
				return publica;
			}
		} catch (Exception e) {
			throw new IllegalStateException("FORJA_JWT_CLAVE_PUBLICA_ANTERIOR no es una clave Ed25519 X.509 en base64");
		}
		throw new IllegalStateException("FORJA_JWT_CLAVE_PUBLICA_ANTERIOR no es una clave Ed25519");
	}

	public Instant caducidadDeUnTokenNuevo() {
		return reloj.instant().plus(VIDA_TOKEN_ACCESO);
	}

	public String emitir(UUID usuario, UUID dispositivo) {
		Instant ahora = reloj.instant();
		String cabecera = codificar("{\"alg\":\"" + ALGORITMO + "\",\"typ\":\"JWT\",\"kid\":\""
				+ escapar(kidActual) + "\"}");
		String carga = codificar("{\"sub\":\"" + usuario + "\",\"sid\":\"" + dispositivo + "\",\"iat\":"
				+ ahora.getEpochSecond() + ",\"exp\":" + ahora.plus(VIDA_TOKEN_ACCESO).getEpochSecond() + "}");
		String firmado = cabecera + "." + carga;
		return firmado + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(firmar(firmado));
	}

	/** Devuelve los datos del token si es válido; vacío ante cualquier fallo, sin decir cuál. */
	public Optional<ClaimsAcceso> validar(String token) {
		try {
			return validarOLanzar(token);
		} catch (RuntimeException e) {
			return Optional.empty();
		}
	}

	private Optional<ClaimsAcceso> validarOLanzar(String token) {
		String[] partes = token.split("\\.", -1);
		if (partes.length != PARTES) {
			return Optional.empty();
		}
		JsonNode cabecera = JSON.readTree(decodificar(partes[0]));
		Ed25519PublicKeyParameters publica = publicasPorKid.get(textoDe(cabecera, "kid"));
		if (!ALGORITMO.equals(textoDe(cabecera, "alg")) || publica == null
				|| !firmaCorrecta(publica, partes[0] + "." + partes[1], decodificar(partes[2]))) {
			return Optional.empty();
		}
		JsonNode carga = JSON.readTree(decodificar(partes[1]));
		if (!carga.path("iat").isIntegralNumber() || !carga.path("exp").isIntegralNumber()
				|| !reloj.instant().isBefore(Instant.ofEpochSecond(carga.get("exp").asLong()))) {
			return Optional.empty();
		}
		return Optional.of(new ClaimsAcceso(UUID.fromString(textoDe(carga, "sub")),
				UUID.fromString(textoDe(carga, "sid"))));
	}

	private static String textoDe(JsonNode nodo, String campo) {
		JsonNode valor = nodo.path(campo);
		if (!valor.isString()) {
			throw new IllegalArgumentException("falta " + campo);
		}
		return valor.asString();
	}

	private byte[] firmar(String texto) {
		Ed25519Signer firmante = new Ed25519Signer();
		firmante.init(true, privadaActual);
		byte[] datos = texto.getBytes(StandardCharsets.US_ASCII);
		firmante.update(datos, 0, datos.length);
		return firmante.generateSignature();
	}

	private static boolean firmaCorrecta(Ed25519PublicKeyParameters publica, String texto, byte[] firma) {
		Ed25519Signer verificador = new Ed25519Signer();
		verificador.init(false, publica);
		byte[] datos = texto.getBytes(StandardCharsets.US_ASCII);
		verificador.update(datos, 0, datos.length);
		return verificador.verifySignature(firma);
	}

	private static String codificar(String json) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
	}

	private static byte[] decodificar(String parte) {
		return Base64.getUrlDecoder().decode(parte);
	}

	private static String escapar(String texto) {
		return texto.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
