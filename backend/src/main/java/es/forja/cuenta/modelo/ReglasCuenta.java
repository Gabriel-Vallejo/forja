package es.forja.cuenta.modelo;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;

/** Reglas puras de la cuenta: sin Spring, sin base de datos. */
public final class ReglasCuenta {

	public static final int EDAD_MINIMA = 18;
	public static final int FALLOS_PARA_BLOQUEAR = 10;
	public static final Duration ESPERA_INICIAL = Duration.ofMinutes(1);
	public static final Duration ESPERA_MAXIMA = Duration.ofHours(1);

	private ReglasCuenta() {
	}

	/** Correo sin espacios alrededor y en minúsculas, para que el índice único no distinga mayúsculas. */
	public static String normalizarCorreo(String correo) {
		return correo.strip().toLowerCase(Locale.ROOT);
	}

	public static boolean esMayorDeEdad(LocalDate nacimiento, LocalDate hoy) {
		return !nacimiento.isAfter(hoy) && Period.between(nacimiento, hoy).getYears() >= EDAD_MINIMA;
	}

	/**
	 * Espera tras {@code fallosSeguidos} fallos: nada hasta el décimo; desde ahí 1 min y se duplica con cada
	 * fallo más, con tope de 1 h.
	 */
	public static Duration esperaTrasFallos(int fallosSeguidos) {
		if (fallosSeguidos < FALLOS_PARA_BLOQUEAR) {
			return Duration.ZERO;
		}
		int duplicaciones = fallosSeguidos - FALLOS_PARA_BLOQUEAR;
		long maximoMinutos = ESPERA_MAXIMA.dividedBy(ESPERA_INICIAL);
		if (duplicaciones >= Long.SIZE - 1 || (1L << duplicaciones) >= maximoMinutos) {
			return ESPERA_MAXIMA;
		}
		return ESPERA_INICIAL.multipliedBy(1L << duplicaciones);
	}
}
