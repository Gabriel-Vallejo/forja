package es.forja.entreno.rutina.solver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import es.forja.PruebaConMysql;
import es.forja.entreno.movilidad.modelo.TestMovilidad;
import es.forja.entreno.movilidad.modelo.Tramo;
import es.forja.entreno.rutina.modelo.ConfiguracionRutina;
import es.forja.entreno.rutina.modelo.EntradaRutina;
import es.forja.entreno.rutina.modelo.EntradaRutina.Partida;
import es.forja.entreno.rutina.modelo.PlanRutina;
import es.forja.entreno.rutina.servicio.ServicioRutina;
import es.forja.restricciones.modelo.Estado;
import es.forja.restricciones.modelo.Lado;
import es.forja.restricciones.modelo.Origen;
import es.forja.restricciones.modelo.Region;
import es.forja.restricciones.modelo.Restriccion;
import es.forja.restricciones.modelo.TipoRestriccion;

/**
 * Los ajustes del plan parten del plan actual (ADR 0002): un ajuste pequeño no reorganiza medio plan. Caso real del
 * emulador: 4×60, gimnasio completo, rodilla «sin carga axial» y hombro limitado; «una serie menos» de press
 * inclinado con barra metía 5 ejercicios, sacaba 2 y cambiaba las series de 8.
 */
@PruebaConMysql
class GeneradorAjustesTest {

	private static final String PRESS_INCLINADO = "Barbell_Incline_Bench_Press_-_Medium_Grip";
	private static final long MILISEGUNDOS_MAXIMOS = 15_000;
	private static final ConfiguracionRutina CONFIG = ConfiguracionDePrueba.estandar();
	private static final GeneradorRutina GENERADOR = new GeneradorRutina(CONFIG);
	private static final List<Restriccion> RODILLA_SIN_CARGA_AXIAL = List.of(new Restriccion(Region.RODILLA,
			Lado.AMBOS, null, TipoRestriccion.SIN_CARGA_AXIAL, null, Estado.ACTUAL, Origen.USUARIO, null));
	private static final Map<TestMovilidad, Tramo> HOMBRO_LIMITADO = Map.of(TestMovilidad.HOMBRO, Tramo.LIMITADO);

	@Autowired
	private ServicioRutina servicio;

	private PlanRutina base;

	/** Qué cambia de una semana a otra, por {@code idExterno}: lo que entra, lo que sale y lo que cambia de series. */
	record Cambios(Set<String> entran, Set<String> salen, Set<String> cambian) {

		static Cambios entre(PlanRutina antes, PlanRutina despues) {
			Map<String, Integer> a = seriesSemana(antes);
			Map<String, Integer> d = seriesSemana(despues);
			Set<String> entran = new TreeSet<>(d.keySet());
			entran.removeAll(a.keySet());
			Set<String> salen = new TreeSet<>(a.keySet());
			salen.removeAll(d.keySet());
			Set<String> cambian = new TreeSet<>();
			a.forEach((id, s) -> {
				if (d.containsKey(id) && !d.get(id).equals(s)) {
					cambian.add(id);
				}
			});
			return new Cambios(entran, salen, cambian);
		}

		/** Cambios de series sin contar el del ejercicio ajustado. */
		Set<String> compensaciones(String ajustado) {
			Set<String> otras = new TreeSet<>(cambian);
			otras.remove(ajustado);
			return otras;
		}
	}

	@BeforeEach
	void planBase() {
		base = generar(entrada(Map.of(), Set.of(), List.of(), Partida.NINGUNA));
	}

	private static EntradaRutina entrada(Map<String, Integer> series, Set<String> exclusiones,
			List<String> prioritarios, Partida partida) {
		return new EntradaRutina(4, 60, GeneradorCatalogoRealTest.MATERIAL_COMPLETO, exclusiones, prioritarios,
				EntradaRutina.FRECUENCIA_POR_DEFECTO, RODILLA_SIN_CARGA_AXIAL, HOMBRO_LIMITADO, series, Set.of(),
				partida);
	}

	/** Lo que hace {@code ServicioAjustes}: el ajuste en las preferencias y el plan actual como partida. */
	private PlanRutina ajustar(Map<String, Integer> series, Set<String> exclusiones, List<String> prioritarios) {
		int libres = prioritarios.isEmpty() ? 0 : CONFIG.pesos().cambiosLibresEnfocar();
		return generar(entrada(series, exclusiones, prioritarios, new Partida(seriesPorDia(base), libres)));
	}

	private PlanRutina generar(EntradaRutina entrada) {
		long inicio = System.nanoTime();
		PlanRutina plan = GENERADOR.generar(servicio.catalogo(), entrada);
		long milisegundos = (System.nanoTime() - inicio) / 1_000_000;
		System.out.println("Ajuste en " + milisegundos + " ms: " + plan.dias().stream()
				.map(d -> d.ejercicios().stream().map(e -> e.idExterno() + " " + e.series()).toList()).toList());
		assertThat(milisegundos).isLessThan(MILISEGUNDOS_MAXIMOS);
		return plan;
	}

	/** Series de cada ejercicio por día (índice 0 = día 1), como las guarda el plan. */
	static Map<String, List<Integer>> seriesPorDia(PlanRutina plan) {
		Map<String, Integer[]> porDia = new TreeMap<>();
		plan.dias().forEach(d -> d.ejercicios().forEach(e -> porDia
				.computeIfAbsent(e.idExterno(), id -> new Integer[plan.dias().size()])[d.dia() - 1] = e.series()));
		Map<String, List<Integer>> partida = new TreeMap<>();
		porDia.forEach((id, s) -> partida.put(id, Arrays.stream(s).map(x -> x == null ? 0 : x).toList()));
		return partida;
	}

	static Map<String, Integer> seriesSemana(PlanRutina plan) {
		Map<String, Integer> series = new TreeMap<>();
		plan.dias().forEach(d -> d.ejercicios().forEach(e -> series.merge(e.idExterno(), e.series(), Integer::sum)));
		return series;
	}

	@Test
	void unaSerieMenosDePressInclinadoSoloQuitaEsaSerieYComoMuchoCompensaDos() {
		int antes = seriesSemana(base).getOrDefault(PRESS_INCLINADO, 0);
		assertThat(antes).as("el caso real parte de un plan con press inclinado").isGreaterThan(2);

		PlanRutina menos = ajustar(Map.of(PRESS_INCLINADO, antes - 1), Set.of(), List.of());

		Cambios cambios = Cambios.entre(base, menos);
		System.out.println("«Menos»: " + cambios);
		assertThat(seriesSemana(menos).get(PRESS_INCLINADO)).isEqualTo(antes - 1);
		assertThat(cambios.entran()).isEmpty();
		assertThat(cambios.salen()).isEmpty();
		assertThat(cambios.compensaciones(PRESS_INCLINADO)).hasSizeLessThanOrEqualTo(2);
	}

	@Test
	void unaSerieMasSoloAnadeEsaSerieYComoMuchoCompensaDos() {
		Map.Entry<String, Integer> elegido = seriesSemana(base).entrySet().stream()
				.filter(e -> e.getValue() < 2 * CONFIG.sesion().seriesMaximasPorEjercicio())
				.filter(e -> seriesSemana(GENERADOR.generar(servicio.catalogo(), entrada(
						Map.of(e.getKey(), e.getValue() + 1), Set.of(), List.of(), new Partida(seriesPorDia(base), 0))))
						.get(e.getKey()) == e.getValue() + 1)
				.findFirst().orElseThrow();

		PlanRutina mas = ajustar(Map.of(elegido.getKey(), elegido.getValue() + 1), Set.of(), List.of());

		Cambios cambios = Cambios.entre(base, mas);
		System.out.println("«Más» en " + elegido.getKey() + ": " + cambios);
		assertThat(seriesSemana(mas).get(elegido.getKey())).isEqualTo(elegido.getValue() + 1);
		assertThat(cambios.entran()).isEmpty();
		assertThat(cambios.salen()).isEmpty();
		assertThat(cambios.compensaciones(elegido.getKey())).hasSizeLessThanOrEqualTo(2);
	}

	@Test
	void noRecomendarCambiaEseEjercicioPorOtroYPocoMas() {
		// Su hueco (mismo patrón y día) queda libre: en el caso real lo ocupa el press inclinado con mancuernas.
		String quitado = PRESS_INCLINADO;

		PlanRutina sin = ajustar(Map.of(), Set.of(quitado), List.of());

		Cambios cambios = Cambios.entre(base, sin);
		System.out.println("«No recomendar»: " + cambios);
		assertThat(cambios.salen()).containsExactly(quitado);
		assertThat(cambios.entran()).hasSizeLessThanOrEqualTo(1);
		assertThat(cambios.cambian()).hasSizeLessThanOrEqualTo(2);
	}

	@Test
	void enfocarSubeElMusculoMoviendoPoco() {
		// Sin los cambios libres no se movía nada: una serie de bíceps de más gana menos de lo que cuesta cambiar.
		PlanRutina enfocado = ajustar(Map.of(), Set.of(), List.of("biceps"));

		Cambios cambios = Cambios.entre(base, enfocado);
		System.out.println("«Enfocar» bíceps: " + cambios);
		assertThat(volumen(enfocado, "biceps")).isGreaterThan(volumen(base, "biceps"));
		assertThat(cambios.entran().size() + cambios.salen().size()).isLessThanOrEqualTo(2);
		assertThat(cambios.cambian()).hasSizeLessThanOrEqualTo(3);
	}

	@Test
	void resolverDosVecesElMismoAjusteDaElMismoPlan() {
		int antes = seriesSemana(base).get(PRESS_INCLINADO);
		Map<String, Integer> menos = Map.of(PRESS_INCLINADO, antes - 1);

		PlanRutina vistaPrevia = ajustar(menos, Set.of(), List.of());
		PlanRutina aplicado = ajustar(menos, Set.of(), List.of());

		assertThat(aplicado.dias()).isEqualTo(vistaPrevia.dias());
	}

	@Test
	void unaPartidaQueYaNoCabeNoHaceInviableElPlan() {
		// El plan actual se hizo con 4 días y ahora hay 3 y 45 min: lo que no cabe se ignora.
		EntradaRutina menosTiempo = new EntradaRutina(3, 45, GeneradorCatalogoRealTest.MATERIAL_COMPLETO, Set.of(),
				List.of(), EntradaRutina.FRECUENCIA_POR_DEFECTO, RODILLA_SIN_CARGA_AXIAL, HOMBRO_LIMITADO, Map.of(),
				Set.of(), new Partida(seriesPorDia(base), 0));

		PlanRutina plan = generar(menosTiempo);

		assertThat(plan.estado()).isIn(PlanRutina.OPTIMO, PlanRutina.FACTIBLE);
		assertThat(plan.dias()).hasSize(3).allSatisfy(d -> assertThat(d.ejercicios()).isNotEmpty());
		assertThat(plan.notas()).isEmpty();
	}

	private static double volumen(PlanRutina plan, String musculo) {
		return plan.volumen().stream().filter(v -> v.musculo().equals(musculo)).findFirst().orElseThrow().series();
	}
}
