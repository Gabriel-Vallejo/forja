package es.forja.entreno.rutina.solver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import es.forja.entreno.rutina.modelo.ConfiguracionRutina;
import es.forja.entreno.rutina.modelo.EjercicioRutina;
import es.forja.entreno.rutina.modelo.PlanRutina;
import es.forja.entreno.rutina.modelo.PlanRutina.DiaPlan;
import es.forja.entreno.rutina.modelo.PlanRutina.EjercicioPlan;
import es.forja.entreno.rutina.solver.CasosAleatorios.Caso;
import es.forja.restricciones.filtro.FiltroRestricciones;
import es.forja.restricciones.modelo.Patron;

/**
 * Propiedades del generador (ADR 0002 y sus enmiendas) sobre {@value #CASOS} catálogos y entradas aleatorios con
 * semilla fija. Si una falla, la semilla la reproduce. Sin jqwik: su autor pide que no lo usen agentes de IA.
 *
 * <p>Cada plan se genera una vez y lo comparten todas las propiedades, para que la suite no tarde.
 */
class GeneradorPropiedadesTest {

	private static final int CASOS = 50;
	private static final int SEGUNDOS_POR_MINUTO = 60;
	private static final ConfiguracionRutina CONFIG = ConfiguracionDePrueba.estandar();
	private static final GeneradorRutina GENERADOR = new GeneradorRutina(CONFIG);
	private static final Map<Long, PlanRutina> PLANES = new ConcurrentHashMap<>();

	static Stream<Long> semillas() {
		return LongStream.range(0, CASOS).boxed();
	}

	private static PlanRutina plan(long semilla) {
		return PLANES.computeIfAbsent(semilla, s -> {
			Caso caso = CasosAleatorios.caso(s);
			return GENERADOR.generar(caso.catalogo(), caso.entrada());
		});
	}

	private static Stream<EjercicioPlan> elegidos(PlanRutina plan) {
		return plan.dias().stream().flatMap(d -> d.ejercicios().stream());
	}

	@ParameterizedTest(name = "semilla {0}")
	@MethodSource("semillas")
	void siempreDevuelveUnPlan(long semilla) {
		PlanRutina plan = plan(semilla);

		assertThat(plan.estado()).isIn(PlanRutina.OPTIMO, PlanRutina.FACTIBLE);
		assertThat(plan.dias()).hasSize(CasosAleatorios.caso(semilla).entrada().dias());
	}

	@ParameterizedTest(name = "semilla {0}")
	@MethodSource("semillas")
	void nuncaSePasaLaDuracionPorSesion(long semilla) {
		Caso caso = CasosAleatorios.caso(semilla);
		ConfiguracionRutina.Tiempos t = CONFIG.tiempos();

		for (DiaPlan dia : plan(semilla).dias()) {
			int segundos = dia.ejercicios().stream().mapToInt(e -> e.series()
					* (t.segundosPorSerie() + t.segundosDeDescanso()) + t.segundosDeTransicion()).sum();
			assertThat(segundos).isLessThanOrEqualTo(caso.entrada().minutos() * SEGUNDOS_POR_MINUTO);
			assertThat(dia.segundos()).isEqualTo(segundos);
		}
	}

	@ParameterizedTest(name = "semilla {0}")
	@MethodSource("semillas")
	void nuncaSePasaElMaximoDeSeriesPorMusculo(long semilla) {
		Map<String, EjercicioRutina> porId = CasosAleatorios.caso(semilla).porId();
		Map<String, Integer> dobles = new HashMap<>();

		elegidos(plan(semilla)).forEach(e -> porId.get(e.idExterno()).musculos()
				.forEach((m, peso) -> dobles.merge(m, peso * e.series(), Integer::sum)));

		dobles.forEach((m, doble) -> assertThat(doble).as(m)
				.isLessThanOrEqualTo(2 * CONFIG.volumen().rango(m).maximo()));
	}

	@ParameterizedTest(name = "semilla {0}")
	@MethodSource("semillas")
	void nuncaMasDeNEjerciciosPorPatronYDia(long semilla) {
		Map<String, EjercicioRutina> porId = CasosAleatorios.caso(semilla).porId();

		for (DiaPlan dia : plan(semilla).dias()) {
			Map<Patron, Long> porPatron = new HashMap<>();
			dia.ejercicios().forEach(e -> porPatron.merge(porId.get(e.idExterno()).patron(), 1L, Long::sum));
			porPatron.values().forEach(n -> assertThat(n).isLessThanOrEqualTo(CONFIG.sesion().maximoPorPatron()));
			assertThat(dia.ejercicios().size()).isLessThanOrEqualTo(CONFIG.sesion().ejerciciosMaximos());
		}
	}

	@ParameterizedTest(name = "semilla {0}")
	@MethodSource("semillas")
	void ningunBasicoPesadoEnDiasSeguidos(long semilla) {
		Map<String, EjercicioRutina> porId = CasosAleatorios.caso(semilla).porId();
		List<DiaPlan> dias = plan(semilla).dias();

		for (int d = 0; d + 1 < dias.size(); d++) {
			List<String> manana = dias.get(d + 1).ejercicios().stream().map(EjercicioPlan::idExterno).toList();
			dias.get(d).ejercicios().stream().filter(e -> porId.get(e.idExterno()).basicoPesado())
					.forEach(e -> assertThat(manana).doesNotContain(e.idExterno()));
		}
	}

	@ParameterizedTest(name = "semilla {0}")
	@MethodSource("semillas")
	void conEvitarNingunEjercicioDeEsePatronYConLimitarSoloVariantesPermitidas(long semilla) {
		Caso caso = CasosAleatorios.caso(semilla);
		Map<String, EjercicioRutina> porId = caso.porId();

		elegidos(plan(semilla)).map(e -> porId.get(e.idExterno())).forEach(e -> {
			assertThat(e.patron()).isNotEqualTo(caso.patronEvitado());
			assertThat(FiltroRestricciones.cumple(e.candidato(), caso.entrada().restricciones())).isTrue();
		});
	}

	@ParameterizedTest(name = "semilla {0}")
	@MethodSource("semillas")
	void soloMaterialDisponibleNingunExcluidoYSoloCuradosAptos(long semilla) {
		Caso caso = CasosAleatorios.caso(semilla);
		Map<String, EjercicioRutina> porId = caso.porId();

		elegidos(plan(semilla)).map(e -> porId.get(e.idExterno())).forEach(e -> {
			assertThat(caso.entrada().material()).contains(e.candidato().implemento());
			assertThat(caso.entrada().exclusiones()).doesNotContain(e.idExterno());
			assertThat(e.candidato().curado()).isTrue();
			assertThat(e.aptoFuerza()).isTrue();
		});
	}

	@ParameterizedTest(name = "semilla {0}")
	@MethodSource("semillas")
	void mismaEntradaYMismaSemillaDanElMismoPlan(long semilla) {
		Caso caso = CasosAleatorios.caso(semilla);

		assertThat(new GeneradorRutina(CONFIG).generar(caso.catalogo(), caso.entrada())).isEqualTo(plan(semilla));
	}

	@ParameterizedTest(name = "semilla {0}")
	@MethodSource("semillas")
	void laTrazaCuadraConElPlan(long semilla) {
		PlanRutina plan = plan(semilla);
		long elegidosDistintos = elegidos(plan).map(EjercicioPlan::idExterno).distinct().count();

		assertThat(plan.contribuciones()).hasSize((int) elegidosDistintos);
		assertThat(plan.alternativas()).hasSizeLessThanOrEqualTo(GeneradorRutina.MAXIMO_RESOLUCIONES_ALTERNATIVAS);
		plan.holguras().forEach(h -> assertThat(h.mensaje()).startsWith("Con "));
		plan.descartes().forEach(d -> assertThat(d.motivo()).isNotBlank());
		assertThat(plan.resumen()).contains(PlanRutina.OPTIMO.equals(plan.estado()) ? "el mejor plan" : "un buen plan");
	}
}
