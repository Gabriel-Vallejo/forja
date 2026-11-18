package es.forja.entreno.perfil.modelo;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

/**
 * Medidas del cuerpo, en cm y kg: es el cuerpo de {@code PUT /api/v1/perfil/medidas} y también lo que se cifra en la
 * base. Todas son opcionales; un {@code null} es una medida que el usuario no ha dado. Rangos amplios pero plausibles.
 */
public record MedidasCuerpo(
		@DecimalMin(value = "100", message = "La altura va de 100 a 250 cm.")
		@DecimalMax(value = "250", message = "La altura va de 100 a 250 cm.") Double alturaCm,
		@DecimalMin(value = "30", message = "El peso va de 30 a 300 kg.")
		@DecimalMax(value = "300", message = "El peso va de 30 a 300 kg.") Double pesoKg,
		@DecimalMin(value = "20", message = "El cuello va de 20 a 70 cm.")
		@DecimalMax(value = "70", message = "El cuello va de 20 a 70 cm.") Double cuelloCm,
		@DecimalMin(value = "40", message = "La cintura va de 40 a 200 cm.")
		@DecimalMax(value = "200", message = "La cintura va de 40 a 200 cm.") Double cinturaCm,
		@DecimalMin(value = "50", message = "La cadera va de 50 a 200 cm.")
		@DecimalMax(value = "200", message = "La cadera va de 50 a 200 cm.") Double caderaCm) {
}
