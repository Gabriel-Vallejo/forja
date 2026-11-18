package es.forja.sesiones.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;

/** «Por qué este ejercicio» (PRD v0.1, RF-19): razones con fuente y nivel, y las de salud solo con consentimiento. */
class PorQueApiTest extends SoporteSesiones {

	private static final List<String> NIVELES = List.of("A", "B", "C", "D", "H", "P");
	private static final String MEDIDAS = "medidas_salud";

	private record EnElPlan(int dia, String idExterno) {
	}

	private static String ruta(int dia, String idExterno) {
		return RUTA_PLAN_ACTUAL + "/ejercicios/" + dia + "/" + idExterno + "/por-que";
	}

	private static List<EnElPlan> enElPlan(JsonNode plan) {
		List<EnElPlan> todos = new ArrayList<>();
		plan.get("dias").forEach(d -> d.get("ejercicios")
				.forEach(e -> todos.add(new EnElPlan(d.get("dia").asInt(), e.get("idExterno").asString()))));
		return todos;
	}

	private JsonNode porQue(Cliente cliente, EnElPlan e) throws Exception {
		return cuerpoDe(consultar(ruta(e.dia(), e.idExterno()), cliente).andExpect(status().isOk())).get("porQue");
	}

	private List<String> codigos(Cliente cliente, EnElPlan e) throws Exception {
		List<String> codigos = new ArrayList<>();
		porQue(cliente, e).forEach(r -> codigos.add(r.get("codigo").asString()));
		return codigos;
	}

	/** El primer ejercicio del plan con una razón de ese código. */
	private EnElPlan conRazon(Cliente cliente, String codigo) throws Exception {
		for (EnElPlan e : enElPlan(planActual(cliente))) {
			if (codigos(cliente, e).contains(codigo)) {
				return e;
			}
		}
		throw new AssertionError("Ningún ejercicio del plan explica «" + codigo + "»");
	}

	private String generar(Cliente cliente, int dias) throws Exception {
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("diasPorSemana", dias);
		cuerpo.put("minutosPorSesion", 60);
		cuerpo.put("material", MATERIAL_COMPLETO);
		return cuerpoDe(enviarAutenticado(RUTA_GENERAR, cliente, cuerpo).andExpect(status().isOk())).get("uuid")
				.asString();
	}

	private String trazaGuardada(Cliente cliente) {
		return jdbc.sql("""
				SELECT p.traza FROM plan_version p JOIN usuario u ON u.id = p.usuario_id
				WHERE u.correo = :correo ORDER BY p.id DESC LIMIT 1""").param("correo", cliente.correo())
				.query(String.class).single();
	}

	@Test
	void cadaEjercicioTraeDeUnaACuatroRazonesConFuenteYNivel() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");
		generarPlan(cliente);

		List<String> todas = new ArrayList<>();
		for (EnElPlan e : enElPlan(planActual(cliente))) {
			JsonNode razones = porQue(cliente, e);
			assertThat(razones.size()).isBetween(1, 4);
			razones.forEach(r -> {
				todas.add(r.get("codigo").asString());
				assertThat(r.get("texto").asString()).isNotBlank();
				assertThat(r.get("fuente").asString()).isNotBlank();
				assertThat(r.get("nivel").asString()).isIn(NIVELES);
			});
		}
		assertThat(todas).contains("volumen", "compuesto", "prioritario").doesNotContain("limitacion", "movilidad");
	}

	@Test
	void laPosicionEstiradaCitaSuEstudio() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");
		generarPlan(cliente);

		EnElPlan e = conRazon(cliente, "posicion_estirada");

		porQue(cliente, e).forEach(r -> {
			if (r.get("codigo").asString().equals("posicion_estirada")) {
				assertThat(r.get("fuente").asString()).matches("[A-Z][a-z]+ 20\\d\\d");
				assertThat(r.get("nivel").asString()).isIn("B", "C");
			}
		});
	}

	@Test
	void conUnaLimitacionSaleLaSustitucionYAlRetirarElConsentimientoDesaparece() throws Exception {
		Cliente cliente = clienteConLimitaciones();
		crearRestriccion(cliente, restriccion(null, "sentadilla", "sin_carga_axial"));
		generar(cliente, 4);

		EnElPlan sustituto = conRazon(cliente, "limitacion");
		JsonNode primera = porQue(cliente, sustituto).get(0);
		assertThat(primera.get("codigo").asString()).isEqualTo("limitacion");
		assertThat(primera.get("texto").asString()).contains("compatible con la limitación que indicaste");
		assertThat(primera.get("nivel").asString()).isEqualTo("P");
		assertThat(trazaGuardada(cliente)).doesNotContain("limitación", "sin carga axial");

		borrarRecurso(RUTA_CONSENTIMIENTOS + "/limitaciones", cliente).andExpect(status().isNoContent());

		assertThat(codigos(cliente, sustituto)).isNotEmpty().doesNotContain("limitacion");
	}

	@Test
	void conElTobilloLimitadoLaGobletExplicaElTramoYNoSeGuarda() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");
		darConsentimiento(cliente, MEDIDAS).andExpect(status().isOk());
		Map<String, Object> tramos = new LinkedHashMap<>();
		tramos.put("tobillo", "limitado");
		tramos.put("cadera", null);
		tramos.put("hombro", null);
		actualizar("/api/v1/movilidad", cliente, tramos).andExpect(status().isOk());
		generar(cliente, 4);

		EnElPlan goblet = enElPlan(planActual(cliente)).stream().filter(e -> e.idExterno().equals("Goblet_Squat"))
				.findFirst().orElseThrow();
		JsonNode movilidad = null;
		for (JsonNode r : porQue(cliente, goblet)) {
			if (r.get("codigo").asString().equals("movilidad")) {
				movilidad = r;
			}
		}
		assertThat(movilidad).isNotNull();
		assertThat(movilidad.get("texto").asString()).contains("tobillo").contains("limitado");
		assertThat(movilidad.get("nivel").asString()).isEqualTo("H");
		assertThat(movilidad.get("fuente").asString()).contains("no ciencia");
		assertThat(trazaGuardada(cliente)).doesNotContain("tramo", "tobillo", "limitado");

		borrarRecurso(RUTA_CONSENTIMIENTOS + "/" + MEDIDAS, cliente).andExpect(status().isNoContent());

		assertThat(codigos(cliente, goblet)).doesNotContain("movilidad");
	}

	@Test
	void unEjercicioQueNoEstaEseDiaDa404() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");
		generarPlan(cliente);
		EnElPlan primero = enElPlan(planActual(cliente)).get(0);

		consultar(ruta(primero.dia(), "No_Existe"), cliente).andExpect(status().isNotFound());
		consultar(ruta(99, primero.idExterno()), cliente).andExpect(status().isNotFound());
	}

	@Test
	void sinPlanDa404YSinSesion401() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		consultar(ruta(1, "Goblet_Squat"), cliente).andExpect(status().isNotFound());
		mvc.perform(get(ruta(1, "Goblet_Squat"))).andExpect(status().isUnauthorized());
	}
}
