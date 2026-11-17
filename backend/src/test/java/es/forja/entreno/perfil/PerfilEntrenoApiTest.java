package es.forja.entreno.perfil;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import es.forja.cuenta.api.ApiCuentaSoporte;

class PerfilEntrenoApiTest extends ApiCuentaSoporte {

	private static final String RUTA = "/api/v1/perfil/entreno";
	private static final String RUTA_MATERIALES = "/api/v1/catalogo/materiales";

	private ResultActions guardar(Cliente cliente, Object cuerpo) throws Exception {
		return mvc.perform(put(RUTA).header("Authorization", cliente.cabecera())
				.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo)));
	}

	private static Map<String, Object> ajustes(Object dias, Object minutos, Object material) {
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("diasPorSemana", dias);
		cuerpo.put("minutosPorSesion", minutos);
		cuerpo.put("material", material);
		return cuerpo;
	}

	@Test
	void sinFilaDevuelveLosValoresPorDefecto() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		consultar(RUTA, cliente).andExpect(status().isOk()).andExpect(jsonPath("$.diasPorSemana").value(3))
				.andExpect(jsonPath("$.minutosPorSesion").value(60)).andExpect(jsonPath("$.porDefecto").value(true))
				.andExpect(jsonPath("$.material", containsInAnyOrder("barra", "barra_ez", "mancuerna", "maquina",
						"polea", "kettlebell", "banda", "peso_corporal")));
	}

	@Test
	void guardaYVuelveALeerLosAjustes() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		guardar(cliente, ajustes(4, 75, List.of("mancuerna", "banda"))).andExpect(status().isOk())
				.andExpect(jsonPath("$.porDefecto").value(false)).andExpect(jsonPath("$.diasPorSemana").value(4));
		consultar(RUTA, cliente).andExpect(jsonPath("$.diasPorSemana").value(4))
				.andExpect(jsonPath("$.minutosPorSesion").value(75)).andExpect(jsonPath("$.porDefecto").value(false))
				.andExpect(jsonPath("$.material", contains("mancuerna", "banda")));

		// Segundo guardado: reemplaza, no acumula; los códigos repetidos cuentan una vez.
		guardar(cliente, ajustes(2, 30, List.of("polea", "polea"))).andExpect(status().isOk());
		consultar(RUTA, cliente).andExpect(jsonPath("$.diasPorSemana").value(2))
				.andExpect(jsonPath("$.minutosPorSesion").value(30)).andExpect(jsonPath("$.material", contains("polea")));
	}

	@Test
	void rechazaLosAjustesQueNoValen() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");
		List<String> bien = List.of("barra");

		guardar(cliente, ajustes(1, 60, bien)).andExpect(status().isBadRequest());
		guardar(cliente, ajustes(7, 60, bien)).andExpect(status().isBadRequest());
		guardar(cliente, ajustes(3, 29, bien)).andExpect(status().isBadRequest());
		guardar(cliente, ajustes(3, 121, bien)).andExpect(status().isBadRequest());
		guardar(cliente, ajustes(null, 60, bien)).andExpect(status().isBadRequest());
		guardar(cliente, ajustes(3, 60, List.of())).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores[0].mensaje", containsString("al menos un material")));
		guardar(cliente, ajustes(3, 60, null)).andExpect(status().isBadRequest());
		guardar(cliente, ajustes(3, 60, List.of("barra", "teletransportador"))).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", containsString("no existe")));

		consultar(RUTA, cliente).andExpect(jsonPath("$.porDefecto").value(true));
	}

	@Test
	void cadaUsuarioTieneLosSuyos() throws Exception {
		Cliente uno = cuentaConSesion("Pixel");
		Cliente otro = cuentaConSesion("iPhone");

		guardar(uno, ajustes(5, 90, List.of("barra"))).andExpect(status().isOk());

		consultar(RUTA, otro).andExpect(jsonPath("$.porDefecto").value(true)).andExpect(jsonPath("$.diasPorSemana").value(3));
		consultar(RUTA, uno).andExpect(jsonPath("$.diasPorSemana").value(5));
	}

	@Test
	void sinSesionNoHayAcceso() throws Exception {
		mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
		mvc.perform(put(RUTA).contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(ajustes(3, 60, List.of("barra"))))).andExpect(status().isUnauthorized());
		mvc.perform(get(RUTA_MATERIALES)).andExpect(status().isUnauthorized());
	}

	@Test
	void elCatalogoOfreceElMaterialSinOtro() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		consultar(RUTA_MATERIALES, cliente).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(11)))
				.andExpect(jsonPath("$[*].codigo", hasItem("barra"))).andExpect(jsonPath("$[*].codigo", not(hasItem("otro"))))
				.andExpect(jsonPath("$[0].codigo").value("barra")).andExpect(jsonPath("$[0].nombre").value("Barra"));
	}
}
