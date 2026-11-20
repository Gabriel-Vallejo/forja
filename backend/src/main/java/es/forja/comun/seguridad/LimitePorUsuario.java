package es.forja.comun.seguridad;

import java.time.Duration;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import es.forja.cuenta.servicio.LimitadorIntentos;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Tope general de peticiones por usuario en las rutas con sesión (429 con {@code Retry-After}). Frena un token
 * robado o un cliente desbocado; las acciones caras (generar, exportar, borrar) llevan además su propio límite.
 * Corre dentro de Spring MVC para que el 429 salga por el mismo manejador de errores que el resto.
 */
@Component
public class LimitePorUsuario implements HandlerInterceptor, WebMvcConfigurer {

	/** Holgado: abrir la app hace unas 10 peticiones y sincronizar sesiones atrasadas, una por sesión. */
	public static final int PETICIONES_POR_MINUTO = 120;
	static final String ACCION = "api";

	private final LimitadorIntentos limitador;

	public LimitePorUsuario(LimitadorIntentos limitador) {
		this.limitador = limitador;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registro) {
		registro.addInterceptor(this).addPathPatterns("/api/**");
	}

	@Override
	public boolean preHandle(HttpServletRequest peticion, HttpServletResponse respuesta, Object manejador) {
		Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioAutenticado usuario) {
			limitador.exigirPorClave(ACCION, usuario.usuario().toString(), PETICIONES_POR_MINUTO,
					Duration.ofMinutes(1));
		}
		return true;
	}
}
