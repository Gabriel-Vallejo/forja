package es.forja.entreno.perfil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import es.forja.cuenta.api.ApiCuentaSoporte;

/** Sexo, experiencia y objetivo: preferencias sin consentimiento, una fila por usuario. */
class PerfilDatosApiTest extends ApiCuentaSoporte {

	private static final String RUTA = "/api/v1/perfil/datos";

	private static Map<String, Object> datos(Object sexo, Object experiencia, Object objetivo) {
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("sexo", sexo);
		cuerpo.put("experiencia", experiencia);
		cuerpo.put("objetivo", objetivo);
		return cuerpo;
	}

	private ResultActions guardar(Cliente cliente, Object cuerpo) throws Exception {
		return mvc.perform(put(RUTA).header("Authorization", cliente.cabecera()).contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(cuerpo)));
	}

	@Test
	void sinFilaElSexoNoSeIndicaYLoDemasQuedaSinDefinir() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		consultar(RUTA, cliente).andExpect(status().isOk()).andExpect(jsonPath("$.sexo").value("no_indica"))
				.andExpect(jsonPath("$.experiencia").doesNotExist()).andExpect(jsonPath("$.objetivo").doesNotExist());
	}

	@Test
	void guardaYVuelveALeer() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		guardar(cliente, datos("hombre", "intermedio", "fuerza_hipertrofia")).andExpect(status().isOk())
				.andExpect(jsonPath("$.sexo").value("hombre")).andExpect(jsonPath("$.experiencia").value("intermedio"))
				.andExpect(jsonPath("$.objetivo").value("fuerza_hipertrofia"));
		consultar(RUTA, cliente).andExpect(jsonPath("$.sexo").value("hombre"))
				.andExpect(jsonPath("$.objetivo").value("fuerza_hipertrofia"));

		guardar(cliente, datos("no_indica", null, "fuerza")).andExpect(status().isOk());
		consultar(RUTA, cliente).andExpect(jsonPath("$.sexo").value("no_indica"))
				.andExpect(jsonPath("$.experiencia").doesNotExist()).andExpect(jsonPath("$.objetivo").value("fuerza"));
		assertThat(contar("SELECT COUNT(*) FROM perfil_datos d JOIN usuario u ON u.id = d.usuario_id "
				+ "WHERE u.correo = ?", cliente.correo())).isEqualTo(1);
	}

	@Test
	void rechazaValoresQueNoExisten() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		guardar(cliente, datos("otro", null, null)).andExpect(status().isBadRequest());
		guardar(cliente, datos(null, null, null)).andExpect(status().isBadRequest());
		guardar(cliente, datos("mujer", "experto", null)).andExpect(status().isBadRequest());
		guardar(cliente, datos("mujer", null, "definicion")).andExpect(status().isBadRequest());
	}

	@Test
	void cadaUsuarioVeSoloLoSuyo() throws Exception {
		Cliente uno = cuentaConSesion("Pixel");
		Cliente otro = cuentaConSesion("Pixel");

		guardar(uno, datos("mujer", "avanzado", "hipertrofia")).andExpect(status().isOk());

		consultar(RUTA, otro).andExpect(jsonPath("$.sexo").value("no_indica"));
	}

	@Test
	void sinSesionNoHayAcceso() throws Exception {
		mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
		mvc.perform(put(RUTA).contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(datos("mujer", null, null)))).andExpect(status().isUnauthorized());
	}
}
