package es.forja.cuenta.servicio;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.json.JsonMapper;

/** Con proveedor configurado, el correo sale por ACS; sin él, no sale nada (desarrollo y CI). */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PropiedadesCorreo.class)
public class ConfiguracionCorreo {

	@Bean
	EnvioCorreo envioCorreo(PropiedadesCorreo propiedades, JsonMapper json) {
		return propiedades.configurado() ? EnvioCorreoAcs.desde(propiedades, json) : new EnvioCorreoSinProveedor();
	}
}
