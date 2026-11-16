package es.forja.cuenta.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import es.forja.cuenta.servicio.LimitadorIntentos;

class LimitesApiTest extends ApiCuentaSoporte {

	private static final String LOGIN = "/api/v1/auth/login";

	@Test
	void elLoginDa429ConRetryAfterAlPasarseElLimiteDeUnaIp() throws Exception {
		for (int i = 0; i < LimitadorIntentos.INTENTOS_POR_IP; i++) {
			enviar(LOGIN, cuerpoLogin(correoNuevo(), CONTRASENA, "P", null)).andExpect(status().isUnauthorized());
		}

		enviar(LOGIN, cuerpoLogin(correoNuevo(), CONTRASENA, "P", null)).andExpect(status().isTooManyRequests())
				.andExpect(header().exists("Retry-After"));
	}

	@Test
	void elRegistroLimitaPorIp() throws Exception {
		String menor = java.time.LocalDate.now().minusYears(10).toString();
		for (int i = 0; i < LimitadorIntentos.INTENTOS_POR_IP; i++) {
			enviar("/api/v1/auth/registro", cuerpoRegistro(correoNuevo(), CONTRASENA, menor))
					.andExpect(status().isBadRequest());
		}

		enviar("/api/v1/auth/registro", cuerpoRegistro(correoNuevo(), CONTRASENA, menor))
				.andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
	}

	@Test
	void laRecuperacionLimitaPorCorreoAunqueCambieLaIp() throws Exception {
		String correo = correoNuevo();
		for (int i = 0; i < LimitadorIntentos.INTENTOS_POR_CORREO; i++) {
			enviarDesde(ip + "." + i, "/api/v1/auth/recuperar", Map.of("correo", correo))
					.andExpect(status().isAccepted());
		}

		enviarDesde(ip + ".99", "/api/v1/auth/recuperar", Map.of("correo", correo))
				.andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
	}

	@Test
	void tras10FallosSeguidosLaCuentaSeBloqueaConEsperaCreciente() throws Exception {
		String correo = crearCuenta();
		for (int i = 0; i < 10; i++) {
			enviar(LOGIN, cuerpoLogin(correo, "contrasena-mala-" + i + "xx", "P", null))
					.andExpect(status().isUnauthorized());
		}

		// Aunque ahora acierte, la cuenta está en espera.
		enviar(LOGIN, cuerpoLogin(correo, CONTRASENA, "P", null)).andExpect(status().isTooManyRequests())
				.andExpect(header().exists("Retry-After"));
		assertThat(contar("SELECT COUNT(*) FROM usuario WHERE correo = ? AND bloqueado_hasta IS NOT NULL", correo))
				.isEqualTo(1);

		// Pasado el minuto se puede probar; un nuevo fallo duplica la espera a 2 min.
		reloj.avanzar(Duration.ofSeconds(61));
		enviar(LOGIN, cuerpoLogin(correo, "contrasena-mala-final", "P", null)).andExpect(status().isUnauthorized());
		reloj.avanzar(Duration.ofSeconds(61));
		enviar(LOGIN, cuerpoLogin(correo, CONTRASENA, "P", null)).andExpect(status().isTooManyRequests());
		reloj.avanzar(Duration.ofSeconds(61));

		// Un acierto fuera de la espera lo deja todo a cero.
		enviar(LOGIN, cuerpoLogin(correo, CONTRASENA, "P", null)).andExpect(status().isOk());
		assertThat(contar("SELECT fallos_seguidos FROM usuario WHERE correo = ?", correo)).isZero();
		assertThat(contar("SELECT COUNT(*) FROM usuario WHERE correo = ? AND bloqueado_hasta IS NOT NULL", correo))
				.isZero();
	}

	@Test
	void unLoginBuenoAntesDelDecimoFalloPoneLaCuentaAcero() throws Exception {
		String correo = crearCuenta();
		for (int i = 0; i < 9; i++) {
			enviar(LOGIN, cuerpoLogin(correo, "contrasena-mala-" + i + "xx", "P", null))
					.andExpect(status().isUnauthorized());
		}
		assertThat(contar("SELECT fallos_seguidos FROM usuario WHERE correo = ?", correo)).isEqualTo(9);

		enviar(LOGIN, cuerpoLogin(correo, CONTRASENA, "P", null)).andExpect(status().isOk());

		assertThat(contar("SELECT fallos_seguidos FROM usuario WHERE correo = ?", correo)).isZero();
		enviar(LOGIN, cuerpoLogin(correo, "mala-otra-vez-123", "P", null)).andExpect(status().isUnauthorized());
		assertThat(contar("SELECT fallos_seguidos FROM usuario WHERE correo = ?", correo)).isEqualTo(1);
	}
}
