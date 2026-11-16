package es.forja.consentimiento.modelo;

import java.util.Arrays;
import java.util.Locale;

/**
 * Qué se consiente (ADR 0004, §3 y enmienda 3). Los códigos coinciden con el ENUM de la tabla.
 * {@code versionVigente} es la del texto que la app enseña hoy: si el cliente manda otra, no vale.
 */
public enum TipoConsentimiento {
	FOTOS_CUERPO, MEDIDAS_SALUD, LESIONES, LIMITACIONES, SUPLEMENTOS;

	public static final String VERSION_V1 = "v1";

	public String codigo() {
		return name().toLowerCase(Locale.ROOT);
	}

	/** Hoy todos los textos van por la v1. */
	public String versionVigente() {
		return VERSION_V1;
	}

	public static TipoConsentimiento desdeCodigo(String codigo) {
		return Arrays.stream(values()).filter(t -> t.codigo().equals(codigo)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Tipo de consentimiento desconocido"));
	}
}
