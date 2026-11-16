package es.forja.entreno.plan.modelo;

import java.math.BigDecimal;

/** Explicaciones en español, de tú, de lo que toca en cada estado. Nunca prometen nada sobre salud. */
final class Motivos {

	private static final BigDecimal MITAD = new BigDecimal("0.5");
	private static final BigDecimal CIEN = BigDecimal.valueOf(100);

	private Motivos() {
	}

	private static String kg(BigDecimal valor) {
		return valor.stripTrailingZeros().toPlainString();
	}

	static String sinSesiones(ReglaEjercicio regla) {
		return "Todavía no hay sesiones de este ejercicio: elige una carga con la que te queden unas "
				+ regla.rirObjetivo() + " repeticiones en reserva.";
	}

	static String deload(ParametrosProgresion.Deload d) {
		BigDecimal porcentaje = d.factorCarga().multiply(CIEN);
		String series = d.factorSeries().compareTo(MITAD) == 0 ? "la mitad de series"
				: "el " + kg(d.factorSeries().multiply(CIEN)) + " % de las series";
		return d.semanas() + " semanas sin superar tu mejor marca estimada: esta semana, deload al "
				+ kg(porcentaje) + " % y " + series + ".";
	}

	static String de(Estado estado, ReglaEjercicio regla, BigDecimal carga, int minimo, int maximo,
			boolean enElTope, boolean trasDeload) {
		return switch (estado) {
			case SUBIR_CARGA -> subir(regla, carga, minimo, maximo, enElTope);
			default -> repetir(regla, carga, maximo, trasDeload);
		};
	}

	private static String subir(ReglaEjercicio regla, BigDecimal carga, int minimo, int maximo, boolean enElTope) {
		if (!regla.pesoCorporal()) {
			return "Has completado " + maximo(regla) + " repeticiones con " + regla.rirObjetivo()
					+ " en reserva o menos en todas las series: sube a " + kg(carga) + " kg (+"
					+ kg(regla.incrementoKg()) + " kg).";
		}
		if (enElTope) {
			return "Has llegado a " + maximo + " repeticiones: añade lastre para seguir progresando.";
		}
		return "Lo has cumplido con tu peso corporal: el objetivo pasa a " + minimo + "-" + maximo + " repeticiones.";
	}

	private static int maximo(ReglaEjercicio regla) {
		return regla.repeticionesMax();
	}

	private static String repetir(ReglaEjercicio regla, BigDecimal carga, int maximo, boolean trasDeload) {
		if (regla.pesoCorporal()) {
			return "Repite con tu peso corporal hasta completar " + maximo + " repeticiones con "
					+ regla.rirObjetivo() + " en reserva o menos en todas las series.";
		}
		if (trasDeload) {
			return "Deload hecho: vuelves a " + kg(carga) + " kg.";
		}
		return "Repite " + kg(carga) + " kg: te falta completar " + maximo + " repeticiones con "
				+ regla.rirObjetivo() + " en reserva o menos en todas las series de trabajo.";
	}
}
