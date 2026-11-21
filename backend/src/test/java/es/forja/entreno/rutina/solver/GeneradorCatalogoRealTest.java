package es.forja.entreno.rutina.solver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import es.forja.PruebaConMysql;
import es.forja.entreno.rutina.modelo.CatalogoRutina;
import es.forja.entreno.rutina.modelo.ConfiguracionRutina;
import es.forja.entreno.rutina.modelo.EjercicioRutina;
import es.forja.restricciones.filtro.EjercicioCandidato;
import es.forja.entreno.rutina.modelo.EntradaRutina;
import es.forja.entreno.rutina.modelo.PlanRutina;
import es.forja.entreno.rutina.modelo.PlanRutina.DiaPlan;
import es.forja.entreno.rutina.modelo.PlanRutina.EjercicioPlan;
import es.forja.entreno.rutina.servicio.ServicioRutina;
import es.forja.restricciones.modelo.Estado;
import es.forja.restricciones.modelo.Lado;
import es.forja.restricciones.modelo.Origen;
import es.forja.restricciones.modelo.Patron;
import es.forja.restricciones.modelo.Restriccion;
import es.forja.restricciones.modelo.TipoRestriccion;

/**
 * Calidad de los planes sobre el catálogo real sembrado por Flyway: que se puedan entrenar (fuerza e hipertrofia),
 * no solo que cumplan las restricciones. También mide el tiempo de generación (ADR 0002, «medir en la fase 1»).
 */
@PruebaConMysql
class GeneradorCatalogoRealTest {

	static final Set<String> MATERIAL_COMPLETO = Set.of("barra", "mancuerna", "maquina", "polea", "peso_corporal",
			"kettlebell", "banda", "barra_ez", "balon_medicinal", "fitball", "rodillo", "otro");
	private static final Set<String> MATERIAL_DE_CASA = Set.of("mancuerna", "peso_corporal");
	private static final Set<String> MATERIAL_DE_GIMNASIO = Set.of("barra", "maquina", "polea", "barra_ez");
	private static final int PESO_ESTIRADA_ALTO = 20;
	private static final int MARGEN_OBJETIVO = 4;
	private static final double FRACCION_MINIMA_SESION = 0.75;
	private static final String BASICO = "basico";
	/**
	 * Alarma contra regresiones grandes, no un benchmark: el objetivo de producto es < 10 s (en el portátil salen 6-9 s;
	 * falta medirlo en el VPS) y el tiempo de reloj en una JVM de tests compartida varía con la máquina.
	 */
	private static final long MILISEGUNDOS_MAXIMOS = 15_000;

	@Autowired
	private ServicioRutina servicio;

	private static EntradaRutina entrada(int dias, int minutos, Set<String> material, List<Restriccion> limitaciones) {
		return new EntradaRutina(dias, minutos, material, Set.of(), List.of(), EntradaRutina.FRECUENCIA_POR_DEFECTO,
				limitaciones);
	}

	private PlanRutina generar(EntradaRutina entrada) {
		long inicio = System.nanoTime();
		PlanRutina plan = new GeneradorRutina(ConfiguracionDePrueba.estandar()).generar(servicio.catalogo(), entrada);
		// Dato que pide el ADR 0002 («[estimación, medir en la fase 1]»): se lee en la salida de la suite.
		System.out.printf("Generación %d×%d: %d ms (%s, %d alternativas)%n", entrada.dias(), entrada.minutos(),
				(System.nanoTime() - inicio) / 1_000_000, plan.estado(), plan.alternativas().size());
		plan.dias().forEach(d -> System.out.println("  Día " + d.dia() + " (" + d.segundos() / 60 + " min): "
				+ d.ejercicios().stream().map(e -> e.nombre() + " " + e.series()).toList()));
		return plan;
	}

	private Map<String, EjercicioRutina> porId() {
		return servicio.catalogo().ejercicios().stream()
				.collect(Collectors.toMap(EjercicioRutina::idExterno, Function.identity()));
	}

	private Set<String> patronesConCompuesto(PlanRutina plan) {
		Map<String, EjercicioRutina> porId = porId();
		return plan.dias().stream().flatMap(d -> d.ejercicios().stream()).map(e -> porId.get(e.idExterno()))
				.filter(EjercicioRutina::compuesto).map(e -> e.patron().name().toLowerCase(java.util.Locale.ROOT))
				.collect(Collectors.toSet());
	}

	private static List<EjercicioPlan> elegidos(PlanRutina plan) {
		return plan.dias().stream().flatMap(d -> d.ejercicios().stream()).toList();
	}

	@Test
	void cuatroPorSesentaConTodoElMaterialEsUnPlanEntrenable() {
		PlanRutina plan = generar(entrada(4, 60, MATERIAL_COMPLETO, List.of()));

		assertThat(patronesConCompuesto(plan)).containsAll(ConfiguracionDePrueba.PATRONES_PRINCIPALES);
		ConfiguracionDePrueba.MUSCULOS_GRANDES.forEach(m -> assertThat(plan.volumen()).anySatisfy(v -> {
			assertThat(v.musculo()).isEqualTo(m);
			assertThat(v.series()).as(m).isBetween(10.0, 20.0);
			assertThat(v.series()).as(m).isBetween((double) v.objetivo() - MARGEN_OBJETIVO,
					(double) v.objetivo() + MARGEN_OBJETIVO);
		}));
		int masLarga = plan.dias().stream().mapToInt(DiaPlan::segundos).max().orElseThrow();
		plan.dias().forEach(d -> assertThat(d.segundos()).as("día " + d.dia())
				.isGreaterThanOrEqualTo((int) (FRACCION_MINIMA_SESION * masLarga)));
	}

	/**
	 * Caso real: 3×60 metía 9 series de sentadilla en un día (5 + 4 de hack). Con 2×60 y 3×45 el mínimo ya iba
	 * con holgura antes del tope, así que solo se exige en 3×60 y 4×60.
	 */
	@ParameterizedTest(name = "{0}×{1}")
	@CsvSource({ "2, 60, false", "3, 45, false", "3, 60, true", "4, 60, true" })
	void ningunPatronPasaDelTopeDeSeriesEnUnDiaYElPlanSigueSiendoBueno(int dias, int minutos, boolean llegaAlMinimo) {
		ConfiguracionRutina config = ConfiguracionDePrueba.estandar();
		Map<String, EjercicioRutina> porId = porId();
		long inicio = System.nanoTime();
		PlanRutina plan = generar(entrada(dias, minutos, MATERIAL_COMPLETO, List.of()));
		long milisegundos = (System.nanoTime() - inicio) / 1_000_000;

		assertThat(plan.estado()).isIn(PlanRutina.OPTIMO, PlanRutina.FACTIBLE);
		assertThat(milisegundos).isLessThan(MILISEGUNDOS_MAXIMOS);
		for (DiaPlan dia : plan.dias()) {
			Map<Patron, Integer> series = dia.ejercicios().stream().collect(Collectors.toMap(
					e -> porId.get(e.idExterno()).patron(), EjercicioPlan::series, Integer::sum));
			series.forEach((p, n) -> {
				if (!ModeloRutina.SIN_TOPE_DE_SERIES_POR_DIA.contains(p)) {
					assertThat(n).as("día %d, %s", dia.dia(), p)
							.isLessThanOrEqualTo(config.sesion().seriesMaximasPorPatronYDia());
				}
			});
		}
		if (llegaAlMinimo) {
			plan.volumen().stream().filter(v -> config.volumen().musculosConMinimo().contains(v.musculo()))
					.forEach(v -> assertThat(v.series()).as(v.musculo())
							.isGreaterThanOrEqualTo(config.volumen().rango(v.musculo()).minimo()));
		}
	}

	/**
	 * Caso real (revisión de Gabriel): fondos y remo salían los tres días de un 3×N. Entrenar a menudo un músculo no
	 * obliga a repetir ejercicio: el tercer día va otro de su familia. Solo se permite repetir si no hay alternativa
	 * curada en su familia con ese material (en casa solo hay un gemelo: con mancuernas).
	 */
	@ParameterizedTest(name = "{0}×{1}, en casa: {2}")
	@CsvSource({ "3, 45, false", "3, 60, false", "3, 90, false", "4, 60, false", "5, 60, false", "6, 60, false",
			"3, 60, true", "4, 60, true" })
	void ningunEjercicioSaleMasDeDosDiasALaSemana(int dias, int minutos, boolean enCasa) {
		Set<String> material = enCasa ? MATERIAL_DE_CASA : MATERIAL_COMPLETO;
		PlanRutina plan = generar(entrada(dias, minutos, material, List.of()));
		Map<String, EjercicioRutina> porId = porId();
		Map<String, Long> curadosPorFamilia = porId.values().stream().map(EjercicioRutina::candidato)
				.filter(c -> c.curado() && material.contains(c.implemento()))
				.collect(Collectors.groupingBy(EjercicioCandidato::familiaSustitucion, Collectors.counting()));

		Map<String, Long> diasPorEjercicio = plan.dias().stream().flatMap(d -> d.ejercicios().stream())
				.collect(Collectors.groupingBy(EjercicioPlan::idExterno, Collectors.counting()));
		diasPorEjercicio.forEach((id, n) -> {
			boolean hayAlternativa = curadosPorFamilia.getOrDefault(porId.get(id).candidato().familiaSustitucion(), 0L) > 1;
			if (hayAlternativa) {
				assertThat(n).as(id).isLessThanOrEqualTo(2);
			}
		});
	}

	@Test
	void tresPorCuarentaYCincoTieneLosSeisPatronesOLaTrazaExplicaLaFalta() {
		PlanRutina plan = generar(entrada(3, 45, MATERIAL_COMPLETO, List.of()));

		Set<String> presentes = patronesConCompuesto(plan);
		ConfiguracionDePrueba.PATRONES_PRINCIPALES.stream().filter(p -> !presentes.contains(p))
				.forEach(p -> assertThat(plan.holguras()).anySatisfy(h -> {
					assertThat(h.codigo()).isEqualTo(p);
					assertThat(h.tipo()).isEqualTo(BASICO);
					assertThat(h.mensaje()).startsWith("Sin ");
				}));
	}

	@Test
	void tresPorSesentaEnCasaDaUnPlanSinBarraNiMaquinas() {
		PlanRutina plan = generar(entrada(3, 60, MATERIAL_DE_CASA, List.of()));
		Map<String, EjercicioRutina> porId = porId();

		assertThat(plan.dias()).hasSize(3).allSatisfy(d -> assertThat(d.ejercicios()).isNotEmpty());
		elegidos(plan).forEach(e -> assertThat(porId.get(e.idExterno()).candidato().implemento())
				.isNotIn(MATERIAL_DE_GIMNASIO));
	}

	@Test
	void conEvitarEnSentadillaNoApareceElPatronYLaTrazaLoExplica() {
		Restriccion evitar = new Restriccion(null, Lado.AMBOS, Patron.SENTADILLA, TipoRestriccion.EVITAR, null,
				Estado.ACTUAL, Origen.USUARIO, null);

		PlanRutina plan = generar(entrada(4, 60, MATERIAL_COMPLETO, List.of(evitar)));
		Map<String, EjercicioRutina> porId = porId();

		elegidos(plan).forEach(e -> assertThat(porId.get(e.idExterno()).patron()).isNotEqualTo(Patron.SENTADILLA));
		assertThat(plan.holguras()).anySatisfy(h -> {
			assertThat(h.codigo()).isEqualTo("sentadilla");
			assertThat(h.mensaje()).isEqualTo("Sin sentadilla: tus restricciones excluyen todas las variantes.");
		});
	}

	@Test
	void dosPorSesentaYDosPorTreintaUsanHolguraYElMensajeLoDice() {
		generar(entrada(2, 60, MATERIAL_COMPLETO, List.of()));
		PlanRutina plan = generar(entrada(2, 30, MATERIAL_COMPLETO, List.of()));

		assertThat(plan.holguras()).anySatisfy(h -> assertThat(h.mensaje())
				.matches("Con 2 días y 30 min no llegas a 10 series de .+; te quedan [0-9,]+\\."));
	}

	@Test
	void subirElPesoDePosicionEstiradaEligeMasSeriesEnPosicionEstirada() {
		CatalogoRutina catalogo = servicio.catalogo();
		EntradaRutina entrada = entrada(4, 60, MATERIAL_COMPLETO, List.of());

		PlanRutina sinPeso = new GeneradorRutina(ConfiguracionDePrueba.conPesoEstirada(0)).generar(catalogo, entrada);
		PlanRutina conPeso = new GeneradorRutina(ConfiguracionDePrueba.conPesoEstirada(PESO_ESTIRADA_ALTO))
				.generar(catalogo, entrada);

		assertThat(seriesEstiradas(conPeso)).isGreaterThan(seriesEstiradas(sinPeso));
	}

	private static int seriesEstiradas(PlanRutina plan) {
		return elegidos(plan).stream().filter(EjercicioPlan::posicionEstirada).mapToInt(EjercicioPlan::series).sum();
	}

	@Test
	void elVolumenSeMarcaComoHeuristicaDeEntrenadores() {
		PlanRutina plan = generar(entrada(3, 45, MATERIAL_COMPLETO, List.of()));

		assertThat(plan.volumen()).allSatisfy(
				v -> assertThat(v.fuente()).isEqualTo("heurística de entrenadores (RP), nivel H"));
	}
}
