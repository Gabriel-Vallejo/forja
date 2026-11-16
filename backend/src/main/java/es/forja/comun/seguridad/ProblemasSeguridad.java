package es.forja.comun.seguridad;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/** Respuestas 401 y 403 de la capa de seguridad como {@code ProblemDetail}, sin trazas (RFC 9457). */
@Component
public class ProblemasSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

	private final JsonMapper json;

	public ProblemasSeguridad(JsonMapper json) {
		this.json = json;
	}

	@Override
	public void commence(HttpServletRequest peticion, HttpServletResponse respuesta, AuthenticationException e)
			throws IOException {
		respuesta.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
		escribir(respuesta, HttpStatus.UNAUTHORIZED, "Hace falta iniciar sesión.");
	}

	@Override
	public void handle(HttpServletRequest peticion, HttpServletResponse respuesta, AccessDeniedException e)
			throws IOException {
		escribir(respuesta, HttpStatus.FORBIDDEN, "No tienes permiso para hacer esto.");
	}

	private void escribir(HttpServletResponse respuesta, HttpStatus estado, String detalle) throws IOException {
		respuesta.setStatus(estado.value());
		respuesta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		respuesta.setCharacterEncoding("UTF-8");
		respuesta.getWriter().write(json.writeValueAsString(ProblemDetail.forStatusAndDetail(estado, detalle)));
	}
}
