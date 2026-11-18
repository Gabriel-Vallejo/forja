package es.forja.entreno.rutina.modelo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lo que el usuario ha ajustado de su plan (PRD v0.1, «ajustes del plan») y se respeta cada vez que se genera.
 * Preferencias de entreno, no datos de salud.
 *
 * @param exclusiones {@code idExterno} quitados («no recomendar» o «reemplazar»)
 * @param prioritarios músculos a enfocar; mandan si la solicitud no trae los suyos
 * @param series series de la semana fijadas por {@code idExterno} («una serie más o menos»)
 * @param evitar {@code idExterno} del plan anterior, que pierden puntos si pidió más variedad
 */
public record Preferencias(Set<String> exclusiones, List<String> prioritarios, Map<String, Integer> series,
		Set<String> evitar) {

	public static final Preferencias NINGUNA = new Preferencias(Set.of(), List.of(), Map.of(), Set.of());

	public Preferencias {
		exclusiones = Set.copyOf(exclusiones);
		prioritarios = List.copyOf(prioritarios);
		series = Map.copyOf(series);
		evitar = Set.copyOf(evitar);
	}
}
