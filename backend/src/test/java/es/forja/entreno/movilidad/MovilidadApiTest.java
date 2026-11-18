package es.forja.entreno.movilidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import es.forja.sesiones.api.SoporteSesiones;

/** Tests de movilidad (RF-08): tres tramos por usuario, con el consentimiento de medidas de salud (ADR 0004). */
class MovilidadApiTest extends SoporteSesiones {

	private static final String RUTA = "/api/v1/movilidad";
	private static final String MEDIDAS = "medidas_salud";

	private static Map<String, Object> tramos(String tobillo, String cadera, String hombro) {
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("tobillo", tobillo);
		cuerpo.put("cadera", cadera);
		cuerpo.put("hombro", hombro);
		return cuerpo;
	}

	private ResultActions guardar(Cliente cliente, Object cuerpo) throws Exception {
		return actualizar(RUTA, cliente, cuerpo);
	}

	private Cliente clienteConMedidas() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");
		darConsentimiento(cliente, MEDIDAS).andExpect(status().isOk());
		return cliente;
	}

	private long filas(Cliente cliente) {
		return contar("SELECT COUNT(*) FROM test_movilidad t JOIN usuario u ON u.id = t.usuario_id WHERE u.correo = ?",
				cliente.correo());
	}

	@Test
	void sinConsentimientoDeMedidasNoSeLeeNiSeGuarda() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		consultar(RUTA, cliente).andExpect(status().isForbidden());
		guardar(cliente, tramos("limitado", null, null)).andExpect(status().isForbidden());
		assertThat(filas(cliente)).isZero();
	}

	@Test
	void sinDatosLosTresTestsEstanSinHacer() throws Exception {
		Cliente cliente = clienteConMedidas();

		consultar(RUTA, cliente).andExpect(status().isOk()).andExpect(jsonPath("$.tobillo").doesNotExist())
				.andExpect(jsonPath("$.cadera").doesNotExist()).andExpect(jsonPath("$.hombro").doesNotExist());
	}

	@Test
	void guardaLosTramosConSuFechaYUnNuloBorraEseTest() throws Exception {
		Cliente cliente = clienteConMedidas();

		guardar(cliente, tramos("limitado", null, "bueno")).andExpect(status().isOk())
				.andExpect(jsonPath("$.tobillo.tramo").value("limitado"))
				.andExpect(jsonPath("$.tobillo.hechoEn").isNotEmpty()).andExpect(jsonPath("$.cadera").doesNotExist())
				.andExpect(jsonPath("$.hombro.tramo").value("bueno"));
		consultar(RUTA, cliente).andExpect(jsonPath("$.tobillo.tramo").value("limitado"))
				.andExpect(jsonPath("$.hombro.tramo").value("bueno"));
		assertThat(filas(cliente)).isEqualTo(2);

		guardar(cliente, tramos("medio", "medio", null)).andExpect(status().isOk());
		consultar(RUTA, cliente).andExpect(jsonPath("$.tobillo.tramo").value("medio"))
				.andExpect(jsonPath("$.cadera.tramo").value("medio")).andExpect(jsonPath("$.hombro").doesNotExist());
		assertThat(filas(cliente)).isEqualTo(2);
	}

	@Test
	void repetirElMismoTramoNoCambiaSuFecha() throws Exception {
		Cliente cliente = clienteConMedidas();
		guardar(cliente, tramos("limitado", "bueno", null)).andExpect(status().isOk());
		String antes = cuerpoDe(consultar(RUTA, cliente)).get("tobillo").get("hechoEn").asString();

		reloj.avanzar(Duration.ofMinutes(1));
		guardar(cliente, tramos("limitado", "medio", null)).andExpect(status().isOk());

		var despues = cuerpoDe(consultar(RUTA, cliente));
		assertThat(despues.get("tobillo").get("hechoEn").asString()).isEqualTo(antes);
		assertThat(despues.get("cadera").get("hechoEn").asString()).isNotEqualTo(antes);
	}

	@Test
	void rechazaUnTramoQueNoExiste() throws Exception {
		Cliente cliente = clienteConMedidas();

		guardar(cliente, tramos("excelente", null, null)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", containsString("limitado, medio o bueno")));
		assertThat(filas(cliente)).isZero();
	}

	@Test
	void retirarElConsentimientoDeMedidasBorraLosTests() throws Exception {
		Cliente cliente = clienteConMedidas();
		guardar(cliente, tramos("limitado", "medio", "bueno")).andExpect(status().isOk());

		borrarRecurso(RUTA_CONSENTIMIENTOS + "/" + MEDIDAS, cliente).andExpect(status().isNoContent());

		assertThat(filas(cliente)).isZero();
		consultar(RUTA, cliente).andExpect(status().isForbidden());
	}

	@Test
	void cadaUsuarioVeSoloLosSuyos() throws Exception {
		Cliente uno = clienteConMedidas();
		Cliente otro = clienteConMedidas();

		guardar(uno, tramos("limitado", "limitado", "limitado")).andExpect(status().isOk());

		consultar(RUTA, otro).andExpect(jsonPath("$.tobillo").doesNotExist());
	}

	@Test
	void sinSesionNoHayAcceso() throws Exception {
		mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
		mvc.perform(put(RUTA).contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(tramos("bueno", null, null)))).andExpect(status().isUnauthorized());
	}

	@Test
	void generarElPlanUsaElTramoYLoExplicaEnElEjercicio() throws Exception {
		Cliente cliente = clienteConMedidas();
		guardar(cliente, tramos("limitado", null, "limitado")).andExpect(status().isOk());

		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("diasPorSemana", 4);
		cuerpo.put("minutosPorSesion", 60);
		cuerpo.put("material", MATERIAL_COMPLETO);
		enviarAutenticado(RUTA_GENERAR, cliente, cuerpo).andExpect(status().isOk())
				.andExpect(jsonPath("$.dias[*].ejercicios[*].anotaciones[*]",
						hasItem(containsString("tu tramo de movilidad de tobillo"))))
				.andExpect(jsonPath("$.dias[*].ejercicios[*].idExterno", hasItem("Goblet_Squat")));
	}

	@Test
	void sinConsentimientoElPlanNoUsaLosTramos() throws Exception {
		Cliente cliente = clienteConMedidas();
		guardar(cliente, tramos("limitado", "limitado", "limitado")).andExpect(status().isOk());
		borrarRecurso(RUTA_CONSENTIMIENTOS + "/" + MEDIDAS, cliente).andExpect(status().isNoContent());

		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("diasPorSemana", 4);
		cuerpo.put("minutosPorSesion", 60);
		cuerpo.put("material", List.copyOf(MATERIAL_COMPLETO));
		String plan = enviarAutenticado(RUTA_GENERAR, cliente, cuerpo).andExpect(status().isOk()).andReturn()
				.getResponse().getContentAsString();
		assertThat(plan).doesNotContain("tramo de movilidad");
	}
}
