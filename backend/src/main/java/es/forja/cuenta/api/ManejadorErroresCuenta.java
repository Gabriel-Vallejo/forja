package es.forja.cuenta.api;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import es.forja.cuenta.servicio.ErrorCuenta;

/**
 * Traduce los fallos de negocio de la cuenta a {@code ProblemDetail}, con {@code Retry-After} en los 429. Vale para
 * toda la API: el límite de intentos también lo usan otros dominios (la generación de la rutina).
 */
@RestControllerAdvice
public class ManejadorErroresCuenta {

	@ExceptionHandler(ErrorCuenta.class)
	ResponseEntity<ProblemDetail> alFallar(ErrorCuenta error) {
		ResponseEntity.BodyBuilder respuesta = ResponseEntity.status(error.estado());
		if (error.segundosDeEspera() > 0) {
			respuesta.header(HttpHeaders.RETRY_AFTER, Long.toString(error.segundosDeEspera()));
		}
		return respuesta.body(ProblemDetail.forStatusAndDetail(error.estado(), error.getMessage()));
	}
}
