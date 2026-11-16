package es.forja.comun.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import es.forja.ClavesDePrueba;

class PropiedadesJwtTest {

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(PropiedadesJwt.class)
	static class Minima {
	}

	private final ApplicationContextRunner contexto = new ApplicationContextRunner().withUserConfiguration(Minima.class);

	@Test
	void sinLaClaveActualLaAplicacionNoArranca() {
		contexto.run(c -> assertThat(c).hasFailed());
		contexto.withPropertyValues("forja.jwt.kid-actual=k").run(c -> assertThat(c).hasFailed());
	}

	@Test
	void conLaClaveActualArrancaYLaAnteriorEsOpcional() {
		contexto.withPropertyValues("forja.jwt.kid-actual=k",
				"forja.jwt.clave-privada-actual=" + ClavesDePrueba.privadaBase64(ClavesDePrueba.ACTUAL))
				.run(c -> assertThat(c).hasNotFailed());
	}
}
