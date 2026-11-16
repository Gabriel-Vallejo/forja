package es.forja.cuenta.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import es.forja.ClavesDePrueba;

class RecuperacionApiTest extends ApiCuentaSoporte {

	@org.springframework.beans.factory.annotation.Autowired
	es.forja.cuenta.repositorio.TokenUnUsoRepositorio tokensUnUso;

	private static final String RECUPERAR = "/api/v1/auth/recuperar";
	private static final String RESTABLECER = "/api/v1/auth/restablecer";
	private static final String NUEVA = "otra-contrasena-larga-2";

	@Test
	void recuperarNoRevelaSiLaCuentaExisteMismoCodigoYMismoCuerpo() throws Exception {
		String existente = crearCuenta();
		buzon.vaciar();

		var conCuenta = enviar(RECUPERAR, Map.of("correo", existente)).andExpect(status().isAccepted());
		var sinCuenta = enviar(RECUPERAR, Map.of("correo", correoNuevo())).andExpect(status().isAccepted());

		assertThat(conCuenta.andReturn().getResponse().getContentAsString())
				.isEqualTo(sinCuenta.andReturn().getResponse().getContentAsString());
		assertThat(buzon.total()).isEqualTo(1);
		assertThat(buzon.ultimoToken(existente, "recuperacion")).isPresent();
	}

	@Test
	void restablecerCambiaLaContrasenaYCierraTodosLosDispositivos() throws Exception {
		String correo = crearCuenta();
		Cliente movil = iniciarSesion(correo, "Móvil", null);
		Cliente pc = iniciarSesion(correo, "PC", null);
		enviar(RECUPERAR, Map.of("correo", correo)).andExpect(status().isAccepted());
		String token = buzon.ultimoToken(correo, "recuperacion").orElseThrow();

		enviar(RESTABLECER, Map.of("token", token, "contrasenaNueva", NUEVA)).andExpect(status().isNoContent());

		consultar("/api/v1/dispositivos", movil).andExpect(status().isUnauthorized());
		consultar("/api/v1/dispositivos", pc).andExpect(status().isUnauthorized());
		enviar("/api/v1/auth/refresco", Map.of("tokenRefresco", movil.tokenRefresco()))
				.andExpect(status().isUnauthorized());
		enviar("/api/v1/auth/login", cuerpoLogin(correo, CONTRASENA, "Móvil", null))
				.andExpect(status().isUnauthorized());
		enviar("/api/v1/auth/login", cuerpoLogin(correo, NUEVA, "Móvil", null)).andExpect(status().isOk());
	}

	@Test
	void elEnlaceSoloSirveUnaVez() throws Exception {
		String correo = crearCuenta();
		enviar(RECUPERAR, Map.of("correo", correo));
		String token = buzon.ultimoToken(correo, "recuperacion").orElseThrow();

		enviar(RESTABLECER, Map.of("token", token, "contrasenaNueva", NUEVA)).andExpect(status().isNoContent());

		enviar(RESTABLECER, Map.of("token", token, "contrasenaNueva", "tercera-contrasena-larga-3"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void elEnlaceCaducaALos30Minutos() throws Exception {
		String correo = crearCuenta();
		enviar(RECUPERAR, Map.of("correo", correo));
		String token = buzon.ultimoToken(correo, "recuperacion").orElseThrow();

		reloj.avanzar(Duration.ofMinutes(29));
		reloj.avanzar(Duration.ofMinutes(2));

		enviar(RESTABLECER, Map.of("token", token, "contrasenaNueva", NUEVA)).andExpect(status().isBadRequest());
	}

	@Test
	void unaContrasenaDebilNoGastaElEnlace() throws Exception {
		String correo = crearCuenta();
		enviar(RECUPERAR, Map.of("correo", correo));
		String token = buzon.ultimoToken(correo, "recuperacion").orElseThrow();

		enviar(RESTABLECER, Map.of("token", token, "contrasenaNueva", "corta")).andExpect(status().isBadRequest());
		enviar(RESTABLECER, Map.of("token", token, "contrasenaNueva", ClavesDePrueba.CONTRASENA_FILTRADA))
				.andExpect(status().isBadRequest());

		enviar(RESTABLECER, Map.of("token", token, "contrasenaNueva", NUEVA)).andExpect(status().isNoContent());
	}

	@Test
	void restablecerLevantaElBloqueoPorFallos() throws Exception {
		String correo = crearCuenta();
		for (int i = 0; i < 10; i++) {
			enviar("/api/v1/auth/login", cuerpoLogin(correo, "contrasena-mala-" + i + "xx", "P", null))
					.andExpect(status().isUnauthorized());
		}
		enviar(RECUPERAR, Map.of("correo", correo));
		String token = buzon.ultimoToken(correo, "recuperacion").orElseThrow();

		enviar(RESTABLECER, Map.of("token", token, "contrasenaNueva", NUEVA)).andExpect(status().isNoContent());

		assertThat(contar("SELECT fallos_seguidos FROM usuario WHERE correo = ?", correo)).isZero();
		enviar("/api/v1/auth/login", cuerpoLogin(correo, NUEVA, "P", null)).andExpect(status().isOk());
	}

	@Test
	void emitirUnEnlaceNuevoAnulaElAnterior() throws Exception {
		String correo = crearCuenta();
		enviar(RECUPERAR, Map.of("correo", correo));
		String viejo = buzon.ultimoToken(correo, "recuperacion").orElseThrow();
		enviar(RECUPERAR, Map.of("correo", correo));
		String nuevo = buzon.ultimoToken(correo, "recuperacion").orElseThrow();

		enviar(RESTABLECER, Map.of("token", viejo, "contrasenaNueva", NUEVA)).andExpect(status().isBadRequest());

		enviar(RESTABLECER, Map.of("token", nuevo, "contrasenaNueva", NUEVA)).andExpect(status().isNoContent());
	}

	@Test
	void restablecerGastaTambienLosOtrosEnlacesPendientes() throws Exception {
		String correo = crearCuenta();
		enviar(RECUPERAR, Map.of("correo", correo));
		String enlace = buzon.ultimoToken(correo, "recuperacion").orElseThrow();
		// Un segundo enlace vivo que no salió por la API (p. ej. un resto de antes del arreglo).
		long usuarioId = contar("SELECT id FROM usuario WHERE correo = ?", correo);
		String otro = "enlace-anterior-que-sigue-vivo";
		tokensUnUso.insertar(usuarioId, es.forja.cuenta.modelo.TipoTokenUnUso.RECUPERACION, sha256(otro),
				reloj.instant().plus(Duration.ofMinutes(20)));

		enviar(RESTABLECER, Map.of("token", enlace, "contrasenaNueva", NUEVA)).andExpect(status().isNoContent());

		enviar(RESTABLECER, Map.of("token", otro, "contrasenaNueva", "tercera-contrasena-larga-3"))
				.andExpect(status().isBadRequest());
		assertThat(contar("SELECT COUNT(*) FROM token_un_uso WHERE usuario_id = ? AND tipo = 'recuperacion' "
				+ "AND usado_en IS NULL", usuarioId)).isZero();
	}

	private static byte[] sha256(String texto) throws Exception {
		return java.security.MessageDigest.getInstance("SHA-256")
				.digest(texto.getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}
}
