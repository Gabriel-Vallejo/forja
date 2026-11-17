package es.forja.cuenta.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import es.forja.ClavesDePrueba;
import es.forja.JwtDePrueba;

class SeguridadHttpTest extends ApiCuentaSoporte {

	private static final String PROTEGIDA = "/api/v1/dispositivos";

	private ResultActions conBearer(String token) throws Exception {
		return mvc.perform(get(PROTEGIDA).header("Authorization", "Bearer " + token));
	}

	/** Token con los sub y sid reales del cliente, para que solo falle lo que se quiere probar. */
	private String forjar(Cliente cliente, String alg, String kid, KeyPair clave, long exp) {
		String[] partes = cliente.tokenAcceso().split("\\.");
		String carga = new String(java.util.Base64.getUrlDecoder().decode(partes[1]));
		var nodo = json.readTree(carga);
		return JwtDePrueba.forjar(JwtDePrueba.cabecera(alg, kid),
				JwtDePrueba.carga(UUID.fromString(nodo.get("sub").asString()),
						UUID.fromString(nodo.get("sid").asString()), Instant.now().getEpochSecond(), exp),
				clave);
	}

	private static long enCincoMinutos() {
		return Instant.now().plusSeconds(300).getEpochSecond();
	}

	@Test
	void sinTokenLasRutasCerradasDan401ComoProblemDetailSinTrazas() throws Exception {
		mvc.perform(get(PROTEGIDA)).andExpect(status().isUnauthorized())
				.andExpect(header().string("WWW-Authenticate", "Bearer"))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.trace").doesNotExist());
		mvc.perform(get("/api/v1/ruta-que-no-existe")).andExpect(status().isUnauthorized());
	}

	@Test
	void elTokenDelLoginSirve() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		conBearer(cliente.tokenAcceso()).andExpect(status().isOk());
	}

	@Test
	void rechazaUnKidDesconocido() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		conBearer(forjar(cliente, "EdDSA", "kid-que-no-existe", ClavesDePrueba.ACTUAL, enCincoMinutos()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void rechazaUnAlgDistintoDeEdDsa() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		for (String alg : new String[] { "none", "HS256", "RS256" }) {
			conBearer(forjar(cliente, alg, ClavesDePrueba.KID_ACTUAL, ClavesDePrueba.ACTUAL, enCincoMinutos()))
					.andExpect(status().isUnauthorized());
		}
	}

	@Test
	void rechazaUnTokenCaducado() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		conBearer(forjar(cliente, "EdDSA", ClavesDePrueba.KID_ACTUAL, ClavesDePrueba.ACTUAL,
				Instant.now().minusSeconds(10).getEpochSecond())).andExpect(status().isUnauthorized());
	}

	@Test
	void rechazaUnTokenFirmadoConOtraClave() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");
		KeyPair intrusa = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();

		conBearer(forjar(cliente, "EdDSA", ClavesDePrueba.KID_ACTUAL, intrusa, enCincoMinutos()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void aceptaUnTokenFirmadoConLaClaveAnteriorMientrasSigueEnRotacion() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		conBearer(forjar(cliente, "EdDSA", ClavesDePrueba.KID_ANTERIOR, ClavesDePrueba.ANTERIOR, enCincoMinutos()))
				.andExpect(status().isOk());
	}

	@Test
	void rechazaUnTokenBienFirmadoDeUnDispositivoQueNoExiste() throws Exception {
		String token = JwtDePrueba.forjar(JwtDePrueba.cabecera("EdDSA", ClavesDePrueba.KID_ACTUAL),
				JwtDePrueba.carga(UUID.randomUUID(), UUID.randomUUID(), Instant.now().getEpochSecond(),
						enCincoMinutos()),
				ClavesDePrueba.ACTUAL);

		conBearer(token).andExpect(status().isUnauthorized());
	}

	@Test
	void unBearerRotoEnUnaRutaAbiertaNoImpideEntrar() throws Exception {
		String correo = crearCuenta();

		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
				.header("Authorization", "Bearer basura").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(cuerpoLogin(correo, CONTRASENA, "P", null))).with(p -> {
					p.setRemoteAddr(ip);
					return p;
				})).andExpect(status().isOk());
	}

	@Test
	void laSaludDelServicioEstaAbierta() throws Exception {
		mvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

	@Test
	void fueraDelPerfilDevNoSeSirveLaDocumentacionDeLaApi() throws Exception {
		mvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
		mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
	}

	@Test
	void corsAceptaLaWebYTauriYRechazaCualquierOtroOrigen() throws Exception {
		for (String origen : new String[] { "https://forja.test", "tauri://localhost", "http://tauri.localhost" }) {
			mvc.perform(options("/api/v1/auth/login").header("Origin", origen)
					.header("Access-Control-Request-Method", "POST")
					.header("Access-Control-Request-Headers", "authorization,content-type"))
					.andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", origen));
		}
		mvc.perform(options("/api/v1/auth/login").header("Origin", "https://malo.test")
				.header("Access-Control-Request-Method", "POST")).andExpect(status().isForbidden());
	}

	@Test
	void unaPeticionDeDatosMalosNoRepiteLaContrasenaNiDaTrazas() throws Exception {
		var resultado = enviar("/api/v1/auth/registro",
				cuerpoRegistro("no-es-correo", "contrasena-que-no-debe-salir", "1998-04-12"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores[0].campo").exists());

		String cuerpo = resultado.andReturn().getResponse().getContentAsString();
		assertThat(cuerpo).doesNotContain("contrasena-que-no-debe-salir").doesNotContain("Exception");
	}
}
