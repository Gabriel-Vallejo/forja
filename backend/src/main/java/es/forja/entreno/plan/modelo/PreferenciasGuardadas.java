package es.forja.entreno.plan.modelo;

import java.util.List;
import java.util.stream.Collectors;

/** Las preferencias guardadas con sus nombres, para enseñarlas y poder quitarlas una a una. */
public record PreferenciasGuardadas(List<Elemento> exclusiones, List<Elemento> prioritarios,
		List<SeriesFijadas> series, boolean variedad) {

	public PreferenciasGuardadas {
		exclusiones = List.copyOf(exclusiones);
		prioritarios = List.copyOf(prioritarios);
		series = List.copyOf(series);
	}

	/** @param codigo {@code idExterno} si es un ejercicio, código si es un músculo */
	public record Elemento(String codigo, String nombre) {
	}

	public record SeriesFijadas(String idExterno, String nombre, int series) {
	}

	public PreferenciasPlan aCodigos() {
		return new PreferenciasPlan(exclusiones.stream().map(Elemento::codigo).toList(),
				prioritarios.stream().map(Elemento::codigo).toList(),
				series.stream().collect(Collectors.toMap(SeriesFijadas::idExterno, SeriesFijadas::series)), variedad);
	}
}
