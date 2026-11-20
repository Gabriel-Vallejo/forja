package es.forja.entreno.plan.modelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import es.forja.entreno.rutina.modelo.PlanRutina;
import es.forja.entreno.rutina.modelo.PlanRutina.ContribucionEjercicio;

class TrazaGuardadaTest {

	@Test
	void noGuardaElTerminoDeMovilidadPorqueDelataElTramo() {
		ContribucionEjercicio c = new ContribucionEjercicio("sentadilla", "Sentadilla",
				Map.of("volumen", 40L, "movilidad", -10L));
		PlanRutina plan = new PlanRutina(PlanRutina.OPTIMO, "", 40, List.of(), List.of(), List.of(), List.of(c),
				List.of(), List.of(), List.of());

		TrazaGuardada traza = TrazaGuardada.de(plan);

		assertThat(traza.contribuciones()).singleElement()
				.satisfies(g -> assertThat(g.terminos()).containsOnlyKeys("volumen"));
	}
}
