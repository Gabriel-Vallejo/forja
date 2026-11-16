package es.forja.sesiones.modelo;

import java.util.List;

/** Una página de sesiones, de la más antigua a la más reciente por {@code realizadaEn}. */
public record PaginaSesiones(List<SesionVista> sesiones, int pagina, int tamano, long total) {

	public PaginaSesiones {
		sesiones = List.copyOf(sesiones);
	}
}
