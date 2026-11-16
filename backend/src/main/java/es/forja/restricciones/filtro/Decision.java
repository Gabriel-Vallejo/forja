package es.forja.restricciones.filtro;

import java.util.List;

/** Qué hace el filtro con un ejercicio del catálogo. */
public sealed interface Decision {

	/**
	 * El ejercicio vale tal cual.
	 *
	 * @param anotaciones avisos para el solver (límite de carga, tempo controlado), nunca exclusiones
	 */
	record Permitido(List<String> anotaciones) implements Decision {
		public Permitido {
			anotaciones = List.copyOf(anotaciones);
		}
	}

	/** El ejercicio queda fuera; {@code motivo} explica qué restricción lo saca. */
	record Excluido(String motivo) implements Decision {
	}

	/**
	 * Otra variante ocupa su lugar; las anotaciones son las que aplican al sustituto.
	 *
	 * @param porIdExterno ejercicio curado que lo sustituye
	 */
	record Sustituido(String porIdExterno, String motivo, List<String> anotaciones) implements Decision {
		public Sustituido {
			anotaciones = List.copyOf(anotaciones);
		}
	}

}
