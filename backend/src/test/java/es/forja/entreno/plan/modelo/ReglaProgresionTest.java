package es.forja.entreno.plan.modelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** La regla de la siguiente sesión (ADR 0008 §5), sin Spring ni base de datos. */
class ReglaProgresionTest {

	private static final int REPETICIONES_MAX = 10;
	private static final int RIR_OBJETIVO = 2;
	private static final BigDecimal INCREMENTO = new BigDecimal("2.5");

	private static BigDecimal kg(String valor) {
		return new BigDecimal(valor);
	}

	private static Optional<BigDecimal> siguiente(List<SerieTrabajo> series) {
		return ReglaProgresion.siguienteCarga(series, 1, REPETICIONES_MAX, RIR_OBJETIVO, INCREMENTO);
	}

	@Test
	void repiteLaCargaSiFaltanSeriesPrevistasAunqueLasHechasCumplan() {
		List<SerieTrabajo> series = List.of(new SerieTrabajo(kg("50"), 10, 2), new SerieTrabajo(kg("50"), 10, 2));

		assertThat(ReglaProgresion.siguienteCarga(series, 3, REPETICIONES_MAX, RIR_OBJETIVO, INCREMENTO))
				.hasValueSatisfying(c -> assertThat(c).isEqualByComparingTo("50"));
	}

	@Test
	void hacerMasSeriesDeLasPrevistasNoImpideSubir() {
		List<SerieTrabajo> series = List.of(new SerieTrabajo(kg("50"), 10, 2), new SerieTrabajo(kg("50"), 10, 1),
				new SerieTrabajo(kg("50"), 10, 2));

		assertThat(ReglaProgresion.siguienteCarga(series, 2, REPETICIONES_MAX, RIR_OBJETIVO, INCREMENTO))
				.hasValueSatisfying(c -> assertThat(c).isEqualByComparingTo("52.5"));
	}

	@Test
	void subeElIncrementoSiTodasLasSeriesLlegaronAlRangoAltoConElRirObjetivo() {
		List<SerieTrabajo> series = List.of(new SerieTrabajo(kg("50"), 10, 2), new SerieTrabajo(kg("50"), 11, 1),
				new SerieTrabajo(kg("50"), 10, 0));

		assertThat(siguiente(series)).hasValueSatisfying(c -> assertThat(c).isEqualByComparingTo("52.5"));
	}

	@Test
	void repiteLaCargaSiUnaSerieNoLlegaAlRangoAlto() {
		List<SerieTrabajo> series = List.of(new SerieTrabajo(kg("50"), 10, 2), new SerieTrabajo(kg("50"), 9, 2));

		assertThat(siguiente(series)).hasValueSatisfying(c -> assertThat(c).isEqualByComparingTo("50"));
	}

	@Test
	void repiteLaCargaSiUnaSerieTuvoMasRirQueElObjetivo() {
		List<SerieTrabajo> series = List.of(new SerieTrabajo(kg("50"), 10, 3));

		assertThat(siguiente(series)).hasValueSatisfying(c -> assertThat(c).isEqualByComparingTo("50"));
	}

	@Test
	void sinRirRegistradoNoSePuedeAfirmarQueSeCumplio() {
		List<SerieTrabajo> series = List.of(new SerieTrabajo(kg("50"), 12, null));

		assertThat(siguiente(series)).hasValueSatisfying(c -> assertThat(c).isEqualByComparingTo("50"));
	}

	@Test
	void parteDeLaCargaMasAltaDeLasSeriesDeTrabajo() {
		List<SerieTrabajo> series = List.of(new SerieTrabajo(kg("45"), 10, 2), new SerieTrabajo(kg("50"), 10, 2));

		assertThat(siguiente(series)).hasValueSatisfying(c -> assertThat(c).isEqualByComparingTo("52.5"));
	}

	@Test
	void sinSeriesNoHayCargaQueProponer() {
		assertThat(siguiente(List.of())).isEmpty();
	}

	@Test
	void conIncrementoCeroLaCargaNoCambiaAunqueSeCumpla() {
		List<SerieTrabajo> series = List.of(new SerieTrabajo(kg("0"), 10, 2));

		assertThat(ReglaProgresion.siguienteCarga(series, 1, REPETICIONES_MAX, RIR_OBJETIVO, BigDecimal.ZERO))
				.hasValueSatisfying(c -> assertThat(c).isEqualByComparingTo("0"));
	}

	/** Propiedad con semilla fija: nunca baja la carga y solo sube cuando se hacen las previstas y todas cumplen. */
	@ParameterizedTest
	@ValueSource(longs = { 1, 2, 3, 4, 5, 6, 7, 8 })
	void nuncaBajaLaCargaYSoloSubeSiTodasLasSeriesCumplen(long semilla) {
		Random azar = new Random(semilla);
		for (int caso = 0; caso < 200; caso++) {
			List<SerieTrabajo> series = seriesAlAzar(azar);
			int previstas = 1 + azar.nextInt(4);
			BigDecimal base = series.stream().map(SerieTrabajo::pesoKg).max(BigDecimal::compareTo).orElseThrow();
			boolean cumplen = series.size() >= previstas && series.stream()
					.allMatch(s -> s.repeticiones() >= REPETICIONES_MAX && s.rir() != null && s.rir() <= RIR_OBJETIVO);

			BigDecimal resultado = ReglaProgresion
					.siguienteCarga(series, previstas, REPETICIONES_MAX, RIR_OBJETIVO, INCREMENTO).orElseThrow();

			assertThat(resultado).isEqualByComparingTo(cumplen ? base.add(INCREMENTO) : base);
		}
	}

	private static List<SerieTrabajo> seriesAlAzar(Random azar) {
		List<SerieTrabajo> series = new ArrayList<>();
		int cuantas = 1 + azar.nextInt(5);
		for (int i = 0; i < cuantas; i++) {
			// Sesgado a que cumplan, para que las dos ramas de la regla salgan a menudo.
			int repeticiones = azar.nextInt(4) == 0 ? azar.nextInt(REPETICIONES_MAX) : REPETICIONES_MAX + azar.nextInt(3);
			Integer rir = azar.nextInt(6) == 0 ? null : azar.nextInt(4);
			series.add(new SerieTrabajo(BigDecimal.valueOf(20 + azar.nextInt(40) * 2.5), repeticiones, rir));
		}
		return series;
	}
}
