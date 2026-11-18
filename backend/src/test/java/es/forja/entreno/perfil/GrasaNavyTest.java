package es.forja.entreno.perfil;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.OptionalDouble;

import org.junit.jupiter.api.Test;

import es.forja.entreno.perfil.modelo.Sexo;
import es.forja.entreno.perfil.servicio.GrasaNavy;

/** Fórmula de la US Navy (cm). Los valores esperados están calculados a mano con las fórmulas del PRD. */
class GrasaNavyTest {

	@Test
	void hombreConCinturaYCuello() {
		// 495 / (1,0324 - 0,19077 * log10(85 - 38) + 0,15456 * log10(180)) - 450
		assertThat(GrasaNavy.porcentaje(Sexo.HOMBRE, 180.0, 38.0, 85.0, null)).hasValue(16.1);
	}

	@Test
	void hombreConMasCinturaQueCuello() {
		assertThat(GrasaNavy.porcentaje(Sexo.HOMBRE, 175.0, 40.0, 110.0, null)).hasValue(31.9);
	}

	@Test
	void mujerNecesitaTambienLaCadera() {
		// 495 / (1,29579 - 0,35004 * log10(72 + 98 - 32) + 0,22100 * log10(165)) - 450
		assertThat(GrasaNavy.porcentaje(Sexo.MUJER, 165.0, 32.0, 72.0, 98.0)).hasValue(27.4);
		assertThat(GrasaNavy.porcentaje(Sexo.MUJER, 165.0, 32.0, 72.0, null)).isEmpty();
	}

	@Test
	void sinSexoUtilNoHayEstimacion() {
		assertThat(GrasaNavy.porcentaje(Sexo.NO_INDICA, 180.0, 38.0, 85.0, 95.0)).isEmpty();
	}

	@Test
	void sinAlgunaMedidaNoHayEstimacion() {
		assertThat(GrasaNavy.porcentaje(Sexo.HOMBRE, null, 38.0, 85.0, null)).isEmpty();
		assertThat(GrasaNavy.porcentaje(Sexo.HOMBRE, 180.0, null, 85.0, null)).isEmpty();
		assertThat(GrasaNavy.porcentaje(Sexo.HOMBRE, 180.0, 38.0, null, null)).isEmpty();
	}

	@Test
	void unResultadoFueraDeLoPlausibleSeDescarta() {
		// La fórmula da -7,2 con cintura 70 y cuello 45 a 190 cm: no es un porcentaje.
		assertThat(GrasaNavy.porcentaje(Sexo.HOMBRE, 190.0, 45.0, 70.0, null)).isEqualTo(OptionalDouble.empty());
	}

	@Test
	void unaDiferenciaNoPositivaNoSeCalcula() {
		assertThat(GrasaNavy.porcentaje(Sexo.HOMBRE, 180.0, 90.0, 85.0, null)).isEmpty();
	}
}
