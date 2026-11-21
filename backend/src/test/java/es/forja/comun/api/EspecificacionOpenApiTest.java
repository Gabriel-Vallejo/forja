package es.forja.comun.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import es.forja.PruebaConMysql;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Escribe la especificación OpenAPI en {@code docs/api/openapi.json}, de donde sale el cliente del frontend. Las
 * claves van ordenadas para que el archivo no cambie entre ejecuciones; el CI falla si el commiteado no coincide.
 */
@PruebaConMysql
@ActiveProfiles("dev")
class EspecificacionOpenApiTest {

	/** Los tests corren desde {@code backend/}. */
	private static final Path DESTINO = Path.of("..", "docs", "api", "openapi.json");

	private static final JsonMapper ORDENADO = JsonMapper.builder()
			.enable(SerializationFeature.INDENT_OUTPUT)
			.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
			.build();

	@Autowired
	private MockMvc mvc;

	@Test
	void escribeLaEspecificacionConLasClavesOrdenadas() throws Exception {
		String cuerpo = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse()
				.getContentAsString(StandardCharsets.UTF_8);

		Map<String, Object> especificacion = ORDENADO.readValue(cuerpo, new TypeReference<>() {
		});

		assertThat(especificacion).containsKeys("openapi", "paths", "components");
		assertThat(((Map<?, ?>) especificacion.get("paths")).keySet()).map(Object::toString).contains(
				"/api/v1/auth/login",
				"/api/v1/auth/refresco", "/api/v1/planes/actual", "/api/v1/rutinas/generar");
		Files.writeString(DESTINO, ORDENADO.writeValueAsString(especificacion) + "\n", StandardCharsets.UTF_8);
	}

	@Test
	void tituloYCodigosDeRespuestaReales() throws Exception {
		JsonNode api = especificacion();

		assertThat(api.at("/info/title").asString()).isEqualTo("Forja API");
		assertThat(codigos(api, "/api/v1/auth/registro", "post")).containsExactly("202");
		assertThat(codigos(api, "/api/v1/auth/recuperar", "post")).containsExactly("202");
		assertThat(codigos(api, "/api/v1/auth/cerrar-sesion", "post")).containsExactly("204");
		assertThat(codigos(api, "/api/v1/restricciones", "post")).containsExactly("201");
		assertThat(codigos(api, "/api/v1/restricciones/{uuid}", "delete")).containsExactly("204");
		assertThat(codigos(api, "/api/v1/cuenta", "delete")).containsExactly("204");
	}

	@Test
	void enLasRespuestasLosCamposSonObligatoriosYLosQuePuedenFaltarAdmitenNull() throws Exception {
		JsonNode efecto = especificacion().at("/components/schemas/RespuestaEfecto");

		assertThat(nombres(efecto.get("required"))).containsExactlyInAnyOrder("idExterno", "nombre", "decision",
				"motivo", "sustituto", "anotaciones");
		assertThat(nombres(efecto.at("/properties/motivo/type"))).containsExactlyInAnyOrder("string", "null");
		assertThat(nombres(efecto.at("/properties/nombre/type"))).containsExactly("string");
		assertThat(efecto.at("/properties/sustituto/anyOf/0/$ref").asString())
				.isEqualTo("#/components/schemas/Sustituto");
		assertThat(efecto.at("/properties/sustituto/anyOf/1/type").asString()).isEqualTo("null");
	}

	@Test
	void lasPeticionesSoloExigenLoQueValida() throws Exception {
		JsonNode login = especificacion().at("/components/schemas/Login");

		assertThat(nombres(login.get("required"))).contains("correo", "contrasena").doesNotContain("dispositivoId");
	}

	private JsonNode especificacion() throws Exception {
		return ORDENADO.readTree(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn()
				.getResponse().getContentAsString(StandardCharsets.UTF_8));
	}

	private static List<String> codigos(JsonNode api, String ruta, String metodo) {
		List<String> codigos = new ArrayList<>();
		api.get("paths").get(ruta).get(metodo).get("responses").propertyNames().forEach(codigos::add);
		return codigos;
	}

	private static List<String> nombres(JsonNode nodo) {
		List<String> nombres = new ArrayList<>();
		if (nodo.isArray()) {
			nodo.forEach(n -> nombres.add(n.asString()));
		} else if (nodo.isString()) {
			nombres.add(nodo.asString());
		}
		return nombres;
	}
}
