package es.forja.entreno.plan.modelo;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import es.forja.entreno.plan.modelo.GrupoMuscular.MusculoEjercicio;
import es.forja.entreno.plan.modelo.PlanActual.ObjetivoEjercicio;

/** Qué día toca hoy y cómo se llama cada día del plan. */
public final class ReglasDia {

	public static final String PIERNA = "PIERNA";
	public static final String TORSO = "TORSO";
	public static final String FULL_BODY = "FULL BODY";

	/** Porcentaje de series, estrictamente por encima del cual el día se llama PIERNA o TORSO. */
	static final int UMBRAL_PORCENTAJE = 60;

	private static final Set<GrupoMuscular> TREN_INFERIOR = EnumSet.of(GrupoMuscular.CUADRICEPS,
			GrupoMuscular.ADUCTORES, GrupoMuscular.ISQUIOS, GrupoMuscular.GLUTEOS, GrupoMuscular.GEMELOS);
	// Abdomen y lumbar son zona media: cuentan en el total, pero no son ni tren superior ni inferior.
	private static final Set<GrupoMuscular> TREN_SUPERIOR = EnumSet.of(GrupoMuscular.PECHO, GrupoMuscular.HOMBROS,
			GrupoMuscular.BRAZOS, GrupoMuscular.TRICEPS, GrupoMuscular.ANTEBRAZOS, GrupoMuscular.TRAPECIO,
			GrupoMuscular.DORSAL);

	private ReglasDia() {
	}

	/**
	 * Heurística propia, no ciencia: reparte las series de cada ejercicio entre sus grupos primarios; si más del 60 % va
	 * al tren inferior, «PIERNA»; si más del 60 % va al superior, «TORSO»; en otro caso, «FULL BODY».
	 */
	public static String nombre(List<ObjetivoEjercicio> ejercicios) {
		long total = 0;
		long inferior = 0;
		long superior = 0;
		for (ObjetivoEjercicio e : ejercicios) {
			for (MusculoEjercicio m : e.musculos()) {
				if (m.rol() != RolMuscular.PRIMARIO) {
					continue;
				}
				total += e.series();
				inferior += TREN_INFERIOR.contains(m.grupo()) ? e.series() : 0;
				superior += TREN_SUPERIOR.contains(m.grupo()) ? e.series() : 0;
			}
		}
		if (inferior * 100 > UMBRAL_PORCENTAJE * total) {
			return PIERNA;
		}
		return superior * 100 > UMBRAL_PORCENTAJE * total ? TORSO : FULL_BODY;
	}

	/**
	 * El día siguiente al de la última sesión, en ciclo. Sin sesiones, o si ese día ya no está en el plan, el primero.
	 *
	 * @param dias números de día del plan, en orden
	 */
	public static int diaDeHoy(List<Integer> dias, Optional<Integer> ultimoDia) {
		int posicion = ultimoDia.map(dias::indexOf).orElse(-1);
		return dias.get((posicion + 1) % dias.size());
	}
}
