package es.forja.entreno.rutina.servicio;

import org.springframework.boot.context.properties.ConfigurationProperties;

import es.forja.entreno.rutina.modelo.ConfiguracionRutina;

/** {@code forja.rutina.*} de {@code application.yml}: pesos del objetivo, rangos de volumen, tiempos y solver. */
@ConfigurationProperties("forja.rutina")
public record PropiedadesRutina(ConfiguracionRutina.Pesos pesos, ConfiguracionRutina.Volumen volumen,
		ConfiguracionRutina.Tiempos tiempos, ConfiguracionRutina.Sesion sesion, ConfiguracionRutina.Solver solver) {

	public ConfiguracionRutina aConfiguracion() {
		return new ConfiguracionRutina(pesos, volumen, tiempos, sesion, solver);
	}
}
