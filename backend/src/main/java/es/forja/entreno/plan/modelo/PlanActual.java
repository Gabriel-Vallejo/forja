package es.forja.entreno.plan.modelo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * El plan con todo lo necesario para entrenar sin red (ADR 0008 §5): por día, el objetivo de cada ejercicio y la
 * regla de la siguiente.
 */
public record PlanActual(UUID uuid, Instant creadoEn, String estado, List<DiaEntreno> dias) {

	public PlanActual {
		dias = List.copyOf(dias);
	}

	public record DiaEntreno(int dia, List<ObjetivoEjercicio> ejercicios) {
		public DiaEntreno {
			ejercicios = List.copyOf(ejercicios);
		}
	}

	/**
	 * @param cargaKg objetivo de carga de la próxima sesión; nula hasta que el usuario entrene el ejercicio
	 * @param incrementoKg lo que sube la carga si se cumple el rango alto al RIR objetivo; 0 con peso corporal
	 */
	public record ObjetivoEjercicio(String idExterno, String nombre, int orden, int series, int repeticionesMin,
			int repeticionesMax, int rirObjetivo, BigDecimal cargaKg, BigDecimal incrementoKg) {
	}
}
