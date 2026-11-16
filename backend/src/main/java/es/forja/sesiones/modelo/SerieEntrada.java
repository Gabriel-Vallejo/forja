package es.forja.sesiones.modelo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Una serie de la sesión. {@code dolor} solo se guarda si el usuario tiene el consentimiento de limitaciones.
 * {@code corrigeA} apunta a la serie que corrige; con {@code anulada} la serie es una anulación de esa otra.
 *
 * @param ejercicio {@code idExterno} del catálogo
 */
public record SerieEntrada(
		@NotNull(message = "Falta el uuid de la serie.") UUID uuid,
		@NotBlank(message = "Falta el ejercicio.") @Size(max = 120, message = "El ejercicio no es válido.")
		String ejercicio,
		@NotNull(message = "Falta el orden de la serie.")
		@Min(value = 0, message = "El orden no es válido.") @Max(value = 1000, message = "El orden no es válido.")
		Integer orden,
		@NotNull(message = "Falta el peso.")
		@DecimalMin(value = "0", message = "El peso va de 0 a 1000 kg.")
		@DecimalMax(value = "1000", message = "El peso va de 0 a 1000 kg.")
		@Digits(integer = 4, fraction = 2, message = "El peso admite como mucho dos decimales.") BigDecimal pesoKg,
		@NotNull(message = "Faltan las repeticiones.")
		@Min(value = 0, message = "Las repeticiones van de 0 a 100.")
		@Max(value = 100, message = "Las repeticiones van de 0 a 100.") Integer repeticiones,
		@Min(value = 0, message = "El RIR va de 0 a 10.") @Max(value = 10, message = "El RIR va de 0 a 10.")
		Integer rir,
		@DecimalMin(value = "1", message = "El RPE va de 1 a 10.")
		@DecimalMax(value = "10", message = "El RPE va de 1 a 10.")
		@Digits(integer = 2, fraction = 1, message = "El RPE admite un decimal.") BigDecimal rpe,
		boolean calentamiento,
		@Min(value = 0, message = "El dolor va de 0 a 10.") @Max(value = 10, message = "El dolor va de 0 a 10.")
		Integer dolor,
		UUID corrigeA,
		boolean anulada,
		@NotNull(message = "Falta la fecha de la serie.") Instant realizadaEn) {
}
