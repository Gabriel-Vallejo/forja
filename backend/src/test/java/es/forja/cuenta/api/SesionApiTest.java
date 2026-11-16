package es.forja.cuenta.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

class SesionApiTest extends ApiCuentaSoporte {

	private static final String LOGIN = "/api/v1/auth/login";
	private static final String REFRESCO = "/api/v1/auth/refresco";
	private static final String DISPOSITIVOS = "/api/v1/dispositivos";

	private ResultActions refrescar(String token) throws Exception {
		return enviar(REFRESCO, Map.of("tokenRefresco", token));
	}

	@Test
	void entrarDevuelveAccesoRefrescoYDaDeAltaElDispositivo() throws Exception {
		String correo = crearCuenta();

		Cliente cliente = iniciarSesion(correo, "Pixel de Gabriel", null);

		assertThat(cliente.tokenAcceso().split("\\.")).hasSize(3);
		assertThat(cliente.tokenRefresco()).hasSizeGreaterThanOrEqualTo(43);
		consultar(DISPOSITIVOS, cliente).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(cliente.dispositivo()))
				.andExpect(jsonPath("$[0].nombre").value("Pixel de Gabriel"))
				.andExpect(jsonPath("$[0].plataforma").value("movil"))
				.andExpect(jsonPath("$[0].actual").value(true));
	}

	@Test
	void laBaseGuardaDelRefrescoSoloSuHashSha256() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		long enClaro = contar("SELECT COUNT(*) FROM token_refresco WHERE hash = ?", cliente.tokenRefresco().getBytes());
		long total = contar("SELECT COUNT(*) FROM token_refresco t JOIN dispositivo d ON d.id = t.dispositivo_id "
				+ "WHERE d.uuid = UNHEX(REPLACE(?, '-', '')) AND LENGTH(t.hash) = 32", cliente.dispositivo());

		assertThat(enClaro).isZero();
		assertThat(total).isEqualTo(1);
	}

	@Test
	void unUsuarioSinVerificarRecibeElMismo401QueUnaContrasenaMala() throws Exception {
		String correo = crearCuentaSinVerificar();

		var sinVerificar = enviar(LOGIN, cuerpoLogin(correo, CONTRASENA, "Portátil", null))
				.andExpect(status().isUnauthorized());
		var mala = enviar(LOGIN, cuerpoLogin(correo, "contrasena-equivocada-1", "Portátil", null))
				.andExpect(status().isUnauthorized());

		assertThat(sinVerificar.andReturn().getResponse().getContentAsString())
				.isEqualTo(mala.andReturn().getResponse().getContentAsString());
		assertThat(contar("SELECT COUNT(*) FROM dispositivo d JOIN usuario u ON u.id = d.usuario_id "
				+ "WHERE u.correo = ?", correo)).isZero();
	}

	@Test
	void trasVerificarElCorreoYaPuedeEntrar() throws Exception {
		String correo = crearCuentaSinVerificar();
		verificarCorreo(correo);

		iniciarSesion(correo, "Portátil", null);
	}

	@Test
	void unaContrasenaMalaYUnCorreoInexistenteDanLaMismaRespuesta() throws Exception {
		String correo = crearCuenta();

		var mala = enviar(LOGIN, cuerpoLogin(correo, "contrasena-equivocada-1", "Pixel", null))
				.andExpect(status().isUnauthorized());
		var inexistente = enviar(LOGIN, cuerpoLogin(correoNuevo(), "contrasena-equivocada-1", "Pixel", null))
				.andExpect(status().isUnauthorized());

		assertThat(mala.andReturn().getResponse().getContentAsString())
				.isEqualTo(inexistente.andReturn().getResponse().getContentAsString());
	}

	@Test
	void entrarOtraVezConSuSidReutilizaElDispositivoYDejaUnSoloRefrescoVivo() throws Exception {
		String correo = crearCuenta();
		Cliente primero = iniciarSesion(correo, "Pixel", null);

		Cliente segundo = iniciarSesion(correo, "Pixel renombrado", primero.dispositivo());

		assertThat(segundo.dispositivo()).isEqualTo(primero.dispositivo());
		consultar(DISPOSITIVOS, segundo).andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].nombre").value("Pixel renombrado"));
		assertThat(contar("SELECT COUNT(*) FROM token_refresco t JOIN dispositivo d ON d.id = t.dispositivo_id "
				+ "WHERE d.uuid = UNHEX(REPLACE(?, '-', '')) AND t.usado_en IS NULL AND t.revocado_en IS NULL",
				primero.dispositivo())).isEqualTo(1);
		refrescar(primero.tokenRefresco()).andExpect(status().isUnauthorized());
	}

	@Test
	void unSidAjenoONoExistenteNoSeAceptaYSeCreaUnDispositivoNuevo() throws Exception {
		Cliente ajeno = cuentaConSesion("Del otro");
		String correo = crearCuenta();

		Cliente propio = iniciarSesion(correo, "Mío", ajeno.dispositivo());
		Cliente inventado = iniciarSesion(correo, "Mío 2", UUID.randomUUID().toString());

		assertThat(propio.dispositivo()).isNotEqualTo(ajeno.dispositivo());
		assertThat(inventado.dispositivo()).isNotIn(propio.dispositivo(), ajeno.dispositivo());
		consultar(DISPOSITIVOS, ajeno).andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void elRefrescoSeRotaYElTokenNuevoSigueValiendo() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		ResultActions rotado = refrescar(cliente.tokenRefresco()).andExpect(status().isOk());
		String nuevoRefresco = cuerpoDe(rotado).get("tokenRefresco").asString();
		String nuevoAcceso = cuerpoDe(rotado).get("tokenAcceso").asString();

		assertThat(nuevoRefresco).isNotEqualTo(cliente.tokenRefresco());
		assertThat(cuerpoDe(rotado).get("dispositivoId").asString()).isEqualTo(cliente.dispositivo());
		consultar(DISPOSITIVOS, new Cliente(cliente.correo(), nuevoAcceso, nuevoRefresco, cliente.dispositivo()))
				.andExpect(status().isOk());
		refrescar(nuevoRefresco).andExpect(status().isOk());
	}

	@Test
	void unRefrescoYaUsadoRevocaLaFamiliaYElDispositivo() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");
		String segundoRefresco = cuerpoDe(refrescar(cliente.tokenRefresco()).andExpect(status().isOk()))
				.get("tokenRefresco").asString();

		// Alguien repite el primero, que ya se había cambiado: se da por robado.
		refrescar(cliente.tokenRefresco()).andExpect(status().isUnauthorized());

		refrescar(segundoRefresco).andExpect(status().isUnauthorized());
		consultar(DISPOSITIVOS, cliente).andExpect(status().isUnauthorized());
		assertThat(contar("SELECT COUNT(*) FROM token_refresco t JOIN dispositivo d ON d.id = t.dispositivo_id "
				+ "WHERE d.uuid = UNHEX(REPLACE(?, '-', '')) AND t.revocado_en IS NULL", cliente.dispositivo()))
				.isZero();
		assertThat(contar("SELECT COUNT(*) FROM dispositivo WHERE uuid = UNHEX(REPLACE(?, '-', '')) "
				+ "AND revocado_en IS NOT NULL", cliente.dispositivo())).isEqualTo(1);
	}

	@Test
	void unRefrescoInventadoDa401SinRevocarNada() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		refrescar("no-existe").andExpect(status().isUnauthorized());

		refrescar(cliente.tokenRefresco()).andExpect(status().isOk());
	}

	@Test
	void elRefrescoCaducaALos30Dias() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		reloj.avanzar(Duration.ofDays(30).plusMinutes(1));

		refrescar(cliente.tokenRefresco()).andExpect(status().isUnauthorized());
	}

	@Test
	void elTokenDeAccesoCaducaALosQuinceMinutos() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		reloj.avanzar(Duration.ofMinutes(14));
		consultar(DISPOSITIVOS, cliente).andExpect(status().isOk());
		reloj.avanzar(Duration.ofMinutes(2));

		consultar(DISPOSITIVOS, cliente).andExpect(status().isUnauthorized());
	}

	@Test
	void cerrarSesionInvalidaElRefrescoYElAccesoDeEseDispositivo() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		enviarAutenticado("/api/v1/auth/cerrar-sesion", cliente, Map.of()).andExpect(status().isNoContent());

		refrescar(cliente.tokenRefresco()).andExpect(status().isUnauthorized());
		consultar(DISPOSITIVOS, cliente).andExpect(status().isUnauthorized());
	}

	@Test
	void cerrarSesionSinTokenDa401() throws Exception {
		enviar("/api/v1/auth/cerrar-sesion", Map.of()).andExpect(status().isUnauthorized());
	}

	@Test
	void revocarDesdeOtroDispositivoCierraSoloEse() throws Exception {
		String correo = crearCuenta();
		Cliente movil = iniciarSesion(correo, "Móvil", null);
		Cliente pc = iniciarSesion(correo, "PC", null);

		borrarRecurso(DISPOSITIVOS + "/" + pc.dispositivo(), movil).andExpect(status().isNoContent());

		consultar(DISPOSITIVOS, pc).andExpect(status().isUnauthorized());
		refrescar(pc.tokenRefresco()).andExpect(status().isUnauthorized());
		consultar(DISPOSITIVOS, movil).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
		refrescar(movil.tokenRefresco()).andExpect(status().isOk());
	}

	@Test
	void revocarUnDispositivoAjenoOInexistenteDa404() throws Exception {
		Cliente yo = cuentaConSesion("Mío");
		Cliente otro = cuentaConSesion("Ajeno");

		borrarRecurso(DISPOSITIVOS + "/" + otro.dispositivo(), yo).andExpect(status().isNotFound());
		borrarRecurso(DISPOSITIVOS + "/" + UUID.randomUUID(), yo).andExpect(status().isNotFound());

		consultar(DISPOSITIVOS, otro).andExpect(status().isOk());
	}

	@Test
	void ningunaRespuestaDeLoginPonePistasNiCookies() throws Exception {
		String correo = crearCuenta();

		ResultActions login = enviar(LOGIN, cuerpoLogin(correo, CONTRASENA, "Pixel", null));

		assertThat(login.andReturn().getResponse().getHeaders("Set-Cookie")).isEmpty();
	}
}
