package es.forja.sesiones.modelo;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Un ejercicio que ha dolido de forma repetida (RF-27, ADR 0007 §4): en al menos {@link #MINIMO_CON_DOLOR} de sus
 * {@link #SESIONES_MIRADAS} últimas sesiones, alguna serie de trabajo pasó de {@link #DOLOR_ACEPTABLE}. La app lo dice
 * y propone cambiarlo; nunca se quita sin preguntar.
 *
 * <p>Los tres números son una [propuesta], heurística y no diagnóstico. El umbral sale del modelo de monitorización
 * del dolor (Silbernagel y otros, Am J Sports Med 2007, doi:10.1177/0363546506298279), que deja seguir entrenando
 * con dolor; el «hasta 5 sobre 10» lo citan resúmenes del estudio, pero no está en el abstract: falta comprobarlo en
 * el texto completo.
 */
public record DolorRepetido(String idExterno, int sesionesConDolor, int sesionesMiradas) {

	public static final int DOLOR_ACEPTABLE = 5;
	public static final int SESIONES_MIRADAS = 3;
	public static final int MINIMO_CON_DOLOR = 2;

	/** Una serie de trabajo vigente; {@code dolor} es null si no se anotó. */
	public record SerieConDolor(UUID sesion, Instant realizadaEn, String ejercicio, Integer dolor) {
	}

	/** Lo que pesa de una sesión para un ejercicio: el dolor más alto que se anotó en ella. */
	private record Sesion(UUID uuid, Instant realizadaEn, int dolorMaximo) {
	}

	/** Los ejercicios marcados, por orden de {@code idExterno}. */
	public static List<DolorRepetido> de(List<SerieConDolor> series) {
		Map<String, List<SerieConDolor>> porEjercicio = series.stream()
				.collect(Collectors.groupingBy(SerieConDolor::ejercicio, TreeMap::new, Collectors.toList()));
		return porEjercicio.entrySet().stream().map(e -> evaluar(e.getKey(), e.getValue())).filter(Objects::nonNull)
				.toList();
	}

	private static DolorRepetido evaluar(String ejercicio, List<SerieConDolor> series) {
		List<Sesion> ultimas = series.stream().collect(Collectors.groupingBy(SerieConDolor::sesion)).values().stream()
				.map(DolorRepetido::sesion)
				.sorted(Comparator.comparing(Sesion::realizadaEn).thenComparing(Sesion::uuid).reversed())
				.limit(SESIONES_MIRADAS).toList();
		int conDolor = (int) ultimas.stream().filter(s -> s.dolorMaximo() > DOLOR_ACEPTABLE).count();
		return conDolor >= MINIMO_CON_DOLOR ? new DolorRepetido(ejercicio, conDolor, ultimas.size()) : null;
	}

	private static Sesion sesion(List<SerieConDolor> deUnaSesion) {
		SerieConDolor primera = deUnaSesion.get(0);
		int maximo = deUnaSesion.stream().map(SerieConDolor::dolor).filter(Objects::nonNull)
				.mapToInt(Integer::intValue).max().orElse(0);
		return new Sesion(primera.sesion(), primera.realizadaEn(), maximo);
	}
}
