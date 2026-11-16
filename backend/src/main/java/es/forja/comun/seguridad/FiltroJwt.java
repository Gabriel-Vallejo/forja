package es.forja.comun.seguridad;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Autentica con el token de la cabecera {@code Authorization: Bearer}. Si el token no vale, no autentica y
 * deja que las reglas de acceso decidan: en una ruta abierta la petición sigue, en una cerrada sale el 401.
 */
public class FiltroJwt extends OncePerRequestFilter {

	private static final String PREFIJO = "Bearer ";

	private final ServicioJwt jwt;
	private final DispositivosActivos dispositivos;

	public FiltroJwt(ServicioJwt jwt, DispositivosActivos dispositivos) {
		this.jwt = jwt;
		this.dispositivos = dispositivos;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
			throws ServletException, IOException {
		String cabecera = peticion.getHeader(HttpHeaders.AUTHORIZATION);
		if (cabecera != null && cabecera.startsWith(PREFIJO)) {
			jwt.validar(cabecera.substring(PREFIJO.length()).strip())
					.filter(c -> dispositivos.estaActivo(c.dispositivo(), c.usuario()))
					.ifPresent(c -> SecurityContextHolder.getContext().setAuthentication(
							UsernamePasswordAuthenticationToken.authenticated(
									new UsuarioAutenticado(c.usuario(), c.dispositivo()), null, List.of())));
		}
		cadena.doFilter(peticion, respuesta);
	}
}
