package es.forja.entreno.plan.modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import es.forja.entreno.rutina.modelo.PlanRutina;
import es.forja.entreno.rutina.modelo.PlanRutina.VolumenMusculo;

/**
 * El plan que saldría con el ajuste, sin guardar, y lo que cambia frente al actual. Solo van los ejercicios y
 * músculos que cambian.
 *
 * @param version uuid del plan actual: se manda de vuelta al aplicar
 * @param preferencias cómo quedarían las preferencias guardadas
 */
public record VistaPrevia(UUID version, PlanRutina plan, List<CambioEjercicio> ejercicios,
		List<CambioMusculo> musculos, PreferenciasPlan preferencias) {

	public VistaPrevia {
		ejercicios = List.copyOf(ejercicios);
		musculos = List.copyOf(musculos);
	}

	/** Series de la semana antes y después: con {@code antes} 0 entra y con {@code despues} 0 sale. */
	public record CambioEjercicio(String idExterno, String nombre, int antes, int despues) {
	}

	/** Series efectivas de la semana (los secundarios a 0,5) antes y después. */
	public record CambioMusculo(String musculo, String nombre, double antes, double despues) {
	}

	/** Series de la semana de cada ejercicio de un plan, por {@code idExterno}. */
	public static Map<String, Integer> seriesSemana(PlanRutina plan) {
		Map<String, Integer> series = new TreeMap<>();
		plan.dias().forEach(d -> d.ejercicios().forEach(e -> series.merge(e.idExterno(), e.series(), Integer::sum)));
		return series;
	}

	/** Ejercicios que entran, salen o cambian de series, por {@code idExterno}. */
	public static List<CambioEjercicio> cambiosDeEjercicios(Map<String, Integer> antes, Map<String, String> nombres,
			PlanRutina despues) {
		Map<String, Integer> nuevas = seriesSemana(despues);
		Map<String, String> todos = new TreeMap<>(nombres);
		despues.dias().forEach(d -> d.ejercicios().forEach(e -> todos.putIfAbsent(e.idExterno(), e.nombre())));
		List<CambioEjercicio> cambios = new ArrayList<>();
		todos.forEach((id, nombre) -> {
			int a = antes.getOrDefault(id, 0);
			int d = nuevas.getOrDefault(id, 0);
			if (a != d) {
				cambios.add(new CambioEjercicio(id, nombre, a, d));
			}
		});
		return cambios;
	}

	/** Músculos cuyas series efectivas cambian, por código. */
	public static List<CambioMusculo> cambiosDeMusculos(List<VolumenMusculo> antes, List<VolumenMusculo> despues) {
		Map<String, VolumenMusculo> a = porCodigo(antes);
		Map<String, VolumenMusculo> d = porCodigo(despues);
		TreeSet<String> codigos = new TreeSet<>(a.keySet());
		codigos.addAll(d.keySet());
		List<CambioMusculo> cambios = new ArrayList<>();
		for (String m : codigos) {
			double sa = a.containsKey(m) ? a.get(m).series() : 0;
			double sd = d.containsKey(m) ? d.get(m).series() : 0;
			if (Double.compare(sa, sd) != 0) {
				cambios.add(new CambioMusculo(m, (d.containsKey(m) ? d : a).get(m).nombre(), sa, sd));
			}
		}
		return cambios;
	}

	private static Map<String, VolumenMusculo> porCodigo(List<VolumenMusculo> volumen) {
		Map<String, VolumenMusculo> mapa = new TreeMap<>();
		volumen.forEach(v -> mapa.put(v.musculo(), v));
		return mapa;
	}
}
