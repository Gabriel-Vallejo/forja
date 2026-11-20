package es.forja.restricciones.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import es.forja.restricciones.servicio.ServicioRestricciones;
import tools.jackson.databind.JsonNode;

@ExtendWith(OutputCaptureExtension.class)
class RestriccionesApiTest extends SoporteLimitaciones {

	private static final String TEXTO_ORIGINAL = "me crujen mucho las escaleras de casa";

	@Test
	void creaListaEditaYBorra() throws Exception {
		Cliente cliente = clienteConLimitaciones();

		String uuid = crearRestriccion(cliente, restriccion("rodilla", "sentadilla", "sin_carga_axial"));

		consultar(RUTA_RESTRICCIONES, cliente).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].uuid").value(uuid)).andExpect(jsonPath("$[0].region").value("rodilla"))
				.andExpect(jsonPath("$[0].lado").value("ambos"))
				.andExpect(jsonPath("$[0].patron").value("sentadilla"))
				.andExpect(jsonPath("$[0].tipo").value("sin_carga_axial"))
				.andExpect(jsonPath("$[0].estado").value("actual")).andExpect(jsonPath("$[0].origen").value("usuario"));

		actualizar(RUTA_RESTRICCIONES + "/" + uuid, cliente, restriccion("cadera", "bisagra", "evitar"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.uuid").value(uuid))
				.andExpect(jsonPath("$.tipo").value("evitar"));
		consultar(RUTA_RESTRICCIONES, cliente).andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].region").value("cadera")).andExpect(jsonPath("$[0].patron").value("bisagra"));

		borrarRecurso(RUTA_RESTRICCIONES + "/" + uuid, cliente).andExpect(status().isNoContent());
		consultar(RUTA_RESTRICCIONES, cliente).andExpect(jsonPath("$", hasSize(0)));
		borrarRecurso(RUTA_RESTRICCIONES + "/" + uuid, cliente).andExpect(status().isNotFound());
	}

	@Test
	void laColumnaDatosNoLlevaNadaEnClaro() throws Exception {
		Cliente cliente = clienteConLimitaciones();
		Map<String, Object> cuerpo = restriccion("rodilla", "sentadilla", "sin_carga_axial");
		cuerpo.put("textoOriginal", TEXTO_ORIGINAL);
		crearRestriccion(cliente, cuerpo);

		List<byte[]> filas = jdbc.sql("""
				SELECT r.datos FROM restriccion r JOIN usuario u ON u.id = r.usuario_id WHERE u.correo = :c""")
				.param("c", cliente.correo()).query((rs, n) -> rs.getBytes(1)).list();

		assertThat(filas).hasSize(1);
		String enClaro = new String(filas.get(0), StandardCharsets.ISO_8859_1);
		assertThat(enClaro).doesNotContain("sin_carga_axial").doesNotContain("rodilla").doesNotContain("sentadilla")
				.doesNotContain("crujen").doesNotContain("escaleras");
	}

	@Test
	void unaRestriccionAjenaDa404EnEdicionYBorrado() throws Exception {
		Cliente dueno = clienteConLimitaciones();
		Cliente otro = clienteConLimitaciones();
		String uuid = crearRestriccion(dueno, restriccion("rodilla", "sentadilla", "evitar"));

		actualizar(RUTA_RESTRICCIONES + "/" + uuid, otro, restriccion("cadera", "bisagra", "evitar"))
				.andExpect(status().isNotFound());
		borrarRecurso(RUTA_RESTRICCIONES + "/" + uuid, otro).andExpect(status().isNotFound());

		consultar(RUTA_RESTRICCIONES, otro).andExpect(jsonPath("$", hasSize(0)));
		consultar(RUTA_RESTRICCIONES, dueno).andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].region").value("rodilla"));
	}

	@Test
	void unIdentificadorQueNoEsUnUuidDa404() throws Exception {
		Cliente cliente = clienteConLimitaciones();

		borrarRecurso(RUTA_RESTRICCIONES + "/no-es-un-uuid", cliente).andExpect(status().is4xxClientError());
	}

	@Test
	void elLimiteDeRestriccionesPorUsuarioDa409() throws Exception {
		Cliente cliente = clienteConLimitaciones();
		for (int i = 0; i < ServicioRestricciones.MAXIMO_POR_USUARIO; i++) {
			crearRestriccion(cliente, restriccion("rodilla", "sentadilla", "evitar"));
		}

		enviarAutenticado(RUTA_RESTRICCIONES, cliente, restriccion("cadera", "bisagra", "evitar"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));

		consultar(RUTA_RESTRICCIONES, cliente)
				.andExpect(jsonPath("$", hasSize(ServicioRestricciones.MAXIMO_POR_USUARIO)));
	}

	@Test
	void sinElConsentimientoDeLimitacionesTodasLasRutasDan403() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");
		String uuid = java.util.UUID.randomUUID().toString();

		enviarAutenticado("/api/v1/cribado", cliente, Map.of()).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.detail").value(containsString("limitaciones")));
		enviarAutenticado("/api/v1/banderas", cliente, Map.of()).andExpect(status().isForbidden());
		consultar(RUTA_RESTRICCIONES, cliente).andExpect(status().isForbidden());
		enviarAutenticado(RUTA_RESTRICCIONES, cliente, restriccion("rodilla", "sentadilla", "evitar"))
				.andExpect(status().isForbidden());
		actualizar(RUTA_RESTRICCIONES + "/" + uuid, cliente, restriccion("rodilla", "sentadilla", "evitar"))
				.andExpect(status().isForbidden());
		borrarRecurso(RUTA_RESTRICCIONES + "/" + uuid, cliente).andExpect(status().isForbidden());
		consultar(RUTA_RESTRICCIONES + "/efecto", cliente).andExpect(status().isForbidden());
	}

	@Test
	void conElConsentimientoLasRutasFuncionan() throws Exception {
		Cliente cliente = clienteConLimitaciones();

		consultar(RUTA_RESTRICCIONES, cliente).andExpect(status().isOk());
		consultar(RUTA_RESTRICCIONES + "/efecto", cliente).andExpect(status().isOk());
	}

	@Test
	void sinSesionLasRutasDan401() throws Exception {
		org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder sinToken = org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.get(RUTA_RESTRICCIONES);

		mvc.perform(sinToken).andExpect(status().isUnauthorized());
	}

	@Test
	void revocarLimitacionesBorraLasRestriccionesYVolverADarloNoLasResucita() throws Exception {
		Cliente cliente = clienteConLimitaciones();
		crearRestriccion(cliente, restriccion("rodilla", "sentadilla", "evitar"));
		crearRestriccion(cliente, restriccion("hombro", "empuje_vertical", "sin_por_encima_cabeza"));

		borrarRecurso("/api/v1/consentimientos/limitaciones", cliente).andExpect(status().isNoContent());

		assertThat(contar("SELECT COUNT(*) FROM restriccion r JOIN usuario u ON u.id = r.usuario_id "
				+ "WHERE u.correo = ?", cliente.correo())).isZero();
		consultar(RUTA_RESTRICCIONES, cliente).andExpect(status().isForbidden());

		darConsentimiento(cliente, "limitaciones").andExpect(status().isOk());
		consultar(RUTA_RESTRICCIONES, cliente).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void revocarOtroConsentimientoNoTocaLasRestricciones() throws Exception {
		Cliente cliente = clienteConLimitaciones();
		darConsentimiento(cliente, "suplementos").andExpect(status().isOk());
		crearRestriccion(cliente, restriccion("rodilla", "sentadilla", "evitar"));

		borrarRecurso("/api/v1/consentimientos/suplementos", cliente).andExpect(status().isNoContent());

		consultar(RUTA_RESTRICCIONES, cliente).andExpect(jsonPath("$", hasSize(1)));
	}

	@Test
	void losDatosNoValidosDan400ConProblemDetailYSinRepetirElValor() throws Exception {
		Cliente cliente = clienteConLimitaciones();
		Map<String, Object> regionRara = restriccion("zona-inventada-xyz", "sentadilla", "evitar");
		Map<String, Object> sinRegionNiPatron = restriccion(null, null, "evitar");
		Map<String, Object> sinLado = restriccion("rodilla", "sentadilla", "evitar");
		sinLado.put("lado", null);
		Map<String, Object> textoLargo = restriccion("rodilla", "sentadilla", "evitar");
		textoLargo.put("textoOriginal", "x".repeat(ServicioRestricciones.MAXIMO_TEXTO_ORIGINAL + 1));

		for (Map<String, Object> cuerpo : List.of(regionRara, sinRegionNiPatron, sinLado, textoLargo)) {
			enviarAutenticado(RUTA_RESTRICCIONES, cliente, cuerpo).andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.trace").doesNotExist());
		}
		consultar(RUTA_RESTRICCIONES, cliente).andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void unaEdicionInvalidaNoCambiaLaRestriccionGuardada() throws Exception {
		Cliente cliente = clienteConLimitaciones();
		String uuid = crearRestriccion(cliente, restriccion("rodilla", "sentadilla", "evitar"));

		actualizar(RUTA_RESTRICCIONES + "/" + uuid, cliente, restriccion(null, null, "evitar"))
				.andExpect(status().isBadRequest());

		consultar(RUTA_RESTRICCIONES, cliente).andExpect(jsonPath("$[0].region").value("rodilla"));
	}

	@Test
	void efectoSustituyeLaSentadillaConBarraSiNoHayCargaAxial() throws Exception {
		Cliente cliente = clienteConLimitaciones();
		crearRestriccion(cliente, restriccion(null, "sentadilla", "sin_carga_axial"));

		JsonNode efecto = cuerpoDe(consultar(RUTA_RESTRICCIONES + "/efecto", cliente).andExpect(status().isOk()));

		JsonNode sentadilla = null;
		java.util.List<String> ids = new java.util.ArrayList<>();
		for (JsonNode e : efecto) {
			ids.add(e.get("idExterno").asString());
			assertThat(e.get("motivo").asString()).matches("\\p{Lu}.*");
			assertThat(e.get("decision").asString()).isIn("excluido", "sustituido", "con_anotaciones");
			assertThat(e.get("idExterno").asString()).isNotEqualTo("Leg_Press");
			if ("Barbell_Squat".equals(e.get("idExterno").asString())) {
				sentadilla = e;
			}
		}
		assertThat(sentadilla).isNotNull();
		// Solo lo que el generador puede poner en el plan: los curados, todos con nombre en español.
		assertThat(jdbc.sql("SELECT DISTINCT origen_etiquetado FROM ejercicio WHERE id_externo IN (:ids)")
				.param("ids", ids).query(String.class).list()).containsExactly("curado");
		assertThat(sentadilla.get("nombre").asString()).isEqualTo("Sentadilla con barra");
		assertThat(sentadilla.get("decision").asString()).isEqualTo("sustituido");
		assertThat(sentadilla.get("sustituto").get("idExterno").asString()).isEqualTo("Leg_Press");
		assertThat(sentadilla.get("motivo").asString())
				.isEqualTo("Sin carga axial en sentadilla: prensa de piernas en vez de sentadilla con barra");
	}

	@Test
	void sinRestriccionesElEfectoEstaVacio() throws Exception {
		Cliente cliente = clienteConLimitaciones();

		consultar(RUTA_RESTRICCIONES + "/efecto", cliente).andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void ningunaRespuestaNiPeticionDejaRastroEnElLog(CapturedOutput salida) throws Exception {
		Cliente cliente = clienteConLimitaciones();
		Map<String, Object> cuerpo = restriccion("rodilla", "sentadilla", "sin_carga_axial");
		cuerpo.put("textoOriginal", TEXTO_ORIGINAL);
		String uuid = crearRestriccion(cliente, cuerpo);
		consultar(RUTA_RESTRICCIONES, cliente);
		consultar(RUTA_RESTRICCIONES + "/efecto", cliente);
		enviarAutenticado(RUTA_RESTRICCIONES, cliente, restriccion("zona-inventada-xyz", null, "evitar"));
		actualizar(RUTA_RESTRICCIONES + "/" + uuid, cliente, restriccion("rodilla", "sentadilla", "evitar"));

		assertThat(salida.getAll()).doesNotContain("crujen").doesNotContain("sin_carga_axial")
				.doesNotContain("zona-inventada-xyz");
	}
}
