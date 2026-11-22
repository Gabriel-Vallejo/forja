package es.forja.sesiones.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import es.forja.sesiones.repositorio.SesionRepositorio.SerieConCarga;

/** El e1RM de la dominada y el fondo con lastre: Epley sobre peso corporal + lastre, menos el peso. */
class E1rmLastreTest {

	private static final BigDecimal OCHENTA = new BigDecimal("80");

	private static SerieConCarga serie(String kg, int reps) {
		return new SerieConCarga("Pullups", "Dominadas", new BigDecimal(kg), reps);
	}

	@Test
	void conPesoCorporalCuentaElCuerpoYTambienLasSeriesSinLastre() {
		// 80 kg × 12 reps sin lastre: 80 × 42 / 30 − 80 = 32 kg de lastre equivalente.
		assertThat(ServicioProgreso.e1rm(serie("0", 12), "dominada_lastre", OCHENTA)).hasValue(new BigDecimal("32.00"));
		// 20 kg × 5: 100 × 35 / 30 − 80 = 36,67.
		assertThat(ServicioProgreso.e1rm(serie("20", 5), "dominada_lastre", OCHENTA)).hasValue(new BigDecimal("36.67"));
	}

	@Test
	void sinPesoCorporalEsEpleySobreElLastreYSinLastreNoCuenta() {
		assertThat(ServicioProgreso.e1rm(serie("20", 5), "dominada_lastre", null)).hasValue(new BigDecimal("23.33"));
		assertThat(ServicioProgreso.e1rm(serie("0", 12), "dominada_lastre", null)).isEmpty();
	}

	@Test
	void masDe12RepeticionesNoCuentanYLosDemasEjerciciosNoMiranElPeso() {
		assertThat(ServicioProgreso.e1rm(serie("0", 13), "dominada_lastre", OCHENTA)).isEmpty();
		assertThat(ServicioProgreso.e1rm(serie("80", 8), "banca", OCHENTA)).hasValue(new BigDecimal("101.33"));
		assertThat(ServicioProgreso.e1rm(serie("0", 8), null, OCHENTA)).isEmpty();
	}
}
