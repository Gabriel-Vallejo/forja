package es.forja.comun.api;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce {@link ErrorNegocio} a {@code ProblemDetail} (RFC 9457), sin trazas ni datos de la petición. */
@RestControllerAdvice
public class ManejadorErrorNegocio {

	@ExceptionHandler(ErrorNegocio.class)
	ResponseEntity<ProblemDetail> alFallar(ErrorNegocio error) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(error.estado(), error.getMessage());
		if (error.codigo() != null) {
			problema.setProperty("codigo", error.codigo());
		}
		return ResponseEntity.status(error.estado()).body(problema);
	}
}
