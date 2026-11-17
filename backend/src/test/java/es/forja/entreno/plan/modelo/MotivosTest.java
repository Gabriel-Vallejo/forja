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
}
