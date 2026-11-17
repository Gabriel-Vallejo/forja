package es.forja;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.UserDetailsService;

@PruebaConMysql
class ForjaBackendApplicationTests {

	@Autowired
	private ApplicationContext contexto;

	@Test
	void arrancaConHibernateEnValidate() {
		// Si el contexto carga, Flyway migró y las entidades casan con el esquema (ddl-auto: validate).
	}

	@Test
	void noHayUsuarioEnMemoriaConContrasenaGenerada() {
		// La autenticación es solo por JWT (ADR 0009); el usuario «user» de Spring sería una puerta trasera sin uso.
		assertThat(contexto.getBeanNamesForType(UserDetailsService.class)).isEmpty();
	}

}
