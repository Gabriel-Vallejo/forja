package es.forja.entreno.plan.modelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import es.forja.entreno.plan.modelo.VistaPrevia.CambioEjercicio;
import es.forja.entreno.plan.modelo.VistaPrevia.CambioMusculo;
import es.forja.entreno.rutina.modelo.PlanRutina;
import es.forja.entreno.rutina.modelo.PlanRutina.DiaPlan;
import es.forja.entreno.rutina.modelo.PlanRutina.EjercicioPlan;
import es.forja.entreno.rutina.modelo.PlanRutina.VolumenMusculo;

class VistaPreviaTest {

	private static EjercicioPlan ejercicio(String id, int series) {
		return new EjercicioPlan(id, "Nombre " + id, "sentadilla", series, false, List.of());
	}

	private static VolumenMusculo volumen(String musculo, double series) {
		return new VolumenMusculo(musculo, "Nombre " + musculo, series, 10, 14, 20, "H");
	}

	private static PlanRutina plan(List<DiaPlan> dias, List<VolumenMusculo> volumen) {
		return new PlanRutina("OPTIMAL", "", 0, dias, volumen, List.of(), List.of(), List.of(), List.of(), List.of());
	}

	@Test
	void cuentaLasSeriesDeLaSemanaYSoloDevuelveLoQueCambia() {
		PlanRutina despues = plan(List.of(new DiaPlan(1, 0, List.of(ejercicio("a", 3), ejercicio("c", 2))),
				new DiaPlan(2, 0, List.of(ejercicio("a", 2), ejercicio("d", 3)))), List.of());
		Map<String, Integer> antes = Map.of("a", 5, "b", 4, "c", 3, "d", 3);

		List<CambioEjercicio> cambios = VistaPrevia.cambiosDeEjercicios(antes,
				Map.of("a", "A", "b", "B", "c", "C", "d", "D"), despues);

		assertThat(VistaPrevia.seriesSemana(despues)).containsEntry("a", 5).containsEntry("c", 2);
		assertThat(cambios).containsExactly(new CambioEjercicio("b", "B", 4, 0), new CambioEjercicio("c", "C", 3, 2));
	}

	@Test
	void unEjercicioQueEntraLlevaElNombreDelPlanNuevo() {
		PlanRutina despues = plan(List.of(new DiaPlan(1, 0, List.of(ejercicio("n", 3)))), List.of());

		assertThat(VistaPrevia.cambiosDeEjercicios(Map.of(), Map.of(), despues))
				.containsExactly(new CambioEjercicio("n", "Nombre n", 0, 3));
	}

	@Test
	void losMusculosQueCambianConSusSeriesAntesYDespues() {
		List<CambioMusculo> cambios = VistaPrevia.cambiosDeMusculos(
				List.of(volumen("pecho", 12), volumen("biceps", 10), volumen("gemelos", 4)),
				List.of(volumen("pecho", 12), volumen("biceps", 13.5), volumen("triceps", 2)));

		assertThat(cambios).containsExactly(new CambioMusculo("biceps", "Nombre biceps", 10, 13.5),
				new CambioMusculo("gemelos", "Nombre gemelos", 4, 0),
				new CambioMusculo("triceps", "Nombre triceps", 0, 2));
	}
}
