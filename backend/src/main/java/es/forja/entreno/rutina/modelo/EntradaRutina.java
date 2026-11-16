package es.forja.entreno.rutina.modelo;

import java.util.List;
import java.util.Set;

import es.forja.restricciones.modelo.Restriccion;

/**
 * Lo que el usuario pide para su rutina.
 *
 * @param material códigos de la tabla {@code material} que tiene a mano
 * @param exclusiones {@code idExterno} de los ejercicios que no quiere
 * @param prioritarios códigos de músculo, como mucho {@value #MAXIMO_PRIORITARIOS}
 * @param restricciones las del usuario si tiene el consentimiento de limitaciones; si no, ninguna
 */
public record EntradaRutina(int dias, int minutos, Set<String> material, Set<String> exclusiones,
		List<String> prioritarios, int frecuenciaMinima, List<Restriccion> restricciones) {

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
	}

	private static void comprobar(boolean condicion, String mensaje) {
		if (!condicion) {
			throw new IllegalArgumentException(mensaje);
		}
	}
}
