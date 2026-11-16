package es.forja.cuenta.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class BorradoCuentaApiTest extends ApiCuentaSoporte {

	private ResultActions borrarCuenta(Cliente cliente, String contrasena) throws Exception {
		return mvc.perform(delete("/api/v1/cuenta").header("Authorization", cliente.cabecera())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("contrasena", contrasena))));
	}

	private long filasDe(String tabla, String correo) {
		return switch (tabla) {
			case "usuario" -> contar("SELECT COUNT(*) FROM usuario WHERE correo = ?", correo);
			case "dispositivo" -> contar("SELECT COUNT(*) FROM dispositivo d JOIN usuario u ON u.id = d.usuario_id "
					+ "WHERE u.correo = ?", correo);
			case "token_refresco" -> contar("SELECT COUNT(*) FROM token_refresco t JOIN dispositivo d "
					+ "ON d.id = t.dispositivo_id JOIN usuario u ON u.id = d.usuario_id WHERE u.correo = ?", correo);
			default -> contar("SELECT COUNT(*) FROM token_un_uso t JOIN usuario u ON u.id = t.usuario_id "
					+ "WHERE u.correo = ?", correo);
		};
	}

	@Test
	void conUnaContrasenaMalaNoBorraNada() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		borrarCuenta(cliente, "contrasena-equivocada-1").andExpect(status().isForbidden());

		assertThat(filasDe("usuario", cliente.correo())).isEqualTo(1);
		consultar("/api/v1/dispositivos", cliente).andExpect(status().isOk());
	}

	@Test
	void borrarLaCuentaSeLlevaDispositivosYTokensEnCascada() throws Exception {
		String correo = crearCuenta();
		Cliente movil = iniciarSesion(correo, "Móvil", null);
		iniciarSesion(correo, "PC", null);
		enviar("/api/v1/auth/recuperar", Map.of("correo", correo));
		assertThat(filasDe("dispositivo", correo)).isEqualTo(2);
		assertThat(filasDe("token_refresco", correo)).isEqualTo(2);
		assertThat(filasDe("token_un_uso", correo)).isEqualTo(2);

		borrarCuenta(movil, CONTRASENA).andExpect(status().isNoContent());

		assertThat(filasDe("usuario", correo)).isZero();
		assertThat(filasDe("dispositivo", correo)).isZero();
		assertThat(filasDe("token_refresco", correo)).isZero();
		assertThat(filasDe("token_un_uso", correo)).isZero();
		consultar("/api/v1/dispositivos", movil).andExpect(status().isUnauthorized());
		enviar("/api/v1/auth/login", cuerpoLogin(correo, CONTRASENA, "Móvil", null))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void sinSesionNoSePuedeBorrar() throws Exception {
		mvc.perform(delete("/api/v1/cuenta").contentType(MediaType.APPLICATION_JSON)
				.content("{\"contrasena\":\"x\"}")).andExpect(status().isUnauthorized());
	}
}
