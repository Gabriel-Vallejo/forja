package es.forja.entreno.rutina.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.oneOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import es.forja.entreno.rutina.servicio.PropiedadesRutina;
import es.forja.entreno.rutina.solver.ConfiguracionDePrueba;
import es.forja.restricciones.api.SoporteLimitaciones;
import tools.jackson.databind.JsonNode;

class RutinasApiTest extends SoporteLimitaciones {

	private static final String RUTA = "/api/v1/rutinas/generar";
	private static final List<String> MATERIAL_COMPLETO = List.of("barra", "mancuerna", "maquina", "polea",
			"peso_corporal", "kettlebell", "banda", "barra_ez", "balon_medicinal", "fitball", "rodillo", "otro");

	@Autowired
	private PropiedadesRutina propiedades;

	private static Map<String, Object> cuerpo(int dias, int minutos) {
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("diasPorSemana", dias);
		cuerpo.put("minutosPorSesion", minutos);
		cuerpo.put("material", MATERIAL_COMPLETO);
		cuerpo.put("exclusiones", List.of());
		cuerpo.put("prioritarios", List.of("pecho"));
		return cuerpo;
	}

	private static List<String> elegidos(JsonNode plan) {
		List<String> ids = new ArrayList<>();
		plan.get("dias").forEach(d -> d.get("ejercicios").forEach(e -> ids.add(e.get("idExterno").asString())));
		return ids;
	}

	private static List<String> tiposDeDescarte(JsonNode plan) {
		List<String> tipos = new ArrayList<>();
		plan.get("descartes").forEach(d -> tipos.add(d.get("tipo").asString()));
		return tipos;
	}

	@Test
	void laConfiguracionDeApplicationYmlEsLaDeLosTests() {
		assertThat(propiedades.aConfiguracion()).isEqualTo(ConfiguracionDePrueba.estandar());
	}

	@Test
	void devuelveUnPlanValidoConSuTraza() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		enviarAutenticado(RUTA, cliente, cuerpo(3, 60)).andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value(oneOf("OPTIMAL", "FEASIBLE")))
				.andExpect(jsonPath("$.dias", hasSize(3))).andExpect(jsonPath("$.volumen").isArray())
				.andExpect(jsonPath("$.contribuciones").isArray()).andExpect(jsonPath("$.alternativas").isArray());
	}

	@Test
	void conSinCargaAxialYConsentimientoNoHaySentadillaConBarra() throws Exception {
		Cliente cliente = clienteConLimitaciones();
		crearRestriccion(cliente, restriccion(null, "sentadilla", "sin_carga_axial"));

		JsonNode plan = cuerpoDe(enviarAutenticado(RUTA, cliente, cuerpo(4, 60)).andExpect(status().isOk()));

		assertThat(elegidos(plan)).doesNotContain("Barbell_Squat", "Front_Barbell_Squat");
		assertThat(tiposDeDescarte(plan)).contains("sustitucion");
	}

	@Test
	void sinConsentimientoLasRestriccionesGuardadasNoSeUsan() throws Exception {
		Cliente cliente = clienteConLimitaciones();
		crearRestriccion(cliente, restriccion(null, "sentadilla", "evitar"));
		// Se revoca a mano, sin el evento que borra las restricciones, para que sigan guardadas.
		jdbc.sql("""
				UPDATE consentimiento c JOIN usuario u ON u.id = c.usuario_id SET c.revocado_en = NOW()
				WHERE u.correo = :correo""").param("correo", cliente.correo()).update();
		assertThat(contar("SELECT COUNT(*) FROM restriccion r JOIN usuario u ON u.id = r.usuario_id "
				+ "WHERE u.correo = ?", cliente.correo())).isEqualTo(1);

		JsonNode plan = cuerpoDe(enviarAutenticado(RUTA, cliente, cuerpo(4, 60)).andExpect(status().isOk()));

		assertThat(tiposDeDescarte(plan)).doesNotContain("restriccion", "sustitucion");
	}

	@Test
	void validaLaEntrada() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");
		Map<String, Object> muchosDias = cuerpo(7, 60);
		Map<String, Object> musculoRaro = cuerpo(3, 60);
		musculoRaro.put("prioritarios", List.of("no_existe"));
		Map<String, Object> frecuenciaRara = cuerpo(3, 60);
		frecuenciaRara.put("frecuenciaMinima", 3);

		enviarAutenticado(RUTA, cliente, muchosDias).andExpect(status().isBadRequest());
		enviarAutenticado(RUTA, cliente, cuerpo(3, 20)).andExpect(status().isBadRequest());
		enviarAutenticado(RUTA, cliente, musculoRaro).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
		enviarAutenticado(RUTA, cliente, frecuenciaRara).andExpect(status().isBadRequest());
	}

	@Test
	void sinSesionDa401() throws Exception {
		mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(cuerpo(3, 60)))).andExpect(status().isUnauthorized());
	}
}
