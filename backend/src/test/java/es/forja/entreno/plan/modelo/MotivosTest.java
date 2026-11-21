package es.forja.entreno.plan.modelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MotivosTest {

	@ParameterizedTest
	@CsvSource(delimiter = '|', textBlock = """
			1 | Elige una carga con la que te quede 1 repetición en reserva.
			2 | Elige una carga con la que te queden unas 2 repeticiones en reserva.
			""")
	void sinSesionesConCargaUsaElSingularCon1(int rir, String esperado) {
		ReglaEjercicio regla = new ReglaEjercicio(3, 6, 10, rir, new BigDecimal("2.5"), null);

		assertThat(Motivos.sinSesiones(regla)).isEqualTo(esperado);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', textBlock = """
			1 | Haz cada serie con tu peso corporal dejando 1 repetición en reserva.
			3 | Haz cada serie con tu peso corporal dejando unas 3 repeticiones en reserva.
			""")
	void sinSesionesConPesoCorporalNoHablaDeCarga(int rir, String esperado) {
		ReglaEjercicio regla = new ReglaEjercicio(3, 8, 12, rir, BigDecimal.ZERO, null);

		assertThat(Motivos.sinSesiones(regla)).isEqualTo(esperado).doesNotContain("carga");
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', textBlock = """
			1 | Repite 50 kg: te falta completar 10 repeticiones con 2 en reserva o menos en la serie de trabajo.
			3 | Repite 50 kg: te falta completar 10 repeticiones con 2 en reserva o menos en las 3 series de trabajo.
			""")
	void repetirNombraLasSeriesPrevistas(int series, String esperado) {
		ReglaEjercicio regla = new ReglaEjercicio(series, 6, 10, 2, new BigDecimal("2.5"), null);

		assertThat(Motivos.de(Estado.ACUMULANDO, regla, new BigDecimal("50"), 6, 10, false, false))
				.isEqualTo(esperado);
	}
}
