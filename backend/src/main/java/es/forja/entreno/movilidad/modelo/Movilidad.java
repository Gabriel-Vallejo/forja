package es.forja.entreno.movilidad.modelo;

import java.time.Instant;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Respuesta de {@code GET} y {@code PUT /api/v1/movilidad}: un resultado por test, nulo si está sin hacer.
 */
public record Movilidad(@Nullable ResultadoMovilidad tobillo, @Nullable ResultadoMovilidad cadera,
		@Nullable ResultadoMovilidad hombro) {

	/** @param tramo {@code limitado}, {@code medio} o {@code bueno}; {@code hechoEn}, cuando cambió por última vez */
	public record ResultadoMovilidad(String tramo, Instant hechoEn) {
	}

	public static Movilidad de(Map<TestMovilidad, ResultadoMovilidad> porTest) {
		return new Movilidad(porTest.get(TestMovilidad.TOBILLO), porTest.get(TestMovilidad.CADERA),
				porTest.get(TestMovilidad.HOMBRO));
	}
}
