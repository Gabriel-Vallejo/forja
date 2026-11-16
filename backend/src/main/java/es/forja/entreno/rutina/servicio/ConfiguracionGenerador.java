package es.forja.entreno.rutina.servicio;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import es.forja.entreno.rutina.solver.GeneradorRutina;

@Configuration
@EnableConfigurationProperties(PropiedadesRutina.class)
public class ConfiguracionGenerador {

	@Bean
	GeneradorRutina generadorRutina(PropiedadesRutina propiedades) {
		return new GeneradorRutina(propiedades.aConfiguracion());
	}
}
