package es.forja.entreno.perfil.modelo;

import java.util.List;

import es.forja.entreno.rutina.modelo.EntradaRutina;
import es.forja.entreno.rutina.modelo.SolicitudRutina;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code PUT /api/v1/perfil/entreno}. Mismas reglas que {@link SolicitudRutina} para días y minutos; el
 * material, además, no puede ir vacío.
 */
public record AjustesEntreno(
		@NotNull(message = "Faltan los días por semana.")
		@Min(value = EntradaRutina.DIAS_MINIMOS, message = "Los días por semana van de 2 a 6.")
		@Max(value = EntradaRutina.DIAS_MAXIMOS, message = "Los días por semana van de 2 a 6.") Integer diasPorSemana,
		@NotNull(message = "Faltan los minutos por sesión.")
		@Min(value = EntradaRutina.MINUTOS_MINIMOS, message = "Los minutos por sesión van de 30 a 120.")
		@Max(value = EntradaRutina.MINUTOS_MAXIMOS, message = "Los minutos por sesión van de 30 a 120.")
		Integer minutosPorSesion,
		@NotNull(message = "Falta el material.") @NotEmpty(message = "Elige al menos un material.")
		@Size(max = SolicitudRutina.MAXIMO_MATERIAL, message = "Demasiado material.")
		List<@NotBlank @Size(max = SolicitudRutina.LARGO_MAXIMO_CODIGO) String> material) {

	/** Lo que recibe quien aún no ha guardado nada (y lo que la app mandaba hasta ahora). */
	public static final AjustesEntreno POR_DEFECTO = new AjustesEntreno(3, 60,
			List.of("barra", "mancuerna", "maquina", "polea", "peso_corporal", "kettlebell", "banda", "barra_ez"));
}
