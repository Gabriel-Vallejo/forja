package es.forja.sesiones.modelo;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code PUT /api/v1/sesiones/{uuid}}: la sesión entera, con su cabecera y todas sus series (ADR 0008 §2).
 *
 * @param planVersion versión del plan con la que se entrenó; nula si se entrenó sin plan
 * @param realizadaEn reloj del móvil
 */
public record SesionEntrada(
		UUID planVersion,
		@Min(value = 1, message = "El día del plan va de 1 a 7.")
		@Max(value = 7, message = "El día del plan va de 1 a 7.") Integer diaPlan,
		@NotNull(message = "Falta la fecha de la sesión.") Instant realizadaEn,
		@NotEmpty(message = "La sesión no tiene series.")
		@Size(max = SesionEntrada.MAXIMO_SERIES, message = "Demasiadas series en una sesión.")
		List<@NotNull(message = "Hay una serie vacía.") @Valid SerieEntrada> series) {

	public static final int MAXIMO_SERIES = 200;
}
