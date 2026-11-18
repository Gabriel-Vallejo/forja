package es.forja.entreno.perfil.modelo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Cuerpo y respuesta de {@code /api/v1/perfil/datos}: sexo, experiencia y objetivo. Son preferencias, no dato de
 * salud. {@code experiencia} y {@code objetivo} a {@code null} quieren decir «sin definir». El año de nacimiento sale
 * de la cuenta y no se repite aquí.
 */
public record DatosPerfil(
		@NotNull(message = "Falta el sexo (o «no_indica»).")
		@Pattern(regexp = "hombre|mujer|no_indica", message = "El sexo es hombre, mujer o no_indica.") String sexo,
		@Pattern(regexp = "principiante|intermedio|avanzado",
				message = "La experiencia es principiante, intermedio o avanzado.") String experiencia,
		@Pattern(regexp = "fuerza|hipertrofia|fuerza_hipertrofia",
				message = "El objetivo es fuerza, hipertrofia o fuerza_hipertrofia.") String objetivo) {

	/** Lo que recibe quien aún no ha guardado nada. */
	public static final DatosPerfil POR_DEFECTO = new DatosPerfil(Sexo.NO_INDICA.codigo(), null, null);
}
