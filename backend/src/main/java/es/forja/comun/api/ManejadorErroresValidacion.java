package es.forja.comun.api;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Errores de validación como {@code ProblemDetail} (RFC 9457). Solo dice qué campo falla y por qué:
 * nunca el valor rechazado, que puede ser una contraseña.
 */
@RestControllerAdvice
public class ManejadorErroresValidacion extends ResponseEntityExceptionHandler {

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
			HttpHeaders cabeceras, HttpStatusCode estado, WebRequest peticion) {
		List<ErrorDeCampo> errores = e.getBindingResult().getFieldErrors().stream()
				.map(f -> new ErrorDeCampo(f.getField(), f.getDefaultMessage())).toList();
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"Hay datos que no son válidos.");
		problema.setProperty("errores", errores);
		return ResponseEntity.badRequest().body(problema);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException e,
			HttpHeaders cabeceras, HttpStatusCode estado, WebRequest peticion) {
		return ResponseEntity.badRequest().body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"El cuerpo de la petición no es válido."));
	}

	public record ErrorDeCampo(String campo, String mensaje) {
	}
}
