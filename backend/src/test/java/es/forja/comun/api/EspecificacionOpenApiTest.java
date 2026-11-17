package es.forja.comun.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import es.forja.PruebaConMysql;
import tools.jackson.core.type.TypeReference;
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
}
