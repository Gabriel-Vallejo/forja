package es.forja.entreno.plan.modelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import es.forja.entreno.plan.modelo.Transicion.Evidencia;

/** Una prueba por cada transición de RF-26, el límite de Epley y la independencia del orden de llegada. */
class MaquinaProgresionTest {

	private static final Instant DIA_0 = Instant.parse("2026-01-01T10:00:00Z");
	private static final ParametrosProgresion PARAMETROS = new ParametrosProgresion(
			new ParametrosProgresion.Deload(3, 3, new BigDecimal("1.0"), new BigDecimal("0.9"),
					new BigDecimal("0.5"), new BigDecimal("0.5")),
			12, 20);
	/** 3 series, 6-10 repeticiones, RIR 2 y +2,5 kg. */
	private static final ReglaEjercicio CON_CARGA = new ReglaEjercicio(3, 6, 10, 2, new BigDecimal("2.5"), null);

	private static SesionEjercicio sesion(int dia, String peso, int repeticiones, Integer rir) {
		return sesion(dia, peso, repeticiones, rir, 3);
	}

	private static SesionEjercicio sesion(int dia, String peso, int repeticiones, Integer rir, int series) {
		List<SerieTrabajo> trabajo = new ArrayList<>();
		for (int i = 0; i < series; i++) {
			trabajo.add(new SerieTrabajo(new BigDecimal(peso), repeticiones, rir));
		}
		return new SesionEjercicio(UUID.nameUUIDFromBytes(("s" + dia + peso + repeticiones).getBytes()),
				DIA_0.plus(Duration.ofDays(dia)), trabajo);
	}

	private static ResultadoProgresion conCarga(SesionEjercicio... sesiones) {
		return MaquinaProgresion.evaluar(List.of(sesiones), CON_CARGA, PARAMETROS);
	}

	private static ResultadoProgresion evaluar(ReglaEjercicio regla, SesionEjercicio... sesiones) {
		return MaquinaProgresion.evaluar(List.of(sesiones), regla, PARAMETROS);
	}

	/** Un pico de e1RM y luego tres sesiones sin superarlo en las últimas tres semanas. */
	private static List<SesionEjercicio> hastaElDeload() {
		return List.of(sesion(0, "60", 10, 2), sesion(30, "60", 8, 2), sesion(35, "60", 8, 2), sesion(40, "60", 8, 2));
	}

	private static ResultadoProgresion evaluar(List<SesionEjercicio> base, SesionEjercicio... mas) {
		List<SesionEjercicio> todas = new ArrayList<>(base);
		todas.addAll(List.of(mas));
		return MaquinaProgresion.evaluar(todas, CON_CARGA, PARAMETROS);
	}

	// ---- transiciones de la función de transición ----

	@Test
	void acumulandoASubirCargaSiSeCumpleLaRegla() {
		assertThat(Transicion.siguiente(Estado.ACUMULANDO, new Evidencia(true, false, false)))
				.isEqualTo(Estado.SUBIR_CARGA);
	}

	@Test
	void subirCargaAAcumulandoSiLaSiguienteSesionNoCumple() {
		assertThat(Transicion.siguiente(Estado.SUBIR_CARGA, new Evidencia(false, false, false)))
				.isEqualTo(Estado.ACUMULANDO);
	}

	@Test
	void acumulandoAEstancadoSiElE1rmNoMejora() {
		assertThat(Transicion.siguiente(Estado.ACUMULANDO, new Evidencia(false, true, false)))
				.isEqualTo(Estado.ESTANCADO);
		assertThat(Transicion.siguiente(Estado.SUBIR_CARGA, new Evidencia(true, true, false)))
				.isEqualTo(Estado.ESTANCADO);
	}

	@Test
	void estancadoSiempreSeResuelveEnDeload() {
		assertThat(Transicion.resolver(Estado.ESTANCADO)).isEqualTo(Estado.DELOAD);
		assertThat(Transicion.resolver(Estado.ACUMULANDO)).isEqualTo(Estado.ACUMULANDO);
	}

	@Test
	void deloadAAcumulandoAlRegistrarLaSesionDeDescarga() {
		assertThat(Transicion.siguiente(Estado.DELOAD, new Evidencia(true, true, true))).isEqualTo(Estado.ACUMULANDO);
		assertThat(Transicion.siguiente(Estado.ESTANCADO, new Evidencia(false, false, true)))
				.isEqualTo(Estado.ACUMULANDO);
	}

	@Test
	void unDeloadIgnoradoSeEvaluaComoUnaSesionNormal() {
		assertThat(Transicion.siguiente(Estado.DELOAD, new Evidencia(false, false, false)))
				.isEqualTo(Estado.ACUMULANDO);
		assertThat(Transicion.siguiente(Estado.DELOAD, new Evidencia(true, false, false)))
				.isEqualTo(Estado.SUBIR_CARGA);
		assertThat(Transicion.siguiente(Estado.DELOAD, new Evidencia(false, true, false)))
				.isEqualTo(Estado.ESTANCADO);
	}

	// ---- la función completa sobre un historial ----

	@Test
	void sinSesionesSeEmpiezaAcumulandoConLaCargaInicialDelPlan() {
		ReglaEjercicio conInicial = new ReglaEjercicio(3, 6, 10, 2, new BigDecimal("2.5"), new BigDecimal("40"));

		ResultadoProgresion r = MaquinaProgresion.evaluar(List.of(), conInicial, PARAMETROS);

		assertThat(r.estado()).isEqualTo(Estado.ACUMULANDO);
		assertThat(r.cargaKg()).isEqualByComparingTo("40");
		assertThat(r.series()).isEqualTo(3);
		assertThat(r.historial()).isEmpty();
		assertThat(r.motivo()).isNotBlank();
	}

	@Test
	void cumplirLaReglaSubeElIncremento() {
		ResultadoProgresion r = evaluar(CON_CARGA, sesion(0, "50", 10, 2));

		assertThat(r.estado()).isEqualTo(Estado.SUBIR_CARGA);
		assertThat(r.cargaKg()).isEqualByComparingTo("52.5");
		assertThat(r.motivo()).contains("52.5");
	}

	@Test
	void trasSubirLaCargaSiNoSeCumpleSeRepite() {
		ResultadoProgresion r = evaluar(CON_CARGA, sesion(0, "50", 10, 2), sesion(3, "52.5", 8, 2));

		assertThat(r.estado()).isEqualTo(Estado.ACUMULANDO);
		assertThat(r.cargaKg()).isEqualByComparingTo("52.5");
		assertThat(r.historial()).hasSize(2);
	}

	@Test
	void trasEstancarseLaSiguienteSesionEsUnDeloadAl90PorCientoYLaMitadDeSeries() {
		ResultadoProgresion r = evaluar(hastaElDeload());

		assertThat(r.estado()).isEqualTo(Estado.DELOAD);
		assertThat(r.cargaKg()).isEqualByComparingTo("54");
		assertThat(r.series()).isEqualTo(2);
		assertThat(r.motivo()).contains("3 semanas sin superar tu mejor marca estimada")
				.contains("deload al 90 %").contains("la mitad de series");
	}

	@Test
	void lasSeriesDelDeloadSeRedondeanHaciaArriba() {
		ReglaEjercicio cinco = new ReglaEjercicio(5, 6, 10, 2, new BigDecimal("2.5"), null);

		ResultadoProgresion r = MaquinaProgresion.evaluar(hastaElDeload(), cinco, PARAMETROS);

		assertThat(r.series()).isEqualTo(3);
	}

	@Test
	void laCargaDelDeloadSeRedondeaAlMedioKilo() {
		ResultadoProgresion r = evaluar(List.of(sesion(0, "62.5", 10, 2), sesion(30, "62.5", 8, 2),
				sesion(35, "62.5", 8, 2), sesion(40, "62.5", 8, 2)));

		assertThat(r.estado()).isEqualTo(Estado.DELOAD);
		assertThat(r.cargaKg()).isEqualByComparingTo("56.5");
	}

	@Test
	void alRegistrarLaSesionDeDeloadSeVuelveAAcumularConLaCargaDeAntes() {
		ResultadoProgresion r = evaluar(hastaElDeload(), sesion(47, "54", 10, 2, 2));

		assertThat(r.estado()).isEqualTo(Estado.ACUMULANDO);
		assertThat(r.cargaKg()).isEqualByComparingTo("60");
		assertThat(r.series()).isEqualTo(3);
	}

	@Test
	void unaSesionPesadaDespuesDelAvisoNoCuentaComoDeload() {
		ResultadoProgresion r = evaluar(hastaElDeload(), sesion(47, "60", 8, 2));

		assertThat(r.estado()).isEqualTo(Estado.DELOAD);
	}

	@Test
	void trasUnDeloadLaVentanaDeEstancamientoEmpiezaDeNuevo() {
		List<SesionEjercicio> tras = new ArrayList<>(hastaElDeload());
		tras.add(sesion(47, "54", 10, 2, 2));
		tras.add(sesion(50, "60", 8, 2));
		tras.add(sesion(54, "60", 8, 2));

		// Solo hay dos sesiones desde el deload: no se puede hablar de estancamiento todavía.
		assertThat(MaquinaProgresion.evaluar(tras, CON_CARGA, PARAMETROS).estado()).isEqualTo(Estado.ACUMULANDO);

		tras.add(sesion(58, "60", 8, 2));
		assertThat(MaquinaProgresion.evaluar(tras, CON_CARGA, PARAMETROS).estado()).isEqualTo(Estado.DELOAD);
	}

	// ---- estancamiento ----

	@Test
	void conMenosDeTresSesionesEnLaVentanaNoHayEstancamiento() {
		ResultadoProgresion r = conCarga(sesion(0, "60", 10, 2), sesion(30, "60", 8, 2), sesion(35, "60", 8, 2));

		assertThat(r.estado()).isNotEqualTo(Estado.DELOAD);
	}

	@Test
	void sinNingunaSesionAnteriorALaVentanaNoHayConQuienComparar() {
		ResultadoProgresion r = conCarga(sesion(0, "60", 8, 2), sesion(5, "60", 8, 2), sesion(10, "60", 8, 2));

		assertThat(r.estado()).isEqualTo(Estado.ACUMULANDO);
	}

	@Test
	void mejorarMasDeUnoPorCientoEvitaElEstancamiento() {
		// Antes 100 kg x 6 (e1RM 120); en la ventana 101,25 kg x 6 (121,5 > 121,2).
		ResultadoProgresion r = conCarga(sesion(0, "100", 6, 3), sesion(30, "101.25", 6, 3),
				sesion(35, "101.25", 6, 3), sesion(40, "101.25", 6, 3));

		assertThat(r.estado()).isEqualTo(Estado.ACUMULANDO);
	}

	@Test
	void mejorarExactamenteUnoPorCientoSigueSiendoEstancamiento() {
		// 101 kg x 6 = 121,2 = 120 x 1,01: no lo supera.
		ResultadoProgresion r = conCarga(sesion(0, "100", 6, 3), sesion(30, "101", 6, 3), sesion(35, "101", 6, 3),
				sesion(40, "101", 6, 3));

		assertThat(r.estado()).isEqualTo(Estado.DELOAD);
	}

	@Test
	void lasSesionesAnterioresALaVentanaNoCuentanComoVentana() {
		// Cuatro sesiones, pero solo dos en las últimas tres semanas.
		ResultadoProgresion r = conCarga(sesion(0, "60", 10, 2), sesion(5, "60", 8, 2), sesion(10, "60", 8, 2),
				sesion(40, "60", 8, 2), sesion(41, "60", 8, 2));

		assertThat(r.estado()).isNotEqualTo(Estado.DELOAD);
	}

	// ---- Epley ----

	@Test
	void epleyDaPesoPorUnoMasRepeticionesEntreTreinta() {
		assertThat(E1rm.deSerie(new SerieTrabajo(new BigDecimal("50"), 12, 1), 12)).hasValueSatisfying(
				e -> assertThat(e).isEqualByComparingTo("70.00"));
		assertThat(E1rm.deSerie(new SerieTrabajo(new BigDecimal("100"), 1, 0), 12)).hasValueSatisfying(
				e -> assertThat(e).isEqualByComparingTo("103.33"));
	}

	@Test
	void porEncimaDeDoceRepeticionesLaSerieNoCuentaParaElE1rm() {
		assertThat(E1rm.deSerie(new SerieTrabajo(new BigDecimal("50"), 13, 1), 12)).isEmpty();
		assertThat(E1rm.deSerie(new SerieTrabajo(new BigDecimal("50"), 0, 0), 12)).isEmpty();
		assertThat(E1rm.deSerie(new SerieTrabajo(BigDecimal.ZERO, 8, 2), 12)).isEmpty();
	}

	@Test
	void elE1rmDeLaSesionEsElMaximoDeLasSeriesQueCuentan() {
		List<SerieTrabajo> series = List.of(new SerieTrabajo(new BigDecimal("100"), 15, 0),
				new SerieTrabajo(new BigDecimal("60"), 10, 2), new SerieTrabajo(new BigDecimal("50"), 8, 2));

		assertThat(E1rm.deSesion(series, 12)).hasValueSatisfying(e -> assertThat(e).isEqualByComparingTo("80.00"));
		assertThat(E1rm.deSesion(List.of(new SerieTrabajo(new BigDecimal("100"), 15, 0)), 12)).isEmpty();
	}

	@Test
	void elHistorialMarcaSinE1rmLasSesionesDeMasDeDoceRepeticiones() {
		ResultadoProgresion r = evaluar(CON_CARGA, sesion(0, "40", 15, 2), sesion(3, "60", 8, 2));

		assertThat(r.historial()).hasSize(2);
		assertThat(r.historial().get(0).e1rm()).isNull();
		assertThat(r.historial().get(1).e1rm()).isEqualByComparingTo("76.00");
		assertThat(r.historial().get(0).realizadaEn()).isBefore(r.historial().get(1).realizadaEn());
	}

	// ---- peso corporal ----

	private static final ReglaEjercicio CORPORAL = new ReglaEjercicio(3, 8, 12, 2, BigDecimal.ZERO, null);

	@Test
	void conPesoCorporalCumplirLaReglaSubeUnaRepeticionElRango() {
		ResultadoProgresion r = MaquinaProgresion.evaluar(List.of(sesion(0, "0", 12, 2)), CORPORAL, PARAMETROS);

		assertThat(r.estado()).isEqualTo(Estado.SUBIR_CARGA);
		assertThat(r.repeticionesMin()).isEqualTo(9);
		assertThat(r.repeticionesMax()).isEqualTo(13);
		assertThat(r.cargaKg()).isEqualByComparingTo("0");
		assertThat(r.motivo()).contains("13");
	}

	@Test
	void conPesoCorporalElRangoSigueSubiendoSesionTrasSesion() {
		ResultadoProgresion r = MaquinaProgresion.evaluar(List.of(sesion(0, "0", 12, 2), sesion(3, "0", 13, 2)),
				CORPORAL, PARAMETROS);

		assertThat(r.repeticionesMax()).isEqualTo(14);
		assertThat(r.repeticionesMin()).isEqualTo(10);
	}

	@Test
	void conPesoCorporalSiNoSeCumpleElRangoNoCambia() {
		ResultadoProgresion r = MaquinaProgresion.evaluar(List.of(sesion(0, "0", 11, 2)), CORPORAL, PARAMETROS);

		assertThat(r.estado()).isEqualTo(Estado.ACUMULANDO);
		assertThat(r.repeticionesMax()).isEqualTo(12);
	}

	@Test
	void conPesoCorporalAlLlegarAlTopeElMotivoSugiereLastre() {
		ReglaEjercicio enElTope = new ReglaEjercicio(3, 16, 20, 2, BigDecimal.ZERO, null);

		ResultadoProgresion r = MaquinaProgresion.evaluar(List.of(sesion(0, "0", 20, 2)), enElTope, PARAMETROS);

		assertThat(r.repeticionesMax()).isEqualTo(20);
		assertThat(r.repeticionesMin()).isEqualTo(16);
		assertThat(r.motivo()).contains("lastre");
	}

	@Test
	void conPesoCorporalElRangoNuncaPasaDelTope() {
		ReglaEjercicio casi = new ReglaEjercicio(3, 15, 19, 2, BigDecimal.ZERO, null);

		ResultadoProgresion r = MaquinaProgresion.evaluar(List.of(sesion(0, "0", 19, 2), sesion(3, "0", 20, 2)),
				casi, PARAMETROS);

		assertThat(r.repeticionesMax()).isEqualTo(20);
		assertThat(r.motivo()).contains("lastre");
	}

	// ---- propiedad: el orden de llegada no importa ----

	@ParameterizedTest
	@ValueSource(longs = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 })
	void elResultadoNoDependeDelOrdenEnQueSePasanLasSesiones(long semilla) {
		Random azar = new Random(semilla);
		for (int caso = 0; caso < 60; caso++) {
			List<SesionEjercicio> sesiones = sesionesAlAzar(azar);
			ReglaEjercicio regla = azar.nextInt(4) == 0 ? CORPORAL : CON_CARGA;
			ResultadoProgresion esperado = MaquinaProgresion.evaluar(sesiones, regla, PARAMETROS);

			for (int barajado = 0; barajado < 4; barajado++) {
				List<SesionEjercicio> otras = new ArrayList<>(sesiones);
				Collections.shuffle(otras, azar);
				assertThat(MaquinaProgresion.evaluar(otras, regla, PARAMETROS)).isEqualTo(esperado);
			}
			assertThat(esperado.historial()).extracting(PuntoE1rm::realizadaEn).isSorted();
		}
	}

	private static List<SesionEjercicio> sesionesAlAzar(Random azar) {
		List<SesionEjercicio> sesiones = new ArrayList<>();
		int cuantas = azar.nextInt(11);
		for (int i = 0; i < cuantas; i++) {
			// Pocos días distintos, para que haya empates de fecha y se vea el desempate por uuid.
			int dia = azar.nextInt(12) * 5;
			List<SerieTrabajo> series = new ArrayList<>();
			for (int s = 1 + azar.nextInt(4); s > 0; s--) {
				series.add(new SerieTrabajo(BigDecimal.valueOf(azar.nextInt(30) * 2.5), azar.nextInt(16),
						azar.nextInt(5) == 0 ? null : azar.nextInt(4)));
			}
			sesiones.add(new SesionEjercicio(new UUID(azar.nextLong(), azar.nextLong()),
					DIA_0.plus(Duration.ofDays(dia)), series));
		}
		return sesiones;
	}

	@Test
	void cadaSesionSeJuzgaConLasSeriesQueLePediaSuPlanNoConLasDelActual() {
		// El plan pasó de 3 a 4 series; la sesión se hizo con el de 3 y las completó todas.
		ReglaEjercicio ahoraCuatro = new ReglaEjercicio(4, 6, 10, 2, new BigDecimal("2.5"), null);
		SesionEjercicio conSuPlan = sesion(0, "60", 10, 2).conSeriesPrevistas(3);
		SesionEjercicio sinSaberlo = sesion(0, "60", 10, 2);

		assertThat(evaluar(ahoraCuatro, conSuPlan).cargaKg()).isEqualByComparingTo("62.5");
		assertThat(evaluar(ahoraCuatro, sinSaberlo).cargaKg()).isEqualByComparingTo("60");
	}
}
