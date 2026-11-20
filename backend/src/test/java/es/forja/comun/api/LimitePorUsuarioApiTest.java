package es.forja.comun.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

import es.forja.comun.seguridad.LimitePorUsuario;
import es.forja.cuenta.api.ApiCuentaSoporte;

/** Tope de peticiones por usuario en todas las rutas con sesión, además de los límites propios de cada acción. */
class LimitePorUsuarioApiTest extends ApiCuentaSoporte {

	private ResultActions dispositivos(Cliente cliente) throws Exception {
		return mvc.perform(get("/api/v1/dispositivos").header("Authorization", cliente.cabecera()));
	}

	@Test
	void pasadoElTopeEs429ConCodigoYRetryAfterYSoloParaEseUsuario() throws Exception {
		Cliente cliente = cuentaConSesion("Móvil");
		for (int i = 0; i < LimitePorUsuario.PETICIONES_POR_MINUTO; i++) {
			dispositivos(cliente).andExpect(status().isOk());
		}

		dispositivos(cliente).andExpect(status().isTooManyRequests())
				.andExpect(header().exists(HttpHeaders.RETRY_AFTER))
				.andExpect(jsonPath("$.codigo").value("demasiadas_peticiones"));
		dispositivos(cuentaConSesion("PC")).andExpect(status().isOk());
	}

	@Test
	void sinSesionNoGastaNadaYSigueSiendo401() throws Exception {
		for (int i = 0; i <= LimitePorUsuario.PETICIONES_POR_MINUTO; i++) {
			mvc.perform(get("/api/v1/dispositivos")).andExpect(status().isUnauthorized());
		}
	}
}
