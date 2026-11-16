package es.forja.entreno.rutina.solver;

import java.util.Locale;

/**
 * Textos de la traza, en español y de tú. Finalidad de fitness (ADR 0007): nada de vocabulario sanitario.
 */
final class Textos {

	static final String RESUMEN_OPTIMO = "Es el mejor plan que cabe en lo que nos has dado.";
	static final String RESUMEN_FACTIBLE = "Es un buen plan: en el tiempo fijado no se ha podido demostrar que sea "
			+ "el mejor.";
	static final String FUENTE_VOLUMEN = "heurística de entrenadores (RP), nivel H";
	static final String VOLUMEN = "volumen";
	static final String FRECUENCIA = "frecuencia";

	private static final Locale ES = Locale.forLanguageTag("es-ES");

	private Textos() {
	}

	/** «con 3 días y 45 min no llegas a 10 series de espalda; te quedan 8» (ADR 0002 §3). */
	static String holguraVolumen(int dias, int minutos, int minimo, String musculo, double series) {
		return "Con %d días y %d min no llegas a %d series de %s; te quedan %s.".formatted(dias, minutos, minimo,
				musculo.toLowerCase(ES), numero(series));
	}

	static String holguraFrecuencia(int dias, int minutos, int minima, String musculo, int entrenado) {
		return ("Con %d días y %d min no llegas a entrenar %s %d días por semana; lo entrenas %d. Repartirlo así es "
				+ "una heurística práctica, no un requisito de la evidencia.").formatted(dias, minutos,
						musculo.toLowerCase(ES), minima, entrenado);
	}

	static String minimoRebajado(int minimo) {
		return ("Con tu material y tus limitaciones no hay ejercicios compatibles para llegar a %d por sesión: "
				+ "alguna sesión lleva menos.").formatted(minimo);
	}

	static String alternativa(String nombre, Long diferencia, boolean inviable) {
		String alternativa = minusculaInicial(nombre);
		if (inviable) {
			return "Con " + alternativa + " el plan no cabe en tus límites de tiempo y volumen.";
		}
		if (diferencia == null) {
			return "No se ha podido comparar con " + alternativa + " dentro del límite fijado.";
		}
		if (diferencia == 0) {
			return "Con " + alternativa + " en su lugar el plan queda igual de bien.";
		}
		return diferencia > 0 ? "Con %s en su lugar el plan pierde %d puntos.".formatted(alternativa, diferencia)
				: "Con %s en su lugar el plan gana %d puntos.".formatted(alternativa, -diferencia);
	}

	/** 8 → «8» y 8,5 → «8,5». */
	static String numero(double valor) {
		return valor == Math.rint(valor) ? Long.toString(Math.round(valor)) : String.format(ES, "%.1f", valor);
	}

	private static String minusculaInicial(String texto) {
		return texto.isEmpty() ? texto : Character.toLowerCase(texto.charAt(0)) + texto.substring(1);
	}
}
