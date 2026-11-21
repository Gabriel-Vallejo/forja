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
	/** Limitaciones v2 (2026-11-21): dice también que cubre el dolor de las series y el nivel de aviso del cuestionario. */
	public static final String VERSION_V2 = "v2";

	public String codigo() {
		return name().toLowerCase(Locale.ROOT);
	}

	/**
	 * La versión del texto que enseña la app. shortcut: un consentimiento dado con una versión anterior sigue
	 * valiendo; antes de tener usuarios de verdad, pedir que se vuelva a dar si el texto cambia en lo sustancial.
	 */
	public String versionVigente() {
		return this == LIMITACIONES ? VERSION_V2 : VERSION_V1;
	}

	public static TipoConsentimiento desdeCodigo(String codigo) {
		return Arrays.stream(values()).filter(t -> t.codigo().equals(codigo)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Tipo de consentimiento desconocido"));
	}
}
