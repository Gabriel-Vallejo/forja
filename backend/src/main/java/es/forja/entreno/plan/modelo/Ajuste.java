package es.forja.entreno.plan.modelo;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Un ajuste del plan (PRD v0.1), para la vista previa y para aplicarlo.
 *
 * <ul>
 * <li>{@code mas} / {@code menos}: una serie más o menos a la semana de {@code idExterno}, que queda fijada.</li>
 * <li>{@code no_recomendar} / {@code reemplazar}: {@code idExterno} no vuelve a salir y el solver elige qué entra.</li>
 * <li>{@code variedad}: prefiere ejercicios distintos a los del plan anterior.</li>
 * <li>{@code enfocar}: {@code musculos} (0 a 2) pasan a ser los prioritarios; vacío quita el enfoque.</li>
 * </ul>
 *
 * @param version uuid del plan sobre el que se vio la vista previa; al aplicar es obligatorio
 */
public record Ajuste(
		@NotNull(message = "Falta el tipo de ajuste.")
		@Pattern(regexp = MAS + "|" + MENOS + "|" + NO_RECOMENDAR + "|" + REEMPLAZAR + "|" + VARIEDAD + "|" + ENFOCAR,
				message = "Ese ajuste no existe.") String tipo,
		@Size(max = PreferenciasPlan.LARGO_ID) String idExterno,
		@Size(max = PreferenciasPlan.MAXIMO_ENFOQUE, message = "Como mucho 2 músculos a enfocar.")
		List<@NotBlank @Size(max = PreferenciasPlan.LARGO_CODIGO) String> musculos,
		UUID version) {

	public static final String MAS = "mas";
	public static final String MENOS = "menos";
	public static final String NO_RECOMENDAR = "no_recomendar";
	public static final String REEMPLAZAR = "reemplazar";
	public static final String VARIEDAD = "variedad";
	public static final String ENFOCAR = "enfocar";

	/** Si el ajuste es de un ejercicio del plan (y no de todo el plan). */
	public boolean deEjercicio() {
		return !VARIEDAD.equals(tipo) && !ENFOCAR.equals(tipo);
	}
}
