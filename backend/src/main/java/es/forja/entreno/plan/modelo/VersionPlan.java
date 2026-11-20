package es.forja.entreno.plan.modelo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Una versión guardada del plan tal cual está en la base, para la exportación: sus ejercicios con el objetivo que se
 * guardó (no el que calcula la progresión) y la traza sin datos de salud.
 *
 * @param actual es la última versión, la que sirve {@code /planes/actual}
 */
public record VersionPlan(UUID uuid, Instant creadoEn, String estado, boolean actual,
		List<EjercicioVersion> ejercicios, TrazaGuardada traza) {

	public VersionPlan {
		ejercicios = List.copyOf(ejercicios);
	}

	public record EjercicioVersion(int dia, int orden, String idExterno, String nombre, int series,
			int repeticionesMin, int repeticionesMax, int rirObjetivo, BigDecimal cargaKg, BigDecimal incrementoKg) {
	}
}
