package es.forja.entreno.estandares;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import es.forja.cuenta.api.ApiCuentaSoporte;
import tools.jackson.databind.JsonNode;

/** Las tablas de la semilla de OpenPowerlifting, tal como las recibe la app. */
class EstandaresApiTest extends ApiCuentaSoporte {

	@Test
	void devuelveLos45GruposCon99PercentilesQueCrecen() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		String cuerpo = mvc.perform(get("/api/v1/estandares").header("Authorization", cliente.cabecera()))
				.andExpect(status().isOk()).andExpect(header().string("Cache-Control", "max-age=604800, private"))
				.andReturn().getResponse().getContentAsString();
		JsonNode tablas = json.readTree(cuerpo);

		assertThat(tablas.size()).isEqualTo(45);
		for (JsonNode tabla : tablas) {
			List<Double> kg = new ArrayList<>();
			tabla.get("kg").forEach(n -> kg.add(n.asDouble()));
			assertThat(kg).hasSize(99).isSorted();
			assertThat(tabla.get("muestra").asInt()).isGreaterThanOrEqualTo(200);
		}
		JsonNode banca = tablas.valueStream().filter(t -> t.get("levantamiento").asString().equals("banca")
				&& t.get("sexo").asString().equals("hombre") && t.get("pesoDesdeKg").asDouble() == 80.0)
				.findFirst().orElseThrow();
		assertThat(banca.get("pesoHastaKg").asDouble()).isEqualTo(90.0);
	}

	@Test
	void sinSesionNoSeSirve() throws Exception {
		mvc.perform(get("/api/v1/estandares")).andExpect(status().isUnauthorized());
	}
}
