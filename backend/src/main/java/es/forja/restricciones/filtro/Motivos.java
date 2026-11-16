package es.forja.restricciones.filtro;

import es.forja.restricciones.modelo.Origen;
import es.forja.restricciones.modelo.Restriccion;

/** Textos en español de las decisiones, citando siempre la restricción que las provoca. */
final class Motivos {

	private Motivos() {
	}

	/** Por ejemplo «sin carga axial en sentadilla» o «evitar en hombro (profesional)». */
	static String describir(Restriccion r) {
		String objetivo = r.region() == null ? r.patron().nombre()
				: r.patron() == null ? r.region().nombre() : r.region().nombre() + " y " + r.patron().nombre();
		String origen = r.origen() == Origen.PROFESIONAL ? " (profesional)" : "";
		return r.tipo().descripcion() + " en " + objetivo + origen;
	}

	static String excluido(Restriccion r, EjercicioCandidato e) {
		return describir(r) + ": se excluye " + minusculaInicial(e.nombre());
	}

	static String sinVariante(Restriccion r, EjercicioCandidato e) {
		return describir(r) + ": sin variante compatible para " + minusculaInicial(e.nombre());
	}

	static String sustituido(Restriccion r, EjercicioCandidato original, EjercicioCandidato sustituto) {
		return describir(r) + ": " + minusculaInicial(sustituto.nombre()) + " en vez de "
				+ minusculaInicial(original.nombre());
	}

	private static String minusculaInicial(String texto) {
		return texto.isEmpty() ? texto : Character.toLowerCase(texto.charAt(0)) + texto.substring(1);
	}

}
