package es.forja.entreno.movilidad.modelo;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Cuerpo de {@code PUT /api/v1/movilidad}: el tramo de cada test ({@code limitado}, {@code medio} o {@code bueno}).
 * Reemplaza los tres: un nulo deja ese test sin hacer.
 */
public record TramosMovilidad(String tobillo, String cadera, String hombro) {

	/** Tramo por test, solo de los que vienen; falla con {@link IllegalArgumentException} si alguno no existe. */
	public Map<TestMovilidad, Tramo> aMapa() {
		Map<TestMovilidad, Tramo> tramos = new EnumMap<>(TestMovilidad.class);
		poner(tramos, TestMovilidad.TOBILLO, tobillo);
		poner(tramos, TestMovilidad.CADERA, cadera);
		poner(tramos, TestMovilidad.HOMBRO, hombro);
		return tramos;
	}

	private static void poner(Map<TestMovilidad, Tramo> tramos, TestMovilidad test, String codigo) {
		Optional.ofNullable(codigo).map(c -> Tramo.desdeCodigo(c).orElseThrow(
				() -> new IllegalArgumentException("El tramo de " + test.codigo() + " es limitado, medio o bueno.")))
				.ifPresent(t -> tramos.put(test, t));
	}
}
