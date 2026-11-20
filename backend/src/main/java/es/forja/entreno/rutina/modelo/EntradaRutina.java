package es.forja.entreno.rutina.modelo;

import java.util.List;
import java.util.Map;
import java.util.Set;

import es.forja.entreno.movilidad.modelo.TestMovilidad;
import es.forja.entreno.movilidad.modelo.Tramo;
import es.forja.restricciones.modelo.Restriccion;

/**
 * Lo que el usuario pide para su rutina.
 *
 * @param material códigos de la tabla {@code material} que tiene a mano
 * @param exclusiones {@code idExterno} de los ejercicios que no quiere
 * @param prioritarios códigos de músculo, como mucho {@value #MAXIMO_PRIORITARIOS}
 * @param restricciones las del usuario si tiene el consentimiento de limitaciones; si no, ninguna
 * @param movilidad tramo de cada test hecho, si tiene el consentimiento de medidas de salud; si no, vacío
 * @param seriesSemana series de la semana fijadas por el usuario para un {@code idExterno} («una serie más o menos»)
 * @param evitar {@code idExterno} que pierden puntos por serie: los del plan anterior si pidió más variedad
 * @param partida plan del que parte un ajuste; {@link Partida#NINGUNA} al generar desde cero
 */
public record EntradaRutina(int dias, int minutos, Set<String> material, Set<String> exclusiones,
		List<String> prioritarios, int frecuenciaMinima, List<Restriccion> restricciones,
		Map<TestMovilidad, Tramo> movilidad, Map<String, Integer> seriesSemana, Set<String> evitar,
		Partida partida) {

	public static final int DIAS_MINIMOS = 2;
	public static final int DIAS_MAXIMOS = 6;
	public static final int MINUTOS_MINIMOS = 30;
	public static final int MINUTOS_MAXIMOS = 120;
	public static final int MAXIMO_PRIORITARIOS = 3;
	public static final int FRECUENCIA_MINIMA = 1;
	public static final int FRECUENCIA_POR_DEFECTO = 2;

	public EntradaRutina {
		comprobar(dias >= DIAS_MINIMOS && dias <= DIAS_MAXIMOS, "Los días por semana van de 2 a 6.");
		comprobar(minutos >= MINUTOS_MINIMOS && minutos <= MINUTOS_MAXIMOS, "Los minutos van de 30 a 120.");
		comprobar(prioritarios.size() <= MAXIMO_PRIORITARIOS, "Como mucho 3 músculos prioritarios.");
		comprobar(frecuenciaMinima >= FRECUENCIA_MINIMA && frecuenciaMinima <= FRECUENCIA_POR_DEFECTO,
				"La frecuencia mínima es 1 o 2.");
		material = Set.copyOf(material);
		exclusiones = Set.copyOf(exclusiones);
		prioritarios = List.copyOf(prioritarios);
		restricciones = List.copyOf(restricciones);
		movilidad = Map.copyOf(movilidad);
		seriesSemana = Map.copyOf(seriesSemana);
		evitar = Set.copyOf(evitar);
	}

	/**
	 * El plan actual del que parte un ajuste: cambiarlo cuesta en el objetivo, salvo los primeros
	 * {@code cambiosLibres}.
	 *
	 * @param series series por día (índice 0 = día 1) de cada {@code idExterno}
	 * @param cambiosLibres series o presencias por día que pueden cambiar sin coste («enfocar»); 0 en el resto
	 */
	public record Partida(Map<String, List<Integer>> series, int cambiosLibres) {

		public static final Partida NINGUNA = new Partida(Map.of(), 0);

		public Partida {
			series = Map.copyOf(series);
		}
	}

	/** Sin preferencias de los ajustes del plan. */
	public EntradaRutina(int dias, int minutos, Set<String> material, Set<String> exclusiones,
			List<String> prioritarios, int frecuenciaMinima, List<Restriccion> restricciones,
			Map<TestMovilidad, Tramo> movilidad) {
		this(dias, minutos, material, exclusiones, prioritarios, frecuenciaMinima, restricciones, movilidad, Map.of(),
				Set.of(), Partida.NINGUNA);
	}

	/** La misma entrada sin las series fijadas: cuando no caben, el plan se genera sin ellas. */
	public EntradaRutina sinSeriesFijadas() {
		return new EntradaRutina(dias, minutos, material, exclusiones, prioritarios, frecuenciaMinima, restricciones,
				movilidad, Map.of(), evitar, partida);
	}

	/** La misma entrada sin plan de partida: el «por qué este y no aquel» compara sin el coste de cambiar. */
	public EntradaRutina sinPartida() {
		return new EntradaRutina(dias, minutos, material, exclusiones, prioritarios, frecuenciaMinima, restricciones,
				movilidad, seriesSemana, evitar, Partida.NINGUNA);
	}

	/** Sin tests de movilidad. */
	public EntradaRutina(int dias, int minutos, Set<String> material, Set<String> exclusiones,
			List<String> prioritarios, int frecuenciaMinima, List<Restriccion> restricciones) {
		this(dias, minutos, material, exclusiones, prioritarios, frecuenciaMinima, restricciones, Map.of());
	}

	private static void comprobar(boolean condicion, String mensaje) {
		if (!condicion) {
			throw new IllegalArgumentException(mensaje);
		}
	}
}
