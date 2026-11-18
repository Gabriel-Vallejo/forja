package es.forja.entreno.plan.modelo;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Preferencias de los ajustes del plan con códigos: cuerpo de {@code PUT /api/v1/planes/preferencias} y lo que se
 * aplica al generar. No son datos de salud.
 *
 * @param exclusiones {@code idExterno} quitados, como mucho {@value #MAXIMO_EXCLUSIONES}
 * @param prioritarios músculos a enfocar, como mucho {@value #MAXIMO_ENFOQUE}
 * @param series series de la semana fijadas por {@code idExterno}
 * @param variedad si prefiere ejercicios distintos a los del plan anterior
 */
public record PreferenciasPlan(
		@NotNull(message = "Faltan las exclusiones.")
		@Size(max = MAXIMO_EXCLUSIONES, message = "Como mucho 4 ejercicios quitados.")
		List<@NotBlank @Size(max = LARGO_ID) String> exclusiones,
		@NotNull(message = "Faltan los músculos a enfocar.")
		@Size(max = MAXIMO_ENFOQUE, message = "Como mucho 2 músculos a enfocar.")
		List<@NotBlank @Size(max = LARGO_CODIGO) String> prioritarios,
		@NotNull(message = "Faltan las series fijadas.")
		@Size(max = MAXIMO_SERIES_FIJADAS, message = "Demasiados ejercicios con series fijadas.")
		Map<@NotBlank @Size(max = LARGO_ID) String, @NotNull @Min(value = SERIES_MINIMAS,
				message = "Las series fijadas van de 2 a 30.") @Max(value = SERIES_MAXIMAS,
						message = "Las series fijadas van de 2 a 30.") Integer> series,
		boolean variedad) {

	public static final int MAXIMO_EXCLUSIONES = 4;
	public static final int MAXIMO_ENFOQUE = 2;
	public static final int MAXIMO_SERIES_FIJADAS = 50;
	/** Los mismos topes que el CHECK de {@code preferencia_ejercicio} (V9). */
	public static final int SERIES_MINIMAS = 2;
	public static final int SERIES_MAXIMAS = 30;
	static final int LARGO_ID = 120;
	static final int LARGO_CODIGO = 30;

	public static final PreferenciasPlan NINGUNA = new PreferenciasPlan(List.of(), List.of(), Map.of(), false);

	public PreferenciasPlan {
		// Nulos solo llegan del cuerpo de la petición, y la validación los rechaza antes de usarlos.
		exclusiones = exclusiones == null ? null : List.copyOf(exclusiones);
		prioritarios = prioritarios == null ? null : List.copyOf(prioritarios);
		series = series == null ? null : Map.copyOf(series);
	}
}
