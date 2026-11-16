package es.forja.entreno.rutina.solver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import es.forja.PruebaConMysql;
import es.forja.entreno.rutina.modelo.CatalogoRutina;
import es.forja.entreno.rutina.modelo.EntradaRutina;
import es.forja.entreno.rutina.modelo.PlanRutina;
import es.forja.entreno.rutina.modelo.PlanRutina.EjercicioPlan;
import es.forja.entreno.rutina.servicio.ServicioRutina;

/** Casos concretos del generador sobre el catálogo real sembrado por Flyway, y el tiempo de resolución. */
@PruebaConMysql
class GeneradorCatalogoRealTest {

	static final Set<String> MATERIAL_COMPLETO = Set.of("barra", "mancuerna", "maquina", "polea", "peso_corporal",
			"kettlebell", "banda", "barra_ez", "balon_medicinal", "fitball", "rodillo", "otro");
	private static final int PESO_ESTIRADA_ALTO = 20;

	@Autowired
	private ServicioRutina servicio;

	private static EntradaRutina entrada(int dias, int minutos) {
		return new EntradaRutina(dias, minutos, MATERIAL_COMPLETO, Set.of(), List.of(),
				EntradaRutina.FRECUENCIA_POR_DEFECTO, List.of());
	}

	private static int seriesEstiradas(PlanRutina plan) {
		return plan.dias().stream().flatMap(d -> d.ejercicios().stream())
				.filter(EjercicioPlan::posicionEstirada).mapToInt(EjercicioPlan::series).sum();
	}

	@Test
	void conDosDiasYTreintaMinutosSeUsaHolguraYElMensajeLoDice() {
		PlanRutina plan = new GeneradorRutina(ConfiguracionDePrueba.estandar()).generar(servicio.catalogo(),
				entrada(2, 30));

		assertThat(plan.holguras()).isNotEmpty();
		assertThat(plan.holguras()).anySatisfy(h -> assertThat(h.mensaje())
				.matches("Con 2 días y 30 min no llegas a 10 series de .+; te quedan [0-9,]+\\."));
	}

	@Test
	void conCuatroDiasYUnaHoraTodoMusculoGrandeAlcanzaElMinimoSinHolgura() {
		CatalogoRutina catalogo = servicio.catalogo();
		long inicio = System.nanoTime();

		PlanRutina plan = new GeneradorRutina(ConfiguracionDePrueba.estandar()).generar(catalogo, entrada(4, 60));

		long milisegundos = (System.nanoTime() - inicio) / 1_000_000;
		// Dato que pide el ADR 0002 («[estimación, medir en la fase 1]»): se lee en la salida de la suite.
		System.out.printf("Tiempo de resolución, catálogo real, 4 días y 60 min: %d ms (estado %s, %d alternativas)%n",
				milisegundos, plan.estado(), plan.alternativas().size());
		plan.dias().forEach(d -> System.out.println("Día " + d.dia() + ": " + d.ejercicios().stream()
				.map(e -> e.nombre() + " " + e.series() + (e.posicionEstirada() ? " (estirada)" : "")).toList()));
		ConfiguracionDePrueba.MUSCULOS_GRANDES.forEach(m -> {
			assertThat(plan.holguras()).noneSatisfy(h -> assertThat(h.musculo()).isEqualTo(m));
			assertThat(plan.volumen()).anySatisfy(v -> {
				assertThat(v.musculo()).isEqualTo(m);
				assertThat(v.series()).isGreaterThanOrEqualTo(v.minimo());
			});
		});
	}

	@Test
	void subirElPesoDePosicionEstiradaEligeMasSeriesEnPosicionEstirada() {
		CatalogoRutina catalogo = servicio.catalogo();

		PlanRutina sinPeso = new GeneradorRutina(ConfiguracionDePrueba.conPesoEstirada(0)).generar(catalogo,
				entrada(4, 60));
		PlanRutina conPeso = new GeneradorRutina(ConfiguracionDePrueba.conPesoEstirada(PESO_ESTIRADA_ALTO))
				.generar(catalogo, entrada(4, 60));

		assertThat(seriesEstiradas(conPeso)).isGreaterThan(seriesEstiradas(sinPeso));
	}

	@Test
	void elVolumenSeMarcaComoHeuristicaDeEntrenadores() {
		PlanRutina plan = new GeneradorRutina(ConfiguracionDePrueba.estandar()).generar(servicio.catalogo(),
				entrada(3, 45));

		assertThat(plan.volumen()).allSatisfy(
				v -> assertThat(v.fuente()).isEqualTo("heurística de entrenadores (RP), nivel H"));
	}
}
