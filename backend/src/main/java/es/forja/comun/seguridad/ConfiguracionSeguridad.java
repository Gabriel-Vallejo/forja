package es.forja.comun.seguridad;

import java.time.Clock;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** Seguridad HTTP sin estado: token de acceso en cabecera, nada de sesión ni cookies (ADR 0009). */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PropiedadesJwt.class)
public class ConfiguracionSeguridad {

	private static final List<String> ORIGENES_TAURI = List.of("tauri://localhost", "http://tauri.localhost");
	private static final long SEGUNDOS_CACHE_CORS = 3600;
	private static final String CSP = "default-src 'none'; script-src 'self'; style-src 'self'; connect-src 'self'; "
			+ "form-action 'self'; base-uri 'none'; frame-ancestors 'none'";
	private static final String[] RUTAS_DOCUMENTACION = { "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html",
			"/swagger-ui/**" };

	@Bean
	ServicioJwt servicioJwt(Clock reloj, PropiedadesJwt propiedades) {
		return ServicioJwt.desdePropiedades(reloj, propiedades);
	}

	@Bean
	PasswordEncoder codificadorDeContrasenas() {
		return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
	}

	@Bean
	SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, ServicioJwt jwt, DispositivosActivos dispositivos,
			ProblemasSeguridad problemas) throws Exception {
		http
				// CSRF desactivado a propósito: el token viaja en la cabecera Authorization, no en una cookie,
				// así que ningún sitio ajeno puede hacer que el navegador lo envíe solo (ADR 0009 §7).
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.requestCache(c -> c.disable())
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				// Las páginas del enlace del correo solo cargan sus propios archivos; la API no sirve HTML.
				.headers(h -> h.contentSecurityPolicy(c -> c.policyDirectives(CSP)))
				.exceptionHandling(e -> e.authenticationEntryPoint(problemas).accessDeniedHandler(problemas))
				.authorizeHttpRequests(a -> a
						// Cerrar sesión necesita saber qué dispositivo cierra, y eso sale del token.
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/cerrar-sesion").authenticated()
						.requestMatchers("/api/v1/auth/**", "/actuator/health", "/actuator/health/**", "/error")
						.permitAll()
						// Páginas del enlace del correo (verificar y restablecer): estáticas, el token va en el fragmento.
						.requestMatchers(HttpMethod.GET, "/cuenta/*").permitAll()
						// Documentación de la API: sin datos de usuarios; fuera del perfil dev, springdoc no la sirve.
						.requestMatchers(HttpMethod.GET, RUTAS_DOCUMENTACION).permitAll()
						.anyRequest().authenticated())
				.addFilterBefore(new FiltroJwt(jwt, dispositivos), BasicAuthenticationFilter.class);
		return http.build();
	}

	/** El nombre del método importa: Spring Security busca el bean {@code corsConfigurationSource}. */
	@Bean
	CorsConfigurationSource corsConfigurationSource(@Value("${forja.origen-web:}") String origenWeb) {
		CorsConfiguration cors = new CorsConfiguration();
		List<String> origenes = new java.util.ArrayList<>(ORIGENES_TAURI);
		if (!origenWeb.isBlank()) {
			origenes.add(origenWeb.strip());
		}
		cors.setAllowedOrigins(origenes);
		cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		cors.setMaxAge(SEGUNDOS_CACHE_CORS);
		UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
		fuente.registerCorsConfiguration("/**", cors);
		return fuente;
	}
}
