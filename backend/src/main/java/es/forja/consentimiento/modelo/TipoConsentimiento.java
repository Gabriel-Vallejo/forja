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
	/**
	 * v2: limitaciones (2026-11-21) dice también que cubre el dolor de las series y el nivel de aviso del cuestionario;
	 * medidas (2026-11-22) dice cuánto se guarda, cómo, y que retirarlo no anula lo hecho antes (art. 7.3).
	 */
	public static final String VERSION_V2 = "v2";

	public String codigo() {
		return name().toLowerCase(Locale.ROOT);
	}

	/**
	 * La versión del texto que enseña la app. Un consentimiento dado con una versión anterior sigue vivo aquí (los
	 * datos y el plan no cambian de golpe), pero la app lo detecta por su versión y pide darlo de nuevo con el texto
	 * vigente o retirarlo, antes de dejar seguir.
	 */
	public String versionVigente() {
		return switch (this) {
			case LIMITACIONES, MEDIDAS_SALUD -> VERSION_V2;
			case FOTOS_CUERPO, LESIONES, SUPLEMENTOS -> VERSION_V1;
		};
	}

	public static TipoConsentimiento desdeCodigo(String codigo) {
		return Arrays.stream(values()).filter(t -> t.codigo().equals(codigo)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Tipo de consentimiento desconocido"));
	}
}
