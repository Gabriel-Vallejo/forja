package es.forja.cuenta.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class LimitadorIntentosTest {

	private final LimitadorIntentos limitador = new LimitadorIntentos();

	@Test
	void dejaPasarHastaElLimiteDeUnaIpYDespuesDaTooManyRequestsConEspera() {
		for (int i = 0; i < LimitadorIntentos.INTENTOS_POR_IP; i++) {
			limitador.exigir("login", "10.0.0.1", "correo" + i + "@forja.test");
		}

		assertThatThrownBy(() -> limitador.exigir("login", "10.0.0.1", "otro@forja.test"))
				.isInstanceOfSatisfying(ErrorCuenta.class, e -> {
					assertThat(e.estado()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
					assertThat(e.segundosDeEspera()).isBetween(1L, 60L);
				});
	}

	@Test
	void unaIpAgotadaNoAfectaAOtra() {
		for (int i = 0; i < LimitadorIntentos.INTENTOS_POR_IP; i++) {
			limitador.exigir("login", "10.0.0.2", "a" + i + "@forja.test");
		}

		assertThatCode(() -> limitador.exigir("login", "10.0.0.3", "b@forja.test")).doesNotThrowAnyException();
	}

	@Test
	void limitaPorCorreoAunqueCambieLaIp() {
		for (int i = 0; i < LimitadorIntentos.INTENTOS_POR_CORREO; i++) {
			limitador.exigir("recuperar", "10.1.0." + i, "victima@forja.test");
		}

		assertThatThrownBy(() -> limitador.exigir("recuperar", "10.9.9.9", "victima@forja.test"))
				.isInstanceOf(ErrorCuenta.class);
	}

	@Test
	void cadaAccionTieneSusPropiosContadores() {
		for (int i = 0; i < LimitadorIntentos.INTENTOS_POR_CORREO; i++) {
			limitador.exigir("login", "10.2.0." + i, "x@forja.test");
		}

		assertThatCode(() -> limitador.exigir("registro", "10.3.0.1", "x@forja.test")).doesNotThrowAnyException();
	}

	@Test
	void unErrorSinEsperaDaCeroSegundos() {
		assertThat(ErrorCuenta.noAutenticado("x").segundosDeEspera()).isZero();
	}
}
