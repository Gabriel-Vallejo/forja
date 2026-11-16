package es.forja.comun;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Un único reloj inyectable: en los tests se sustituye para probar caducidades sin esperar. */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionReloj {

	@Bean
	Clock reloj() {
		return Clock.systemUTC();
	}

}
