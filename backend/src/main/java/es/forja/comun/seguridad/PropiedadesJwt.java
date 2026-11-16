package es.forja.comun.seguridad;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Claves de firma del token de acceso (ADR 0009). Si falta la actual, la aplicación no arranca.
 * {@code clavePrivadaActual} es PKCS#8 en base64 y {@code clavePublicaAnterior}, X.509 en base64.
 */
@Validated
@ConfigurationProperties("forja.jwt")
public record PropiedadesJwt(@NotBlank String kidActual, @NotBlank String clavePrivadaActual, String kidAnterior,
		String clavePublicaAnterior) {

	@Override
	public String toString() {
		return "PropiedadesJwt[kidActual=" + kidActual + "]";
	}
}
