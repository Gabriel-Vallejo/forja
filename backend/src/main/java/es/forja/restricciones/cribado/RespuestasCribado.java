package es.forja.restricciones.cribado;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Respuestas del cribado: las siete contestadas y, aparte, cuáles de los síntomas ocurrieron
 * en las últimas {@value #SEMANAS_RECIENTE} semanas (umbral provisional, pendiente de revisión clínica).
 */
public record RespuestasCribado(Map<PreguntaCribado, Boolean> respuestas, Set<PreguntaCribado> recientes) {

	public static final int SEMANAS_RECIENTE = 4;

	public RespuestasCribado {
		for (PreguntaCribado p : PreguntaCribado.values()) {
			if (respuestas == null || respuestas.get(p) == null) {
				throw new IllegalArgumentException("Falta contestar la pregunta " + p);
			}
		}
		respuestas = Map.copyOf(new EnumMap<>(respuestas));
		recientes = Set.copyOf(recientes);
		for (PreguntaCribado p : recientes) {
			if (!p.admiteReciente() || !respuestas.get(p)) {
				throw new IllegalArgumentException(
						"Solo un síntoma contestado con sí puede marcarse como reciente: " + p);
			}
		}
	}

	boolean si(PreguntaCribado pregunta) {
		return respuestas.get(pregunta);
	}

}
