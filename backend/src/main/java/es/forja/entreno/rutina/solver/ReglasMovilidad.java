package es.forja.entreno.rutina.solver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import es.forja.entreno.movilidad.modelo.TestMovilidad;
import es.forja.entreno.movilidad.modelo.Tramo;

/**
 * Qué variantes cambia el tramo de cada test de movilidad (RF-09, ADR 0007). No prohíbe nada: da puntos por serie
 * en el objetivo (por el peso {@code movilidad}), positivos a las variantes que suelen ir mejor con ese tramo y
 * negativos a las que piden más rango. Limitado cuenta {@value #PUNTOS_LIMITADO}, medio la mitad y bueno nada.
 *
 * <p>Heurística de entrenadores (nivel H), no ciencia. Lo único con evidencia es que la dorsiflexión del test de rodilla
 * a la pared predice la profundidad de la sentadilla (ADR 0007); la variante que se elige con ella es criterio de
 * entrenador. Solo cita ejercicios del catálogo curado ({@code GeneradorMovilidadTest} lo comprueba).
 */
final class ReglasMovilidad {

	static final int PUNTOS_LIMITADO = 2;
	private static final int PUNTOS_MEDIO = 1;
	private static final String FUENTE = " Heurística de entrenadores, no ciencia.";

	private record Regla(TestMovilidad test, Set<String> favorecidos, Set<String> penalizados, String porQue) {
	}

	private static final List<Regla> REGLAS = List.of(
			// Rodilla a la pared: con la carga delante (y una talonera o un disco bajo los talones) se baja con menos
			// dorsiflexión que con la barra a la espalda o la frontal.
			new Regla(TestMovilidad.TOBILLO, Set.of("Goblet_Squat"), Set.of("Barbell_Squat", "Front_Barbell_Squat"),
					"con la carga delante y una talonera o un disco bajo los talones se suele bajar más con el tronco "
							+ "recto que con la barra."),
			// Rotación de cadera: el sumo abre mucho los pies y pide rotar la cadera; la postura cerrada, menos.
			new Regla(TestMovilidad.CADERA, Set.of("Barbell_Deadlift", "Trap_Bar_Deadlift"), Set.of("Sumo_Deadlift"),
					"con los pies a la anchura de las caderas se pide menos rotación de cadera que en el sumo."),
			// Flexión de hombro: el press de pie lleva los brazos del todo arriba y, si no llegan, se compensa
			// arqueando la zona lumbar; el inclinado trabaja hombro y pecho alto sin llegar a ese rango.
			new Regla(TestMovilidad.HOMBRO, Set.of("Barbell_Incline_Bench_Press_-_Medium_Grip", "Incline_Dumbbell_Press"),
					Set.of("Standing_Military_Press", "Dumbbell_Shoulder_Press"),
					"el press inclinado trabaja el hombro sin llevar los brazos del todo por encima de la cabeza."));

	private ReglasMovilidad() {
	}

	/** Puntos por serie de cada ejercicio que cambia con estos tramos, por {@code idExterno}; sin entrada si no cambia. */
	static Map<String, Integer> puntos(Map<TestMovilidad, Tramo> tramos) {
		Map<String, Integer> puntos = new TreeMap<>();
		for (Regla r : REGLAS) {
			int peso = peso(tramos.get(r.test()));
			if (peso == 0) {
				continue;
			}
			r.favorecidos().forEach(id -> puntos.merge(id, peso, Integer::sum));
			r.penalizados().forEach(id -> puntos.merge(id, -peso, Integer::sum));
		}
		return puntos;
	}

	/** Un test cuyo tramo favorece el ejercicio, con el porqué. */
	record Favorece(TestMovilidad test, Tramo tramo, String porQue) {
	}

	/** Los tests cuyo tramo favorece este ejercicio; vacío si ninguno. */
	static List<Favorece> favorecen(String idExterno, Map<TestMovilidad, Tramo> tramos) {
		List<Favorece> favorecen = new ArrayList<>();
		for (Regla r : REGLAS) {
			Tramo tramo = tramos.get(r.test());
			if (peso(tramo) > 0 && r.favorecidos().contains(idExterno)) {
				favorecen.add(new Favorece(r.test(), tramo, r.porQue()));
			}
		}
		return favorecen;
	}

	/** Por qué el tramo favorece este ejercicio, para la traza; vacío si no lo favorece. */
	static List<String> motivos(String idExterno, Map<TestMovilidad, Tramo> tramos) {
		return favorecen(idExterno, tramos).stream().map(f -> "Elegido por tu tramo de movilidad de %s (%s): %s%s"
				.formatted(f.test().codigo(), f.tramo().codigo(), f.porQue(), FUENTE)).toList();
	}

	static Set<String> idsCitados() {
		Set<String> ids = new TreeSet<>();
		REGLAS.forEach(r -> {
			ids.addAll(r.favorecidos());
			ids.addAll(r.penalizados());
		});
		return ids;
	}

	private static int peso(Tramo tramo) {
		if (tramo == null) {
			return 0;
		}
		return switch (tramo) {
			case LIMITADO -> PUNTOS_LIMITADO;
			case MEDIO -> PUNTOS_MEDIO;
			case BUENO -> 0;
		};
	}
}
