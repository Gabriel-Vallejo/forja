package es.forja.cuenta.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import es.forja.ClavesDePrueba;

class RecuperacionApiTest extends ApiCuentaSoporte {

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
}
