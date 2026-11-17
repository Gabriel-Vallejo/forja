package es.forja.entreno.plan.modelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import es.forja.entreno.plan.modelo.GrupoMuscular.MusculoEjercicio;
import es.forja.entreno.plan.modelo.PlanActual.ObjetivoEjercicio;

class ReglasDiaTest {

	private static ObjetivoEjercicio ejercicio(int series, MusculoEjercicio... musculos) {
		return new ObjetivoEjercicio("x", "X", 0, series, 8, 12, 2, BigDecimal.TEN, BigDecimal.ONE,
				Estado.ACUMULANDO, "motivo", List.of(musculos), true);
	}

	private static MusculoEjercicio primario(GrupoMuscular g) {
		return new MusculoEjercicio(g, RolMuscular.PRIMARIO);
	}

	@Test
	void masDelSesentaPorCientoDeSeriesAlTrenInferiorEsPierna() {
		var dia = List.of(ejercicio(4, primario(GrupoMuscular.CUADRICEPS)), ejercicio(3, primario(GrupoMuscular.ISQUIOS)),
				ejercicio(2, primario(GrupoMuscular.PECHO)));

		assertThat(ReglasDia.nombre(dia)).isEqualTo(ReglasDia.PIERNA);
	}

	@Test
	void masDelSesentaPorCientoDeSeriesAlTrenSuperiorEsTorso() {
		var dia = List.of(ejercicio(4, primario(GrupoMuscular.PECHO)), ejercicio(4, primario(GrupoMuscular.DORSAL)),
				ejercicio(2, primario(GrupoMuscular.GEMELOS)));

		assertThat(ReglasDia.nombre(dia)).isEqualTo(ReglasDia.TORSO);
	}

	@Test
	void justoElSesentaPorCientoNoBastaYSaleFullBody() {
		var dia = List.of(ejercicio(6, primario(GrupoMuscular.PECHO)), ejercicio(4, primario(GrupoMuscular.CUADRICEPS)));

		assertThat(ReglasDia.nombre(dia)).isEqualTo(ReglasDia.FULL_BODY);
	}

	@Test
	void losSecundariosNoCuentanYSinPrimariosSaleFullBody() {
		var dia = List.of(ejercicio(5, new MusculoEjercicio(GrupoMuscular.PECHO, RolMuscular.SECUNDARIO)));

		assertThat(ReglasDia.nombre(dia)).isEqualTo(ReglasDia.FULL_BODY);
	}

	@Test
	void unEjercicioConDosPrimariosReparteSusSeries() {
		var dia = List.of(ejercicio(3, primario(GrupoMuscular.CUADRICEPS), primario(GrupoMuscular.GLUTEOS)),
				ejercicio(1, primario(GrupoMuscular.PECHO)));

		assertThat(ReglasDia.nombre(dia)).isEqualTo(ReglasDia.PIERNA);
	}

	@Test
	void sinSesionesTocaElPrimerDia() {
		assertThat(ReglasDia.diaDeHoy(List.of(1, 2, 3), Optional.empty())).isEqualTo(1);
	}

	@Test
	void despuesDelUltimoDiaVuelveAlPrimero() {
		assertThat(ReglasDia.diaDeHoy(List.of(1, 2, 3), Optional.of(2))).isEqualTo(3);
		assertThat(ReglasDia.diaDeHoy(List.of(1, 2, 3), Optional.of(3))).isEqualTo(1);
	}

	@Test
	void unDiaQueYaNoEstaEnElPlanTambienEmpiezaPorElPrimero() {
		assertThat(ReglasDia.diaDeHoy(List.of(1, 2, 3), Optional.of(9))).isEqualTo(1);
	}
}
