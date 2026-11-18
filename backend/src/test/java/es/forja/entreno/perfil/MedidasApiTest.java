package es.forja.entreno.perfil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import es.forja.restricciones.api.SoporteLimitaciones;

/** Altura, peso y circunferencias: con el consentimiento de medidas de salud, cifradas, y con el % de grasa calculado. */
class MedidasApiTest extends SoporteLimitaciones {

	private static final String RUTA = "/api/v1/perfil/medidas";
	private static final String RUTA_DATOS = "/api/v1/perfil/datos";
	private static final String MEDIDAS = "medidas_salud";

	private static Map<String, Object> medidas(Object altura, Object peso, Object cuello, Object cintura,
			Object cadera) {
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("alturaCm", altura);
		cuerpo.put("pesoKg", peso);
		cuerpo.put("cuelloCm", cuello);
		cuerpo.put("cinturaCm", cintura);
		cuerpo.put("caderaCm", cadera);
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

	private void indicarSexo(Cliente cliente, String sexo) throws Exception {
		actualizar(RUTA_DATOS, cliente, Map.of("sexo", sexo)).andExpect(status().isOk());
	}

	private long filas(Cliente cliente) {
		return contar("SELECT COUNT(*) FROM perfil_medidas m JOIN usuario u ON u.id = m.usuario_id WHERE u.correo = ?",
				cliente.correo());
	}

	@Test
	void sinConsentimientoDeMedidasNoSeLeeNiSeGuarda() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		consultar(RUTA, cliente).andExpect(status().isForbidden());
		guardar(cliente, medidas(180.0, 75.0, null, null, null)).andExpect(status().isForbidden());
		assertThat(filas(cliente)).isZero();
	}

	@Test
	void sinDatosTodoVaANuloYNoHayEstimacion() throws Exception {
		Cliente cliente = clienteConMedidas();

		consultar(RUTA, cliente).andExpect(status().isOk()).andExpect(jsonPath("$.alturaCm").doesNotExist())
				.andExpect(jsonPath("$.pesoKg").doesNotExist()).andExpect(jsonPath("$.grasaEstimada").doesNotExist());
	}

	@Test
	void guardaLasMedidasYCalculaElPorcentajeDeGrasaSinGuardarlo() throws Exception {
		Cliente cliente = clienteConMedidas();
		indicarSexo(cliente, "hombre");

		guardar(cliente, medidas(180.0, 78.5, 38.0, 85.0, null)).andExpect(status().isOk())
				.andExpect(jsonPath("$.alturaCm").value(180.0)).andExpect(jsonPath("$.pesoKg").value(78.5))
				.andExpect(jsonPath("$.cuelloCm").value(38.0)).andExpect(jsonPath("$.cinturaCm").value(85.0))
				.andExpect(jsonPath("$.caderaCm").doesNotExist())
				.andExpect(jsonPath("$.grasaEstimada.porcentaje").value(16.1))
				.andExpect(jsonPath("$.grasaEstimada.margen").value(4.0));
		consultar(RUTA, cliente).andExpect(jsonPath("$.grasaEstimada.porcentaje").value(16.1));
		assertThat(filas(cliente)).isEqualTo(1);
	}

	@Test
	void laMujerNecesitaLaCaderaParaLaEstimacion() throws Exception {
		Cliente cliente = clienteConMedidas();
		indicarSexo(cliente, "mujer");

		guardar(cliente, medidas(165.0, 60.0, 32.0, 72.0, null)).andExpect(status().isOk())
				.andExpect(jsonPath("$.grasaEstimada").doesNotExist());
		guardar(cliente, medidas(165.0, 60.0, 32.0, 72.0, 98.0)).andExpect(status().isOk())
				.andExpect(jsonPath("$.grasaEstimada.porcentaje").value(27.4));
	}

	@Test
	void sinSexoIndicadoNoHayEstimacionPeroSiLasMedidas() throws Exception {
		Cliente cliente = clienteConMedidas();

		guardar(cliente, medidas(180.0, 78.5, 38.0, 85.0, null)).andExpect(status().isOk())
				.andExpect(jsonPath("$.pesoKg").value(78.5)).andExpect(jsonPath("$.grasaEstimada").doesNotExist());
	}

	@Test
	void guardarReemplazaTodasLasMedidas() throws Exception {
		Cliente cliente = clienteConMedidas();
		guardar(cliente, medidas(180.0, 78.5, 38.0, 85.0, 95.0)).andExpect(status().isOk());

		guardar(cliente, medidas(181.0, null, null, null, null)).andExpect(status().isOk());

		consultar(RUTA, cliente).andExpect(jsonPath("$.alturaCm").value(181.0))
				.andExpect(jsonPath("$.pesoKg").doesNotExist()).andExpect(jsonPath("$.cinturaCm").doesNotExist());
		assertThat(filas(cliente)).isEqualTo(1);
	}

	@Test
	void rechazaMedidasFueraDeRango() throws Exception {
		Cliente cliente = clienteConMedidas();

		guardar(cliente, medidas(99.0, null, null, null, null)).andExpect(status().isBadRequest());
		guardar(cliente, medidas(251.0, null, null, null, null)).andExpect(status().isBadRequest());
		guardar(cliente, medidas(null, 29.0, null, null, null)).andExpect(status().isBadRequest());
		guardar(cliente, medidas(null, 301.0, null, null, null)).andExpect(status().isBadRequest());
		guardar(cliente, medidas(null, null, 19.0, null, null)).andExpect(status().isBadRequest());
		guardar(cliente, medidas(null, null, null, 39.0, null)).andExpect(status().isBadRequest());
		guardar(cliente, medidas(null, null, null, null, 201.0)).andExpect(status().isBadRequest());
		assertThat(filas(cliente)).isZero();
	}

	@Test
	void laCinturaTieneQueSerMayorQueElCuello() throws Exception {
		Cliente cliente = clienteConMedidas();

		guardar(cliente, medidas(180.0, null, 40.0, 40.0, null)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", containsString("cintura")));
		assertThat(filas(cliente)).isZero();
	}

	@Test
	void lasMedidasSeGuardanCifradasEnLaBase() throws Exception {
		Cliente cliente = clienteConMedidas();
		guardar(cliente, medidas(183.0, 77.5, 39.0, 86.0, 97.0)).andExpect(status().isOk());

		byte[] datos = jdbc.sql("""
				SELECT m.datos FROM perfil_medidas m JOIN usuario u ON u.id = m.usuario_id WHERE u.correo = :c""")
				.param("c", cliente.correo()).query(byte[].class).single();

		String enClaro = new String(datos, StandardCharsets.ISO_8859_1);
		assertThat(enClaro).doesNotContain("alturaCm").doesNotContain("183").doesNotContain("77.5");
	}

	@Test
	void retirarElConsentimientoDeMedidasLasBorra() throws Exception {
		Cliente cliente = clienteConMedidas();
		guardar(cliente, medidas(180.0, 78.5, null, null, null)).andExpect(status().isOk());

		borrarRecurso(RUTA_CONSENTIMIENTOS + "/" + MEDIDAS, cliente).andExpect(status().isNoContent());

		assertThat(filas(cliente)).isZero();
		consultar(RUTA, cliente).andExpect(status().isForbidden());
		// Volver a darlo no recupera nada.
		darConsentimiento(cliente, MEDIDAS).andExpect(status().isOk());
		consultar(RUTA, cliente).andExpect(jsonPath("$.alturaCm").doesNotExist());
	}

	@Test
	void cadaUsuarioVeSoloLasSuyas() throws Exception {
		Cliente uno = clienteConMedidas();
		Cliente otro = clienteConMedidas();

		guardar(uno, medidas(180.0, 78.5, null, null, null)).andExpect(status().isOk());

		consultar(RUTA, otro).andExpect(jsonPath("$.alturaCm").doesNotExist());
	}

	@Test
	void sinSesionNoHayAcceso() throws Exception {
		mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
		mvc.perform(put(RUTA).contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(medidas(180.0, null, null, null, null))))
				.andExpect(status().isUnauthorized());
	}
}
