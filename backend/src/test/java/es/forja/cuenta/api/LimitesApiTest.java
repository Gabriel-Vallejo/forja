package es.forja.cuenta.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import es.forja.cuenta.servicio.FallosLogin;
import es.forja.cuenta.servicio.LimitadorIntentos;

class LimitesApiTest extends ApiCuentaSoporte {

	@Autowired
	private FallosLogin fallos;

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
	void elLoginLimitaPorCorreoAunqueCambieLaIp() throws Exception {
		// El freno por cuenta que cubre todos los orígenes: la espera de FallosLogin va por origen, este no.
		String correo = crearCuenta();
		for (int i = 0; i < LimitadorIntentos.INTENTOS_POR_CORREO; i++) {
			enviarDesde(ip + "." + i, LOGIN, cuerpoLogin(correo, "contrasena-mala-" + i + "xx", "P", null))
					.andExpect(status().isUnauthorized());
		}

		enviarDesde(ip + ".99", LOGIN, cuerpoLogin(correo, CONTRASENA, "P", null))
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
	void tras10FallosSeguidosEseOrigenEsperaCadaVezMasYElDuenoEntraDesdeOtro() throws Exception {
		String correo = crearCuenta();
		String atacante = ip + ".66";
		for (int i = 0; i < 10; i++) {
			enviarDesde(atacante, LOGIN, cuerpoLogin(correo, "contrasena-mala-" + i + "xx", "P", null))
					.andExpect(status().isUnauthorized());
		}

		// Desde ese origen, aunque acierte, está en espera y no se distingue de un correo inexistente.
		enviarDesde(atacante, LOGIN, cuerpoLogin(correo, CONTRASENA, "P", null)).andExpect(status().isUnauthorized())
				.andExpect(header().doesNotExist("Retry-After"));
		// El dueño, desde su red, entra: ya no se puede dejar fuera a nadie sabiendo su correo.
		enviar(LOGIN, cuerpoLogin(correo, CONTRASENA, "P", null)).andExpect(status().isOk());

		// Pasado el minuto el atacante puede probar; un nuevo fallo duplica su espera a 2 min.
		reloj.avanzar(Duration.ofSeconds(61));
		enviarDesde(atacante, LOGIN, cuerpoLogin(correo, "contrasena-mala-final", "P", null))
				.andExpect(status().isUnauthorized());
		reloj.avanzar(Duration.ofSeconds(61));
		enviarDesde(atacante, LOGIN, cuerpoLogin(correo, CONTRASENA, "P", null)).andExpect(status().isUnauthorized());
		reloj.avanzar(Duration.ofSeconds(61));

		// Un acierto fuera de la espera pone ese origen a cero.
		enviarDesde(atacante, LOGIN, cuerpoLogin(correo, CONTRASENA, "P", null)).andExpect(status().isOk());
		assertThat(fallos.fallos(idDe(correo), atacante)).isZero();
	}

	@Test
	void unLoginBuenoAntesDelDecimoFalloPoneEseOrigenAcero() throws Exception {
		String correo = crearCuenta();
		for (int i = 0; i < 9; i++) {
			enviar(LOGIN, cuerpoLogin(correo, "contrasena-mala-" + i + "xx", "P", null))
					.andExpect(status().isUnauthorized());
		}
		assertThat(fallos.fallos(idDe(correo), ip)).isEqualTo(9);

		enviar(LOGIN, cuerpoLogin(correo, CONTRASENA, "P", null)).andExpect(status().isOk());

		assertThat(fallos.fallos(idDe(correo), ip)).isZero();
		enviar(LOGIN, cuerpoLogin(correo, "mala-otra-vez-123", "P", null)).andExpect(status().isUnauthorized());
		assertThat(fallos.fallos(idDe(correo), ip)).isEqualTo(1);
	}

	@Test
	void unOrigenEnEsperaYUnCorreoInexistenteDanLaMismaRespuestaYElMismoCodigo() throws Exception {
		String correo = crearCuenta();
		for (int i = 0; i < 10; i++) {
			enviar(LOGIN, cuerpoLogin(correo, "contrasena-mala-" + i + "xx", "P", null))
					.andExpect(status().isUnauthorized());
		}

		var bloqueada = enviar(LOGIN, cuerpoLogin(correo, CONTRASENA, "P", null));
		var inexistente = enviar(LOGIN, cuerpoLogin(correoNuevo(), CONTRASENA, "P", null));

		assertThat(bloqueada.andReturn().getResponse().getStatus())
				.isEqualTo(inexistente.andReturn().getResponse().getStatus()).isEqualTo(401);
		assertThat(bloqueada.andReturn().getResponse().getContentAsString())
				.isEqualTo(inexistente.andReturn().getResponse().getContentAsString());
		assertThat(bloqueada.andReturn().getResponse().getHeaderNames())
				.isEqualTo(inexistente.andReturn().getResponse().getHeaderNames());
	}

	@Test
	void fallosEnParaleloDesdeElMismoOrigenNoSePierden() throws Exception {
		String correo = crearCuenta();
		int fallosEnParalelo = 8;
		var piscina = java.util.concurrent.Executors.newFixedThreadPool(fallosEnParalelo);
		var salida = new java.util.concurrent.CountDownLatch(1);
		try {
			var tareas = new java.util.ArrayList<java.util.concurrent.Future<Integer>>();
			for (int i = 0; i < fallosEnParalelo; i++) {
				String mala = "contrasena-mala-" + i + "xx";
				tareas.add(piscina.submit(() -> {
					salida.await();
					return enviar(LOGIN, cuerpoLogin(correo, mala, "P", null)).andReturn().getResponse().getStatus();
				}));
			}
			salida.countDown();
			for (var tarea : tareas) {
				assertThat(tarea.get()).isEqualTo(401);
			}
		} finally {
			piscina.shutdownNow();
		}

		assertThat(fallos.fallos(idDe(correo), ip)).isEqualTo(fallosEnParalelo);
	}

	private long idDe(String correo) {
		return contar("SELECT id FROM usuario WHERE correo = ?", correo);
	}
}
