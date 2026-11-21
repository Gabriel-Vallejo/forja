package es.forja.consentimiento.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;

import es.forja.restricciones.api.SoporteLimitaciones;

class ConsentimientosApiTest extends SoporteLimitaciones {

	private long filas(Cliente cliente, String tipo, boolean soloVivos) {
		return contar("SELECT COUNT(*) FROM consentimiento c JOIN usuario u ON u.id = c.usuario_id "
				+ "WHERE u.correo = ? AND c.tipo = ?" + (soloVivos ? " AND c.revocado_en IS NULL" : ""),
				cliente.correo(), tipo);
	}

	@Test
	void sinConsentimientosLaListaEstaVacia() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		consultar(RUTA_CONSENTIMIENTOS, cliente).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void darUnConsentimientoLoListaConSuVersion() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		darConsentimiento(cliente, "lesiones").andExpect(status().isOk())
				.andExpect(jsonPath("$.tipo").value("lesiones"))
				.andExpect(jsonPath("$.versionTexto").value(VERSION_VIGENTE));

		consultar(RUTA_CONSENTIMIENTOS, cliente).andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].tipo").value("lesiones"))
				.andExpect(jsonPath("$[0].versionTexto").value(VERSION_VIGENTE))
				.andExpect(jsonPath("$[0].dadoEn").isNotEmpty());
	}

	@Test
	void elDeLimitacionesVaPorLaV2YConElTextoViejoNoVale() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		enviarAutenticado(RUTA_CONSENTIMIENTOS, cliente, Map.of("tipo", "limitaciones", "versionTexto", "v1"))
				.andExpect(status().isConflict());
		darConsentimiento(cliente, "limitaciones").andExpect(status().isOk())
				.andExpect(jsonPath("$.versionTexto").value("v2"));
	}

	@Test
	void darloDosVecesEsIdempotente() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		darConsentimiento(cliente, "limitaciones").andExpect(status().isOk());
		darConsentimiento(cliente, "limitaciones").andExpect(status().isOk());

		assertThat(filas(cliente, "limitaciones", false)).isEqualTo(1);
	}

	@Test
	void unaVersionDeTextoViejaDa409YNoGuardaNada() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		enviarAutenticado(RUTA_CONSENTIMIENTOS, cliente, Map.of("tipo", "limitaciones", "versionTexto", "v0"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));

		assertThat(filas(cliente, "limitaciones", false)).isZero();
	}

	@Test
	void unTipoDesconocidoOUnCuerpoIncompletoDa400() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		enviarAutenticado(RUTA_CONSENTIMIENTOS, cliente, Map.of("tipo", "inventado", "versionTexto", VERSION_VIGENTE))
				.andExpect(status().isBadRequest());
		enviarAutenticado(RUTA_CONSENTIMIENTOS, cliente, Map.of("tipo", "limitaciones"))
				.andExpect(status().isBadRequest());
		borrarRecurso(RUTA_CONSENTIMIENTOS + "/inventado", cliente).andExpect(status().isBadRequest());
	}

	@Test
	void revocarGuardaLaFechaYLoQuitaDeLaLista() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");
		darConsentimiento(cliente, "suplementos").andExpect(status().isOk());

		borrarRecurso(RUTA_CONSENTIMIENTOS + "/suplementos", cliente).andExpect(status().isNoContent());

		consultar(RUTA_CONSENTIMIENTOS, cliente).andExpect(jsonPath("$", hasSize(0)));
		assertThat(filas(cliente, "suplementos", false)).isEqualTo(1);
		assertThat(filas(cliente, "suplementos", true)).isZero();
	}

	@Test
	void revocarUnoQueNoEstabaDadoEsIdempotente() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		borrarRecurso(RUTA_CONSENTIMIENTOS + "/suplementos", cliente).andExpect(status().isNoContent());
	}

	@Test
	void volverADarloTrasRevocarCreaUnoNuevoYConservaElHistorial() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");
		darConsentimiento(cliente, "fotos_cuerpo").andExpect(status().isOk());
		borrarRecurso(RUTA_CONSENTIMIENTOS + "/fotos_cuerpo", cliente).andExpect(status().isNoContent());

		darConsentimiento(cliente, "fotos_cuerpo").andExpect(status().isOk());

		assertThat(filas(cliente, "fotos_cuerpo", false)).isEqualTo(2);
		assertThat(filas(cliente, "fotos_cuerpo", true)).isEqualTo(1);
	}

	@Test
	void cadaUsuarioSoloVeLosSuyos() throws Exception {
		Cliente uno = cuentaConSesion("Pixel");
		Cliente otro = cuentaConSesion("PC");
		darConsentimiento(uno, "medidas_salud").andExpect(status().isOk());

		consultar(RUTA_CONSENTIMIENTOS, otro).andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void sinSesionDa401() throws Exception {
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(RUTA_CONSENTIMIENTOS))
				.andExpect(status().isUnauthorized());
	}
}
