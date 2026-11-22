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
	void unaCuentaNuevaNoTieneMarcas() throws Exception {
		consultar("/api/v1/progreso/marcas", cuentaConSesion("Pixel")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void lasMarcasSonLaMejorSeriePorE1rmSinCalentamientoNiPesoCorporalNiLoDeOtros() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");
		String plan = generarPlan(cliente);
		String banca = "Barbell_Bench_Press_-_Medium_Grip";
		var cuando = reloj.instant().minus(Duration.ofHours(2));
		guardarSesion(cliente, UUID.randomUUID(), sesion(plan, cuando, List.of(
				serie(UUID.randomUUID(), banca, 1, "80", 8, 2), // e1RM 101,33
				serie(UUID.randomUUID(), banca, 2, "90", 3, 1), // e1RM 99
				serie(UUID.randomUUID(), banca, 3, "60", 20, 0), // más de 12 repeticiones: no cuenta
				con(serie(UUID.randomUUID(), banca, 4, "120", 1, 0), "calentamiento", true))))
				.andExpect(status().isOk());
		Cliente otro = cuentaConSesion("Otro");
		guardarSesion(otro, UUID.randomUUID(), sesion(generarPlan(otro), cuando,
				List.of(serie(UUID.randomUUID(), banca, 1, "150", 5, 2)))).andExpect(status().isOk());

		consultar("/api/v1/progreso/marcas", cliente).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].idExterno").value(banca))
				.andExpect(jsonPath("$[0].levantamiento").value("banca"))
				.andExpect(jsonPath("$[0].pesoKg").value(80)).andExpect(jsonPath("$[0].repeticiones").value(8))
				.andExpect(jsonPath("$[0].e1rmKg").value(101.33))
				.andExpect(jsonPath("$[0].grupos[0]").value("pecho"));
	}

	@Test
	void enLasDominadasConPesoCorporalLaMarcaEsElLastreEquivalente() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");
		darConsentimiento(cliente, "medidas_salud").andExpect(status().isOk());
		actualizar("/api/v1/perfil/medidas", cliente, java.util.Map.of("pesoKg", 80)).andExpect(status().isOk());
		String plan = generarPlan(cliente);
		guardarSesion(cliente, UUID.randomUUID(), sesion(plan, reloj.instant().minus(Duration.ofHours(2)), List.of(
				serie(UUID.randomUUID(), "Pullups", 1, "0", 12, 1), // 80 × 42 / 30 − 80 = 32
				serie(UUID.randomUUID(), "Pullups", 2, "20", 5, 1)))) // 100 × 35 / 30 − 80 = 36,67
				.andExpect(status().isOk());

		consultar("/api/v1/progreso/marcas", cliente).andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].levantamiento").value("dominada_lastre"))
				.andExpect(jsonPath("$[0].pesoKg").value(20)).andExpect(jsonPath("$[0].e1rmKg").value(36.67));
	}

	@Test
	void unaZonaQueNoExisteDa400() throws Exception {
		consultar(RUTA + "?zona=Marte/Olimpo", cuentaConSesion("Pixel")).andExpect(status().isBadRequest());
	}
}
