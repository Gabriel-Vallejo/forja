package es.forja.entreno.plan.modelo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import es.forja.entreno.plan.modelo.GrupoMuscular.MusculoEjercicio;

/**
 * El plan con todo lo necesario para entrenar sin red (ADR 0008 §5): por día, el objetivo de cada ejercicio y la
 * regla de la siguiente.
 *
 * @param diaDeHoy el día siguiente al de la última sesión registrada con este plan, en ciclo; el primero si no hay
 */
public record PlanActual(UUID uuid, Instant creadoEn, String estado, int diaDeHoy, List<DiaEntreno> dias) {

	public PlanActual {
		dias = List.copyOf(dias);
	}

	/** @param nombre «PIERNA», «TORSO» o «FULL BODY», deducido de los grupos primarios (ver {@link ReglasDia}) */
	public record DiaEntreno(int dia, String nombre, List<ObjetivoEjercicio> ejercicios) {
		public DiaEntreno {
			ejercicios = List.copyOf(ejercicios);
		}
	}

	/**
	 * @param estado en qué punto de la progresión está el ejercicio (RF-26)
	 * @param series las del plan, o las del deload si toca
	 * @param repeticionesMin y {@code repeticionesMax}: el rango objetivo; con peso corporal sube al cumplir la regla
	 * @param cargaKg objetivo de carga de la próxima sesión; nula hasta que el usuario entrene el ejercicio
	 * @param motivo qué toca y por qué, en español
	 * @param incrementoKg lo que sube la carga si se cumple el rango alto al RIR objetivo; 0 con peso corporal
	 * @param musculos grupos del cuerpo que trabaja, sin repetir y primarios primero
	 * @param primeraSesion no hay ninguna sesión de trabajo de este ejercicio
	 */
	public record ObjetivoEjercicio(String idExterno, String nombre, int orden, int series, int repeticionesMin,
			int repeticionesMax, int rirObjetivo, BigDecimal cargaKg, BigDecimal incrementoKg, Estado estado,
			String motivo, List<MusculoEjercicio> musculos, boolean primeraSesion) {

		public ObjetivoEjercicio {
			musculos = List.copyOf(musculos);
		}
	}
}
