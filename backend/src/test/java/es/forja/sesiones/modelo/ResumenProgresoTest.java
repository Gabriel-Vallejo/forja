package es.forja.sesiones.modelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import es.forja.sesiones.modelo.ResumenProgreso.SesionHecha;

/** Racha semanal y cifras del perfil, sin Spring ni base de datos. */
class ResumenProgresoTest {

	/** Un jueves: la semana en curso empieza el lunes 16. */
	private static final LocalDate HOY = LocalDate.of(2026, 11, 19);
	private static final LocalDate LUNES = LocalDate.of(2026, 11, 16);

	private static SesionHecha el(LocalDate dia, String kg) {
		return new SesionHecha(dia, new BigDecimal(kg));
	}

	/** {@code dias} sesiones en la semana que empieza {@code semanasAtras} lunes antes del actual. */
	private static List<SesionHecha> semana(int semanasAtras, int dias) {
		List<SesionHecha> sesiones = new ArrayList<>();
		for (int d = 0; d < dias; d++) {
			sesiones.add(el(LUNES.minusWeeks(semanasAtras).plusDays(d), "1000"));
		}
		return sesiones;
	}

	@Test
	void sinSesionesTodoACero() {
		ResumenProgreso r = ResumenProgreso.de(List.of(), 3, HOY);

		assertThat(r.sesiones()).isZero();
		assertThat(r.semanasDeRacha()).isZero();
		assertThat(r.mejorRacha()).isZero();
		assertThat(r.porcentajeCumplido()).isZero();
		assertThat(r.ultimasSemanas()).hasSize(ResumenProgreso.SEMANAS_EN_EL_MAPA)
				.allSatisfy(s -> assertThat(s.dias()).containsOnly(false));
	}

	@Test
	void cuentanLasSemanasCumplidasSeguidasYLaEnCursoNoRompeLaRacha() {
		List<SesionHecha> sesiones = new ArrayList<>();
		sesiones.addAll(semana(3, 3));
		sesiones.addAll(semana(2, 3));
		sesiones.addAll(semana(1, 4));
		sesiones.addAll(semana(0, 1)); // la semana en curso aún no se ha cumplido

		ResumenProgreso r = ResumenProgreso.de(sesiones, 3, HOY);

		assertThat(r.semanasDeRacha()).isEqualTo(3);
		assertThat(r.hechosEstaSemana()).isEqualTo(1);
		assertThat(r.objetivoSemanal()).isEqualTo(3);
		assertThat(r.sesiones()).isEqualTo(11);
		assertThat(r.kgMovidos()).isEqualByComparingTo("11000");
	}

	@Test
	void unaSemanaSinCumplirCortaLaRachaPeroNoLaMejor() {
		List<SesionHecha> sesiones = new ArrayList<>();
		for (int s = 8; s >= 5; s--) {
			sesiones.addAll(semana(s, 3));
		}
		sesiones.addAll(semana(4, 2)); // no llega a 3: corta
		sesiones.addAll(semana(3, 3));
		sesiones.addAll(semana(2, 3));
		sesiones.addAll(semana(1, 3));

		ResumenProgreso r = ResumenProgreso.de(sesiones, 3, HOY);

		assertThat(r.semanasDeRacha()).isEqualTo(3);
		assertThat(r.mejorRacha()).isEqualTo(4);
	}

	@Test
	void siLaSemanaEnCursoYaSeCumplioSumaALaRacha() {
		List<SesionHecha> sesiones = new ArrayList<>(semana(1, 3));
		sesiones.addAll(semana(0, 3));

		assertThat(ResumenProgreso.de(sesiones, 3, HOY.plusDays(2)).semanasDeRacha()).isEqualTo(2);
	}

	@Test
	void dosSesionesElMismoDiaCuentanUnDia() {
		List<SesionHecha> sesiones = List.of(el(LUNES.minusWeeks(1), "500"), el(LUNES.minusWeeks(1), "500"),
				el(LUNES.minusWeeks(1).plusDays(2), "0"));

		ResumenProgreso r = ResumenProgreso.de(sesiones, 3, HOY);

		assertThat(r.diasEntrenados()).isEqualTo(2);
		assertThat(r.sesiones()).isEqualTo(3);
		assertThat(r.semanasDeRacha()).isZero();
	}

	@Test
	void elPorcentajeCumplidoMiraLasSemanasAcabadasDesdeLaPrimeraYNoPasaDe100() {
		List<SesionHecha> sesiones = new ArrayList<>(semana(2, 5)); // 5 de 3: cuenta 3
		sesiones.addAll(semana(0, 1)); // la semana en curso no cuenta

		// Semanas acabadas desde la primera: 2 (3 de 3 y 0 de 3) → 50 %.
		assertThat(ResumenProgreso.de(sesiones, 3, HOY).porcentajeCumplido()).isEqualTo(50);
	}

	@Test
	void elMapaVaDeLaSemanaMasAntiguaALaActualConLosDiasDeLunesADomingo() {
		List<SesionHecha> sesiones = List.of(el(LUNES, "0"), el(LUNES.plusDays(6), "0"),
				el(LUNES.minusWeeks(11).plusDays(2), "0"), el(LUNES.minusWeeks(12), "0"));

		ResumenProgreso r = ResumenProgreso.de(sesiones, 3, HOY);

		assertThat(r.ultimasSemanas().getFirst().lunes()).isEqualTo(LUNES.minusWeeks(11));
		assertThat(r.ultimasSemanas().getFirst().dias()).containsExactly(false, false, true, false, false, false, false);
		assertThat(r.ultimasSemanas().getLast().lunes()).isEqualTo(LUNES);
		assertThat(r.ultimasSemanas().getLast().dias()).containsExactly(true, false, false, false, false, false, true);
	}
}
