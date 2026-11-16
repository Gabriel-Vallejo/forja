package es.forja.entreno.rutina.solver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import es.forja.PruebaConMysql;
import es.forja.entreno.rutina.modelo.CatalogoRutina;
import es.forja.entreno.rutina.modelo.EjercicioRutina;
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
