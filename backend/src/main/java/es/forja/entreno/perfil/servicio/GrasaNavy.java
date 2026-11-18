package es.forja.entreno.perfil.servicio;

import static java.lang.Math.log10;

import java.util.OptionalDouble;

import es.forja.entreno.perfil.modelo.Sexo;

/**
 * % de grasa por la fórmula de la US Navy (medidas en cm). Es una estimación con un error típico de unos 3-4 puntos
 * ({@link #MARGEN}): se presenta siempre con él.
 */
public final class GrasaNavy {

	/** Margen de error que se enseña junto a la estimación, en puntos porcentuales. */
	public static final double MARGEN = 4.0;

	// Fuera de este tramo la fórmula se está usando fuera de lo que describe: se descarta en vez de enseñar un absurdo.
	private static final double MINIMO_PLAUSIBLE = 2.0;
	private static final double MAXIMO_PLAUSIBLE = 60.0;

	private GrasaNavy() {
	}

	/** Vacío si el sexo no es hombre o mujer, falta alguna medida que la fórmula pide o el resultado no es plausible. */
	public static OptionalDouble porcentaje(Sexo sexo, Double alturaCm, Double cuelloCm, Double cinturaCm,
			Double caderaCm) {
		if (alturaCm == null || cuelloCm == null || cinturaCm == null) {
			return OptionalDouble.empty();
		}
		double densidad;
		if (sexo == Sexo.HOMBRE) {
			if (cinturaCm <= cuelloCm) {
				return OptionalDouble.empty();
			}
			densidad = 1.0324 - 0.19077 * log10(cinturaCm - cuelloCm) + 0.15456 * log10(alturaCm);
		} else if (sexo == Sexo.MUJER && caderaCm != null && cinturaCm + caderaCm > cuelloCm) {
			densidad = 1.29579 - 0.35004 * log10(cinturaCm + caderaCm - cuelloCm) + 0.22100 * log10(alturaCm);
		} else {
			return OptionalDouble.empty();
		}
		double grasa = 495 / densidad - 450;
		if (grasa < MINIMO_PLAUSIBLE || grasa > MAXIMO_PLAUSIBLE) {
			return OptionalDouble.empty();
		}
		return OptionalDouble.of(Math.round(grasa * 10) / 10.0);
	}
}
