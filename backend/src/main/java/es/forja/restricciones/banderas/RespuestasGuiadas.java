package es.forja.restricciones.banderas;

import java.util.EnumMap;
import java.util.Map;

import es.forja.restricciones.modelo.Region;

/**
 * Respuestas estructuradas de las preguntas guiadas (ADR 0007). Dato de salud (ADR 0004).
 *
 * @param dolorPorZona dolor actual de 0 a 10 por región; la región que falte cuenta como 0
 * @param hormigueoOEntumecimiento hormigueo o falta de sensibilidad nuevos o que van a más; uno crónico ya valorado no cuenta
 * @param perdidaDeFuerza debilidad nueva o que va a más; una crónica ya valorada por un profesional no cuenta
 */
public record RespuestasGuiadas(
		Map<Region, Integer> dolorPorZona,
		boolean dolorEnReposo,
		boolean dolorNocturnoQueDespierta,
		boolean hormigueoOEntumecimiento,
		boolean perdidaDeFuerza,
		boolean perdidaControlEsfinteresOAnestesiaSillaDeMontar,
		boolean fiebreConDolorDeEspalda,
		boolean traumatismoReciente,
		boolean perdidaDePesoSinExplicacion,
		boolean dolorToracico,
		boolean mareoODesmayo) {

	public static final int DOLOR_MINIMO = 0;
	public static final int DOLOR_MAXIMO = 10;

	public RespuestasGuiadas {
		dolorPorZona = dolorPorZona.isEmpty() ? Map.of() : Map.copyOf(new EnumMap<>(dolorPorZona));
		dolorPorZona.forEach((region, dolor) -> {
			if (dolor < DOLOR_MINIMO || dolor > DOLOR_MAXIMO) {
				throw new IllegalArgumentException(
						"El dolor de " + region.nombre() + " debe estar entre 0 y 10, y es " + dolor);
			}
		});
	}

	/** Hay dolor (más de 0) en alguna zona. */
	boolean hayDolor() {
		return dolorPorZona.values().stream().anyMatch(d -> d > DOLOR_MINIMO);
	}

}
