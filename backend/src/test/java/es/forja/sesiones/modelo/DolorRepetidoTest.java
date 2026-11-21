package es.forja.sesiones.modelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import es.forja.sesiones.modelo.DolorRepetido.SerieConDolor;

/** RF-27: qué ejercicio ha dolido de forma repetida, sin Spring ni base de datos. */
class DolorRepetidoTest {

	private static final Instant LUNES = Instant.parse("2026-11-16T10:00:00Z");

	/** Una sesión por día con una serie de trabajo de {@code ejercicio} y ese dolor (null: sin anotar). */
	private static List<SerieConDolor> sesiones(String ejercicio, Integer... dolores) {
		List<SerieConDolor> series = new ArrayList<>();
		for (int i = 0; i < dolores.length; i++) {
			series.add(new SerieConDolor(UUID.randomUUID(), LUNES.plusSeconds(86_400L * i), ejercicio, dolores[i]));
		}
		return series;
	}

	@Test
	void dosDeLasTresUltimasSesionesConDolorPorEncimaDe5LoMarcan() {
		List<DolorRepetido> marcados = DolorRepetido.de(sesiones("sentadilla", 2, 6, null, 7));

		assertThat(marcados).containsExactly(new DolorRepetido("sentadilla", 2, 3));
	}

	@Test
	void unDolorDe5EsAceptableYNoCuenta() {
		assertThat(DolorRepetido.de(sesiones("sentadilla", 5, 5, 5))).isEmpty();
	}

	@Test
	void soloMiraLasTresUltimasSesionesDelEjercicio() {
		// Dolió al principio y ya no: las tres últimas están limpias.
		assertThat(DolorRepetido.de(sesiones("sentadilla", 8, 9, 0, 1, 2))).isEmpty();
	}

	@Test
	void unaSolaSesionConDolorNoEsRepetido() {
		assertThat(DolorRepetido.de(sesiones("sentadilla", 9))).isEmpty();
	}

	@Test
	void variasSeriesDeUnaSesionCuentanComoUnaYVaElDolorMasAlto() {
		UUID sesion = UUID.randomUUID();
		List<SerieConDolor> series = new ArrayList<>(List.of(new SerieConDolor(sesion, LUNES, "press", 1),
				new SerieConDolor(sesion, LUNES, "press", 8), new SerieConDolor(sesion, LUNES, "press", 9)));
		series.addAll(sesiones("press", 6));

		assertThat(DolorRepetido.de(series)).containsExactly(new DolorRepetido("press", 2, 2));
	}

	@Test
	void cadaEjercicioVaPorSuCuentaYSalenOrdenados() {
		List<SerieConDolor> series = new ArrayList<>(sesiones("remo", 7, 7));
		series.addAll(sesiones("press", 0, 0));
		series.addAll(sesiones("curl", 6, 8, 9));

		assertThat(DolorRepetido.de(series)).containsExactly(new DolorRepetido("curl", 3, 3),
				new DolorRepetido("remo", 2, 2));
	}
}
