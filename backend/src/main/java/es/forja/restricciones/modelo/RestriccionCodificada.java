package es.forja.restricciones.modelo;

import java.util.function.Function;

/**
 * La {@link Restriccion} con sus enums como códigos de texto ({@code sin_carga_axial}, {@code rodilla}…):
 * es la forma de la API y la que se serializa y se cifra en la base. Los mensajes de error nombran el campo,
 * nunca el valor recibido.
 */
public record RestriccionCodificada(
		String region,
		String lado,
		String patron,
		String tipo,
		String parametro,
		String estado,
		String origen,
		String textoOriginal) {

	public static RestriccionCodificada de(Restriccion r) {
		return new RestriccionCodificada(
				r.region() == null ? null : r.region().codigo(),
				r.lado().codigo(),
				r.patron() == null ? null : r.patron().codigo(),
				r.tipo().codigo(),
				r.parametro(),
				r.estado().codigo(),
				r.origen().codigo(),
				r.textoOriginal());
	}

	/** Falla con {@link IllegalArgumentException} si falta un campo obligatorio o un código no existe. */
	public Restriccion aRestriccion() {
		return new Restriccion(
				opcional(region, Region::desdeCodigo, "region"),
				obligatorio(lado, Lado::desdeCodigo, "lado"),
				opcional(patron, Patron::desdeCodigo, "patron"),
				obligatorio(tipo, TipoRestriccion::desdeCodigo, "tipo"),
				parametro,
				obligatorio(estado, Estado::desdeCodigo, "estado"),
				obligatorio(origen, Origen::desdeCodigo, "origen"),
				textoOriginal);
	}

	private static <E> E obligatorio(String codigo, Function<String, E> buscar, String campo) {
		if (codigo == null || codigo.isBlank()) {
			throw new IllegalArgumentException("Falta el campo «" + campo + "».");
		}
		return opcional(codigo, buscar, campo);
	}

	private static <E> E opcional(String codigo, Function<String, E> buscar, String campo) {
		if (codigo == null) {
			return null;
		}
		try {
			return buscar.apply(codigo);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("El valor del campo «" + campo + "» no es válido.");
		}
	}
}
