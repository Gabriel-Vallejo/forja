package es.forja.restricciones.banderas;

import java.util.Objects;

/**
 * Señal de aviso que para la generación de la rutina.
 *
 * @param motivo texto para el usuario, de fitness y sin diagnóstico
 * @param guia guía pública de la que sale la regla
 */
public record BanderaRoja(Nivel nivel, String motivo, String guia) {

	public enum Nivel {
		/** Primero en la lista: el usuario debe ir a urgencias hoy. */
		URGENCIAS_HOY,
		PROFESIONAL_EN_DIAS
	}

	public BanderaRoja {
		Objects.requireNonNull(nivel, "nivel es obligatorio");
		Objects.requireNonNull(motivo, "motivo es obligatorio");
		Objects.requireNonNull(guia, "guia es obligatoria");
	}

}
