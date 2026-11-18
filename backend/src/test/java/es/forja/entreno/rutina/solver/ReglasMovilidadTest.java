package es.forja.entreno.rutina.solver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import es.forja.entreno.movilidad.modelo.TestMovilidad;
import es.forja.entreno.movilidad.modelo.Tramo;

class ReglasMovilidadTest {

	/** Vocabulario sanitario que la finalidad de fitness prohíbe (ADR 0007). */
	private static final String PROHIBIDAS = "(?i).*(rehabilit|alivi|corrig|previen|lesi[oó]n|desequilibr|atrofi|seguro).*";

	@Test
	void sinTestsNoPuntuaNada() {
		assertThat(ReglasMovilidad.puntos(Map.of())).isEmpty();
	}

	@Test
	void tobilloLimitadoFavoreceLaGobletYPenalizaLasSentadillasConBarra() {
		Map<String, Integer> puntos = ReglasMovilidad.puntos(Map.of(TestMovilidad.TOBILLO, Tramo.LIMITADO));

		assertThat(puntos).containsEntry("Goblet_Squat", 2).containsEntry("Barbell_Squat", -2)
				.containsEntry("Front_Barbell_Squat", -2);
	}

	@Test
	void elTramoMedioPesaLaMitadYElBuenoNada() {
		assertThat(ReglasMovilidad.puntos(Map.of(TestMovilidad.HOMBRO, Tramo.MEDIO)))
				.containsEntry("Incline_Dumbbell_Press", 1).containsEntry("Standing_Military_Press", -1);
		assertThat(ReglasMovilidad.puntos(Map.of(TestMovilidad.CADERA, Tramo.BUENO))).isEmpty();
	}

	@Test
	void caderaLimitadaPenalizaElSumoYFavoreceLaPosturaCerrada() {
		assertThat(ReglasMovilidad.puntos(Map.of(TestMovilidad.CADERA, Tramo.LIMITADO)))
				.containsEntry("Sumo_Deadlift", -2).containsEntry("Barbell_Deadlift", 2);
	}

	@Test
	void elMotivoSoloVaEnLosFavorecidosYDiceElTestYElTramo() {
		Map<TestMovilidad, Tramo> tobillo = Map.of(TestMovilidad.TOBILLO, Tramo.MEDIO);

		assertThat(ReglasMovilidad.motivos("Goblet_Squat", tobillo)).singleElement().asString()
				.startsWith("Elegido por tu tramo de movilidad de tobillo (medio): ")
				.endsWith("Heurística de entrenadores, no ciencia.");
		assertThat(ReglasMovilidad.motivos("Barbell_Squat", tobillo)).isEmpty();
		assertThat(ReglasMovilidad.motivos("Goblet_Squat", Map.of(TestMovilidad.TOBILLO, Tramo.BUENO))).isEmpty();
	}

	@ParameterizedTest
	@EnumSource(TestMovilidad.class)
	void ningunMotivoUsaVocabularioSanitario(TestMovilidad test) {
		ReglasMovilidad.idsCitados().forEach(id -> assertThat(ReglasMovilidad.motivos(id, Map.of(test, Tramo.LIMITADO)))
				.allSatisfy(m -> assertThat(m).doesNotMatch(PROHIBIDAS)));
	}
}
