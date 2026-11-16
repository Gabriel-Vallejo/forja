package es.forja.restricciones.filtro;

import es.forja.entreno.catalogo.modelo.Ejercicio;
import es.forja.restricciones.modelo.Patron;
import es.forja.restricciones.modelo.Region;

/** Construye el {@link EjercicioCandidato} desde la entidad del catálogo. Hay que llamarlo con la sesión abierta. */
public final class AdaptadorCatalogo {

	private static final String ORIGEN_CURADO = "curado";

	private AdaptadorCatalogo() {
	}

	public static EjercicioCandidato desde(Ejercicio e) {
		String nombre = e.getNombreEs() != null ? e.getNombreEs() : e.getNombreOriginal();
		return new EjercicioCandidato(
				e.getIdExterno(),
				nombre,
				Patron.desdeCodigo(e.getPatron().getCodigo()),
				e.getRegiones().stream().map(r -> Region.desdeCodigo(r.getCodigo()))
						.collect(java.util.stream.Collectors.toSet()),
				e.isCargaAxial(),
				e.isImpacto(),
				Agarre.valueOf(e.getAgarre().toUpperCase(java.util.Locale.ROOT)),
				e.isPorEncimaCabeza(),
				e.getFamiliaSustitucion(),
				RangoMovimiento.valueOf(e.getRangoMovimiento().toUpperCase(java.util.Locale.ROOT)),
				e.getImplemento().getCodigo(),
				ORIGEN_CURADO.equals(e.getOrigenEtiquetado()));
	}

}
