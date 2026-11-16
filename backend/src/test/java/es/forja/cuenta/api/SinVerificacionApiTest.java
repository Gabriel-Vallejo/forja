package es.forja.cuenta.api;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/** Con {@code forja.cuenta.exigir-verificacion=false} (solo desarrollo local) se puede entrar sin verificar. */
@TestPropertySource(properties = "forja.cuenta.exigir-verificacion=false")
class SinVerificacionApiTest extends ApiCuentaSoporte {

	@Test
	void sinExigirVerificacionUnUsuarioSinVerificarPuedeEntrar() throws Exception {
		String correo = crearCuentaSinVerificar();

		enviar("/api/v1/auth/login", cuerpoLogin(correo, CONTRASENA, "Portátil", null)).andExpect(status().isOk());
	}

	@Test
	void sinExigirVerificacionLaContrasenaMalaSigueDando401() throws Exception {
		String correo = crearCuentaSinVerificar();

		enviar("/api/v1/auth/login", cuerpoLogin(correo, "contrasena-equivocada-1", "Portátil", null))
				.andExpect(status().isUnauthorized());
	}
}
