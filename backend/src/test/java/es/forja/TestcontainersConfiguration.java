package es.forja;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/** MySQL real de la misma versión que producción (ADR 0001), no un H2 que se le parezca. */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	static final String IMAGEN_MYSQL = "mysql:8.4.13";

	@Bean
	@ServiceConnection
	MySQLContainer mysqlContainer() {
		return new MySQLContainer(DockerImageName.parse(IMAGEN_MYSQL));
	}

}
