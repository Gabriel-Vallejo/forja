package es.forja;

import org.junit.jupiter.api.Test;

@PruebaConMysql
class ForjaBackendApplicationTests {

	@Test
	void arrancaConHibernateEnValidate() {
		// Si el contexto carga, Flyway migró y las entidades casan con el esquema (ddl-auto: validate).
	}

}
