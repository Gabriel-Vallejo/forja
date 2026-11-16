package es.forja;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

/**
 * Arranca la aplicación contra un MySQL 8.4 de Testcontainers.
 *
 * <p>Las propiedades de conexión del application.yml salen de variables de entorno que en las pruebas
 * no existen; @ServiceConnection manda sobre estos valores de relleno, que nunca llegan a usarse.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = ClavesDePrueba.class)
@Import({ TestcontainersConfiguration.class, RelojDePrueba.Configuracion.class, BuzonDeCorreo.Configuracion.class })
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:mysql://relleno:3306/relleno",
		"spring.datasource.username=relleno",
		"spring.datasource.password=relleno"
})
public @interface PruebaConMysql {
}
