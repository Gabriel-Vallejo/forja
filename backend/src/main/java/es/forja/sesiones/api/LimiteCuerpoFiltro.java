package es.forja.sesiones.api;

import java.io.IOException;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tope al cuerpo de {@code PUT /api/v1/sesiones/*}: 413 si el cliente declara más de {@value #MAXIMO_BYTES} bytes y,
 * si no los declara (chunked), corta la lectura al pasarse, con lo que la petición acaba en 400.
 */
@Component
public class LimiteCuerpoFiltro extends OncePerRequestFilter {

	public static final int MAXIMO_BYTES = 256 * 1024;
	private static final String PREFIJO = "/api/v1/sesiones/";

	private final JsonMapper json;

	public LimiteCuerpoFiltro(JsonMapper json) {
		this.json = json;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest peticion) {
		return !HttpMethod.PUT.matches(peticion.getMethod()) || !peticion.getRequestURI().startsWith(PREFIJO);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
			throws ServletException, IOException {
		if (peticion.getContentLengthLong() > MAXIMO_BYTES) {
			respuesta.setStatus(HttpStatus.CONTENT_TOO_LARGE.value());
			respuesta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
			respuesta.getWriter().write(json.writeValueAsString(ProblemDetail.forStatusAndDetail(
					HttpStatus.CONTENT_TOO_LARGE, "La sesión es demasiado grande.")));
			return;
		}
		cadena.doFilter(new CuerpoAcotado(peticion), respuesta);
	}

	/** Lee como mucho {@link #MAXIMO_BYTES}; el siguiente byte lanza {@code IOException}. */
	private static final class CuerpoAcotado extends HttpServletRequestWrapper {

		CuerpoAcotado(HttpServletRequest peticion) {
			super(peticion);
		}

		@Override
		public ServletInputStream getInputStream() throws IOException {
			ServletInputStream origen = super.getInputStream();
			return new ServletInputStream() {
				private long leidos;

				@Override
				public int read() throws IOException {
					int byteLeido = origen.read();
					contar(byteLeido < 0 ? 0 : 1);
					return byteLeido;
				}

				@Override
				public int read(byte[] destino, int desde, int largo) throws IOException {
					int n = origen.read(destino, desde, largo);
					contar(Math.max(n, 0));
					return n;
				}

				private void contar(int n) throws IOException {
					leidos += n;
					if (leidos > MAXIMO_BYTES) {
						throw new IOException("Cuerpo demasiado grande.");
					}
				}

				@Override
				public boolean isFinished() {
					return origen.isFinished();
				}

				@Override
				public boolean isReady() {
					return origen.isReady();
				}

				@Override
				public void setReadListener(ReadListener oyente) {
					origen.setReadListener(oyente);
				}
			};
		}
	}
}
