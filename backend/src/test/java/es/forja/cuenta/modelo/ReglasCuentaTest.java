package es.forja.cuenta.modelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ReglasCuentaTest {

	private static final LocalDate HOY = LocalDate.of(2026, 11, 16);

	@Test
	void normalizaElCorreoQuitandoEspaciosYMayusculas() {
		assertThat(ReglasCuenta.normalizarCorreo("  Gabriel@Forja.ES ")).isEqualTo("gabriel@forja.es");
	}

	@ParameterizedTest
	@CsvSource({
			"2008-11-16, true",   // cumple 18 hoy
			"2008-11-17, false",  // los cumple mañana
			"2000-01-01, true",
			"2010-05-05, false",
			"2008-02-29, true",   // nacido un 29 de febrero
			"2027-01-01, false"   // fecha futura
	})
	void soloSeAdmiteLaMayoriaDeEdadCumplida(LocalDate nacimiento, boolean esperado) {
		assertThat(ReglasCuenta.esMayorDeEdad(nacimiento, HOY)).isEqualTo(esperado);
	}

	@ParameterizedTest
	@CsvSource({
			"0, 0", "1, 0", "9, 0",
			"10, 1", "11, 2", "12, 4", "13, 8", "14, 16", "15, 32",
			"16, 60", "17, 60", "40, 60", "2147483647, 60"
	})
	void laEsperaEmpiezaEnElDecimoFalloDuplicaYTieneTope(int fallos, long minutosEsperados) {
		assertThat(ReglasCuenta.esperaTrasFallos(fallos)).isEqualTo(Duration.ofMinutes(minutosEsperados));
	}
}
