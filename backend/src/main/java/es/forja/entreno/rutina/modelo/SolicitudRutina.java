package es.forja.entreno.rutina.modelo;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import es.forja.entreno.movilidad.modelo.TestMovilidad;
import es.forja.entreno.movilidad.modelo.Tramo;

import es.forja.restricciones.modelo.Restriccion;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code POST /api/v1/rutinas/generar}: la entrada sin las restricciones, que las pone el servidor si el
 * usuario tiene el consentimiento de limitaciones.
 *
 * @param frecuenciaMinima 1 o 2; si falta, 2
 */
public record SolicitudRutina(
		@NotNull(message = "Faltan los días por semana.")
		@Min(value = EntradaRutina.DIAS_MINIMOS, message = "Los días por semana van de 2 a 6.")
		@Max(value = EntradaRutina.DIAS_MAXIMOS, message = "Los días por semana van de 2 a 6.") Integer diasPorSemana,
		@NotNull(message = "Faltan los minutos por sesión.")
		@Min(value = EntradaRutina.MINUTOS_MINIMOS, message = "Los minutos por sesión van de 30 a 120.")
		@Max(value = EntradaRutina.MINUTOS_MAXIMOS, message = "Los minutos por sesión van de 30 a 120.")
		Integer minutosPorSesion,
		@NotNull(message = "Falta el material.")
		@Size(max = MAXIMO_MATERIAL, message = "Demasiado material.")
		List<@NotBlank @Size(max = LARGO_MAXIMO_CODIGO) String> material,
		@Size(max = MAXIMO_EXCLUSIONES, message = "Demasiadas exclusiones.")
		List<@NotBlank @Size(max = LARGO_MAXIMO_ID) String> exclusiones,
		@Size(max = EntradaRutina.MAXIMO_PRIORITARIOS, message = "Como mucho 3 músculos prioritarios.")
		List<@NotBlank @Size(max = LARGO_MAXIMO_CODIGO) String> prioritarios,
		@Min(value = EntradaRutina.FRECUENCIA_MINIMA, message = "La frecuencia mínima es 1 o 2.")
		@Max(value = EntradaRutina.FRECUENCIA_POR_DEFECTO, message = "La frecuencia mínima es 1 o 2.")
		Integer frecuenciaMinima) {

	public static final int MAXIMO_MATERIAL = 20;
	public static final int MAXIMO_EXCLUSIONES = 100;
	public static final int LARGO_MAXIMO_CODIGO = 30;
	public static final int LARGO_MAXIMO_ID = 120;

	/** Con las preferencias guardadas: sus exclusiones se suman y sus prioritarios cuentan si aquí no hay. */
	public EntradaRutina aEntrada(List<Restriccion> restricciones, Map<TestMovilidad, Tramo> movilidad,
			Preferencias preferencias) {
		Set<String> todas = new HashSet<>(preferencias.exclusiones());
		if (exclusiones != null) {
			todas.addAll(exclusiones);
		}
		List<String> enfoque = prioritarios == null || prioritarios.isEmpty() ? preferencias.prioritarios()
				: prioritarios;
		return new EntradaRutina(diasPorSemana, minutosPorSesion, Set.copyOf(material), todas, enfoque,
				frecuenciaMinima == null ? EntradaRutina.FRECUENCIA_POR_DEFECTO : frecuenciaMinima, restricciones,
				movilidad, preferencias.series(), preferencias.evitar(), preferencias.partida());
	}
}
