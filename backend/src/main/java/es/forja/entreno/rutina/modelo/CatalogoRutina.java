package es.forja.entreno.rutina.modelo;

import java.util.List;
import java.util.Map;

/**
 * Catálogo con el que se arma la rutina.
 *
 * @param nombresMusculo código → nombre con tildes, para los mensajes
 */
public record CatalogoRutina(List<EjercicioRutina> ejercicios, Map<String, String> nombresMusculo) {

	public CatalogoRutina {
		ejercicios = List.copyOf(ejercicios);
		nombresMusculo = Map.copyOf(nombresMusculo);
	}
}
