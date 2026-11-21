package es.forja.sesiones.api;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/** {@code GET /api/v1/progreso/resumen}: racha y cifras del perfil con las sesiones de verdad. */
class ProgresoApiTest extends SoporteSesiones {

	private static final String RUTA = "/api/v1/progreso/resumen";

	@Test
	void sinSesionesTodoACeroYElObjetivoEsElDeSusAjustes() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		consultar(RUTA + "?zona=Europe/Madrid", cliente).andExpect(status().isOk())
				.andExpect(jsonPath("$.sesiones").value(0)).andExpect(jsonPath("$.kgMovidos").value(0))
				.andExpect(jsonPath("$.objetivoSemanal").value(3)).andExpect(jsonPath("$.semanasDeRacha").value(0))
				.andExpect(jsonPath("$.ultimasSemanas.length()").value(12))
				.andExpect(jsonPath("$.ultimasSemanas[11].dias.length()").value(7));
	}

	@Test
	void cuentaLasSesionesConSeriesDeTrabajoYSusKgPeroNoElCalentamientoNiLoDeOtros() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");
		String plan = generarPlan(cliente);
		Objetivo o = objetivos(planActual(cliente)).getFirst();
		var cuando = reloj.instant().minus(Duration.ofHours(2));
		guardarSesion(cliente, UUID.randomUUID(), sesion(plan, cuando,
				List.of(serie(UUID.randomUUID(), o.idExterno(), 1, "50", 8, 2),
						serie(UUID.randomUUID(), o.idExterno(), 2, "60", 5, 2))))
				.andExpect(status().isOk());
		guardarSesion(cliente, UUID.randomUUID(), sesion(plan, cuando,
				List.of(con(serie(UUID.randomUUID(), o.idExterno(), 1, "20", 10, 2), "calentamiento", true))))
				.andExpect(status().isOk());
		Cliente otro = cuentaConSesion("Otro");
		String planOtro = generarPlan(otro);
		guardarSesion(otro, UUID.randomUUID(), sesion(planOtro, cuando,
				List.of(serie(UUID.randomUUID(), objetivos(planActual(otro)).getFirst().idExterno(), 1, "100", 10, 2))))
				.andExpect(status().isOk());

		consultar(RUTA + "?zona=Europe/Madrid", cliente).andExpect(status().isOk())
				.andExpect(jsonPath("$.sesiones").value(1)).andExpect(jsonPath("$.kgMovidos").value(700))
				.andExpect(jsonPath("$.diasEntrenados").value(1)).andExpect(jsonPath("$.hechosEstaSemana").value(1));
	}

	@Test
	void unaZonaQueNoExisteDa400() throws Exception {
		consultar(RUTA + "?zona=Marte/Olimpo", cuentaConSesion("Pixel")).andExpect(status().isBadRequest());
	}
}
