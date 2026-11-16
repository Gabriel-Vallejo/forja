package es.forja.restricciones.modelo;

import java.util.Arrays;
import java.util.Locale;

/** Enum cuyo código coincide con el de las tablas de referencia del catálogo (minúsculas con guion bajo). */
interface ConCodigo {

	String name();

	default String codigo() {
		return name().toLowerCase(Locale.ROOT);
	}

	/** Busca la constante por su código; falla con un mensaje claro si no existe. */
	static <E extends Enum<E> & ConCodigo> E buscar(Class<E> tipo, String codigo) {
		return Arrays.stream(tipo.getEnumConstants())
				.filter(c -> c.codigo().equals(codigo))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException(
						"Código desconocido de " + tipo.getSimpleName() + ": " + codigo));
	}

}
