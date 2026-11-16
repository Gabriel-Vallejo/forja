package es.forja.sesiones.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;

/** RF-26 de punta a punta: la máquina de estados vista desde {@code /planes/actual} y {@code /progresion}. */
class ProgresionApiTest extends SoporteSesiones {

	private static final String RUTA_PROGRESION = "/api/v1/progresion/";

	private record Escenario(Cliente cliente, String plan, Objetivo objetivo) {
	}

	private Escenario escenario() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");
		String plan = generarPlan(cliente);
		return new Escenario(cliente, plan, ejercicioConIncremento(cliente));
	}

	private Instant haceDias(long dias) {
		return reloj.instant().minus(Duration.ofDays(dias));
	}

	/** Una sesión de {@code series} series de trabajo con el mismo peso y repeticiones, a RIR 2. */
	private void entrenar(Escenario e, long diasAtras, String peso, int repeticiones, int series) throws Exception {
		List<java.util.Map<String, Object>> filas = java.util.stream.IntStream.range(0, series)
				.mapToObj(i -> serie(UUID.randomUUID(), e.objetivo().idExterno(), i, peso, repeticiones, 2)).toList();
		guardarSesion(e.cliente(), UUID.randomUUID(), sesion(e.plan(), haceDias(diasAtras), filas))
				.andExpect(status().isOk());
	}

	private JsonNode ejercicio(Escenario e) throws Exception {
		for (JsonNode dia : planActual(e.cliente()).get("dias")) {
			for (JsonNode ej : dia.get("ejercicios")) {
				if (ej.get("idExterno").asString().equals(e.objetivo().idExterno())) {
					return ej;
				}
			}
		}
		throw new AssertionError("El ejercicio no está en el plan");
	}

	@Test
	void unPlanNuevoEmpiezaAcumulandoYCadaEjercicioTraeSuMotivo() throws Exception {
		Escenario e = escenario();

		for (JsonNode dia : planActual(e.cliente()).get("dias")) {
			for (JsonNode ej : dia.get("ejercicios")) {
				assertThat(ej.get("estado").asString()).isEqualTo("acumulando");
				assertThat(ej.get("motivo").asString()).isNotBlank();
			}
		}
	}

	@Test
	void cumplirLaReglaPasaASubirCargaConSuMotivo() throws Exception {
		Escenario e = escenario();
		entrenar(e, 2, "50", e.objetivo().repeticionesMax(), e.objetivo().series());

		JsonNode ej = ejercicio(e);

		assertThat(ej.get("estado").asString()).isEqualTo("subir_carga");
		assertThat(ej.get("cargaKg").decimalValue()).isEqualByComparingTo(BigDecimal.valueOf(50 + e.objetivo().incrementoKg()));
		assertThat(ej.get("motivo").asString()).contains("sube a");
	}

	@Test
	void tresSemanasSinMejorarLlevanAUnDeloadYLaSesionDeDescargaVuelveAAcumular() throws Exception {
		Escenario e = escenario();
		int reps = e.objetivo().repeticionesMax();
		entrenar(e, 60, "60", reps, e.objetivo().series());
		entrenar(e, 30, "60", reps - 2, e.objetivo().series());
		entrenar(e, 25, "60", reps - 2, e.objetivo().series());
		entrenar(e, 20, "60", reps - 2, e.objetivo().series());

		JsonNode deload = ejercicio(e);

		assertThat(deload.get("estado").asString()).isEqualTo("deload");
		assertThat(deload.get("cargaKg").decimalValue()).isEqualByComparingTo("54");
		assertThat(deload.get("series").asInt()).isEqualTo((e.objetivo().series() + 1) / 2);
		assertThat(deload.get("motivo").asString()).contains("3 semanas sin superar tu mejor marca estimada")
				.contains("deload al 90 %").contains("la mitad de series");

		entrenar(e, 10, "54", reps, (e.objetivo().series() + 1) / 2);

		JsonNode despues = ejercicio(e);
		assertThat(despues.get("estado").asString()).isEqualTo("acumulando");
		assertThat(despues.get("cargaKg").decimalValue()).isEqualByComparingTo("60");
		assertThat(despues.get("series").asInt()).isEqualTo(e.objetivo().series());
	}

	@Test
	void laRutaDeProgresionDaElEstadoElHistorialDeE1rmYLaCargaSiguiente() throws Exception {
		Escenario e = escenario();
		entrenar(e, 9, "50", 8, e.objetivo().series());
		entrenar(e, 2, "60", 10, e.objetivo().series());

		consultar(RUTA_PROGRESION + e.objetivo().idExterno(), e.cliente()).andExpect(status().isOk())
				.andExpect(jsonPath("$.idExterno").value(e.objetivo().idExterno()))
				.andExpect(jsonPath("$.estado").value("subir_carga"))
				.andExpect(jsonPath("$.historial.length()").value(2))
				.andExpect(jsonPath("$.historial[0].e1rm").value(63.33))
				.andExpect(jsonPath("$.historial[1].e1rm").value(80.0))
				.andExpect(jsonPath("$.cargaSiguienteKg").value(60 + e.objetivo().incrementoKg()))
				.andExpect(jsonPath("$.motivo").isNotEmpty());
	}

	@Test
	void unEjercicioSinSesionesDa200ConElHistorialVacio() throws Exception {
		Escenario e = escenario();

		consultar(RUTA_PROGRESION + e.objetivo().idExterno(), e.cliente()).andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("acumulando")).andExpect(jsonPath("$.historial.length()").value(0));
	}

	@Test
	void unEjercicioFueraDelPlanSeEvaluaConLosObjetivosPorDefecto() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		consultar(RUTA_PROGRESION + ejercicioDelCatalogo(), cliente).andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("acumulando")).andExpect(jsonPath("$.series").value(3));
	}

	@Test
	void unEjercicioQueNoExisteDa404YSinSesionDa401() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		consultar(RUTA_PROGRESION + "no_existe", cliente).andExpect(status().isNotFound());
		mvc.perform(get(RUTA_PROGRESION + "Barbell_Squat")).andExpect(status().isUnauthorized());
	}

	@Test
	void laProgresionDeUnUsuarioNoVeLasSesionesDeOtro() throws Exception {
		Escenario mio = escenario();
		Escenario ajeno = escenario();
		entrenar(ajeno, 2, "50", ajeno.objetivo().repeticionesMax(), ajeno.objetivo().series());

		consultar(RUTA_PROGRESION + ajeno.objetivo().idExterno(), mio.cliente()).andExpect(status().isOk())
				.andExpect(jsonPath("$.historial.length()").value(0));
	}

	private String ejercicioDelCatalogo() {
		return jdbc.sql("SELECT id_externo FROM ejercicio WHERE origen_etiquetado = 'curado' LIMIT 1")
				.query(String.class).single();
	}
}
