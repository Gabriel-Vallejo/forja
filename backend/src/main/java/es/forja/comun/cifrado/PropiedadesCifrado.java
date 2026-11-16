package es.forja.comun.cifrado;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Claves del cifrado por columna (ADR 0004, enmienda 8). Si falta la actual, la aplicación no arranca.
 *
 * @param claveActual 32 bytes en base64
 * @param clavesAnteriores opcional: {@code kid:base64,kid:base64}, solo para descifrar durante la rotación
 */
@Validated
@ConfigurationProperties("forja.cifrado")
public record PropiedadesCifrado(@NotBlank String kidActual, @NotBlank String claveActual, String clavesAnteriores) {

	@Override
	public String toString() {
		return "PropiedadesCifrado[kidActual=" + kidActual + "]";
	}
}
