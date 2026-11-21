package es.forja.entreno.rutina.modelo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Plan semanal y su traza (ADR 0002 §5 y enmiendas). Se guarda como versión inmutable (ADR 0008), sin la parte de la
 * traza que cita las limitaciones del usuario.
 *
 * @param estado {@value #OPTIMO} o {@value #FACTIBLE}
 * @param objetivo valor del objetivo que maximiza el solver
 * @param notas avisos generales del plan, por ejemplo si hubo que bajar el mínimo de ejercicios por sesión
 */
public record PlanRutina(String estado, String resumen, long objetivo, List<DiaPlan> dias,
		List<VolumenMusculo> volumen, List<Holgura> holguras, List<ContribucionEjercicio> contribuciones,
		List<Alternativa> alternativas, List<Descarte> descartes, List<String> notas) {

	public static final String OPTIMO = "OPTIMAL";
	public static final String FACTIBLE = "FEASIBLE";

	public PlanRutina {
		dias = List.copyOf(dias);
		volumen = List.copyOf(volumen);
		holguras = List.copyOf(holguras);
		contribuciones = List.copyOf(contribuciones);
		alternativas = List.copyOf(alternativas);
		descartes = List.copyOf(descartes);
		notas = List.copyOf(notas);
	}

	/** @param segundos duración estimada con los tiempos de la configuración */
	public record DiaPlan(int dia, int segundos, List<EjercicioPlan> ejercicios) {
		public DiaPlan {
			ejercicios = List.copyOf(ejercicios);
		}
	}

	/** @param anotaciones avisos del filtro (límite de carga, tempo controlado) */
	public record EjercicioPlan(String idExterno, String nombre, String patron, int series,
			boolean posicionEstirada, List<String> anotaciones) {
		public EjercicioPlan {
			anotaciones = List.copyOf(anotaciones);
		}
	}

	/**
	 * Series efectivas de un músculo frente a su rango.
	 *
	 * @param minimo y {@code objetivo}: nulos si el músculo no tiene meta de volumen
	 */
	public record VolumenMusculo(String musculo, String nombre, double series, @Nullable Integer minimo,
			@Nullable Integer objetivo, int maximo, String fuente) {
	}

	/**
	 * @param codigo músculo, o patrón si el tipo es {@code basico}
	 * @param tipo {@code volumen}, {@code frecuencia} o {@code basico}; {@code faltan}, en series, días o patrones
	 */
	public record Holgura(String codigo, String tipo, double faltan, String mensaje) {
	}

	/** Lo que aporta el ejercicio a cada término del objetivo, en puntos. */
	public record ContribucionEjercicio(String idExterno, String nombre, Map<String, Long> terminos) {
		public ContribucionEjercicio {
			// en orden fijo, para que la respuesta salga siempre igual
			terminos = Collections.unmodifiableMap(new LinkedHashMap<>(terminos));
		}
	}

	/**
	 * «Por qué este y no aquel»: se prohíbe el elegido, se fuerza la alternativa y se vuelve a resolver.
	 *
	 * @param diferencia puntos que pierde el plan con la alternativa; nula si no cabe o no hubo respuesta
	 */
	public record Alternativa(String elegido, String alternativa, String nombreAlternativa, @Nullable Long diferencia,
			String mensaje) {
	}

	/** @param tipo {@code material}, {@code exclusion}, {@code restriccion} o {@code sustitucion} */
	public record Descarte(String idExterno, String nombre, String tipo, String motivo) {
	}
}
