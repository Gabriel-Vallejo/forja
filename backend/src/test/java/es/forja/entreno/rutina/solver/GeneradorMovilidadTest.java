package es.forja.entreno.rutina.solver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import es.forja.PruebaConMysql;
import es.forja.entreno.movilidad.modelo.TestMovilidad;
import es.forja.entreno.movilidad.modelo.Tramo;
import es.forja.entreno.rutina.modelo.EjercicioRutina;
import es.forja.entreno.rutina.modelo.EntradaRutina;
import es.forja.entreno.rutina.modelo.PlanRutina;
import es.forja.entreno.rutina.modelo.PlanRutina.EjercicioPlan;
import es.forja.entreno.rutina.servicio.ServicioRutina;

/** RF-09: el tramo de cada test de movilidad cambia variantes concretas del plan, sin hacerlo inviable. */
@PruebaConMysql
class GeneradorMovilidadTest {

	private static final long MILISEGUNDOS_MAXIMOS = 15_000;
	private static final Set<String> SENTADILLAS_CON_BARRA = Set.of("Barbell_Squat", "Front_Barbell_Squat");
	private static final Set<String> PRESS_INCLINADOS = Set.of("Barbell_Incline_Bench_Press_-_Medium_Grip",
			"Incline_Dumbbell_Press");
	private static final Set<String> PESO_MUERTO_CERRADO = Set.of("Barbell_Deadlift", "Trap_Bar_Deadlift");
	private static final Set<String> PRESS_DE_PIE = Set.of("Standing_Military_Press", "Dumbbell_Shoulder_Press");

	@Autowired
	private ServicioRutina servicio;

	private PlanRutina generar(int dias, int minutos, Map<TestMovilidad, Tramo> movilidad) {
		EntradaRutina entrada = new EntradaRutina(dias, minutos, GeneradorCatalogoRealTest.MATERIAL_COMPLETO,
				Set.of(), List.of(), EntradaRutina.FRECUENCIA_POR_DEFECTO, List.of(), movilidad);
		PlanRutina plan = new GeneradorRutina(ConfiguracionDePrueba.estandar()).generar(servicio.catalogo(), entrada);
		System.out.println("Movilidad " + movilidad + " " + dias + "×" + minutos + ": " + plan.dias().stream()
				.map(d -> d.ejercicios().stream().map(e -> e.idExterno() + " " + e.series()).toList()).toList());
		return plan;
	}

	private static List<EjercicioPlan> elegidos(PlanRutina plan) {
		return plan.dias().stream().flatMap(d -> d.ejercicios().stream()).toList();
	}

	private static Set<String> ids(PlanRutina plan) {
		return elegidos(plan).stream().map(EjercicioPlan::idExterno).collect(Collectors.toSet());
	}

	private static int series(PlanRutina plan, Set<String> ids) {
		return elegidos(plan).stream().filter(e -> ids.contains(e.idExterno())).mapToInt(EjercicioPlan::series).sum();
	}

	@Test
	void conTobilloLimitadoSaleLaGobletEnLugarDeLaSentadillaConBarraYLoExplica() {
		PlanRutina plan = generar(4, 60, Map.of(TestMovilidad.TOBILLO, Tramo.LIMITADO));

		assertThat(ids(plan)).contains("Goblet_Squat").doesNotContainAnyElementsOf(SENTADILLAS_CON_BARRA);
		assertThat(elegidos(plan)).filteredOn(e -> e.idExterno().equals("Goblet_Squat")).allSatisfy(
				e -> assertThat(e.anotaciones()).anySatisfy(a -> assertThat(a).startsWith(
						"Elegido por tu tramo de movilidad de tobillo (limitado)")));
		assertThat(plan.contribuciones()).filteredOn(c -> c.idExterno().equals("Goblet_Squat"))
				.allSatisfy(c -> assertThat(c.terminos().get("movilidad")).isPositive());
	}

	@Test
	void conHombroLimitadoSaleUnPressInclinadoYBajaElPressDePie() {
		PlanRutina base = generar(4, 60, Map.of());
		PlanRutina plan = generar(4, 60, Map.of(TestMovilidad.HOMBRO, Tramo.LIMITADO));

		assertThat(ids(plan)).containsAnyElementsOf(PRESS_INCLINADOS).doesNotContainAnyElementsOf(PRESS_DE_PIE);
		assertThat(series(plan, PRESS_INCLINADOS)).isGreaterThan(series(base, PRESS_INCLINADOS));
	}

	@Test
	void conCaderaLimitadaSaleUnPesoMuertoDePosturaCerradaYNoElSumo() {
		PlanRutina base = generar(4, 60, Map.of());
		PlanRutina plan = generar(4, 60, Map.of(TestMovilidad.CADERA, Tramo.LIMITADO));

		assertThat(ids(base)).doesNotContainAnyElementsOf(PESO_MUERTO_CERRADO);
		assertThat(ids(plan)).containsAnyElementsOf(PESO_MUERTO_CERRADO).doesNotContain("Sumo_Deadlift");
		assertThat(elegidos(plan)).filteredOn(e -> PESO_MUERTO_CERRADO.contains(e.idExterno())).allSatisfy(
				e -> assertThat(e.anotaciones()).anySatisfy(a -> assertThat(a).contains("movilidad de cadera")));
	}

	@Test
	void conTodoBuenoNoSeFuerzaNadaYElPlanEsElDeSinTests() {
		PlanRutina sinTests = generar(4, 60, Map.of());
		PlanRutina bueno = generar(4, 60,
				Map.of(TestMovilidad.TOBILLO, Tramo.BUENO, TestMovilidad.CADERA, Tramo.BUENO, TestMovilidad.HOMBRO,
						Tramo.BUENO));

		assertThat(bueno).isEqualTo(sinTests);
		assertThat(elegidos(bueno)).allSatisfy(e -> assertThat(e.anotaciones()).noneMatch(a -> a.contains("movilidad")));
	}

	@ParameterizedTest(name = "{0}×{1}")
	@CsvSource({ "2, 60", "3, 45", "3, 60", "4, 60" })
	void conLosTresLimitadosElPlanSigueSiendoFactibleYRapido(int dias, int minutos) {
		long inicio = System.nanoTime();
		PlanRutina plan = generar(dias, minutos, Map.of(TestMovilidad.TOBILLO, Tramo.LIMITADO, TestMovilidad.CADERA,
				Tramo.LIMITADO, TestMovilidad.HOMBRO, Tramo.LIMITADO));
		long milisegundos = (System.nanoTime() - inicio) / 1_000_000;

		assertThat(plan.estado()).isIn(PlanRutina.OPTIMO, PlanRutina.FACTIBLE);
		assertThat(plan.dias()).allSatisfy(d -> assertThat(d.ejercicios()).isNotEmpty());
		assertThat(milisegundos).isLessThan(MILISEGUNDOS_MAXIMOS);
	}

	@Test
	void lasReglasSoloCitanEjerciciosDelCatalogoCurado() {
		Set<String> curados = servicio.catalogo().ejercicios().stream().map(EjercicioRutina::idExterno)
				.collect(Collectors.toSet());

		assertThat(curados).containsAll(ReglasMovilidad.idsCitados());
	}
}
