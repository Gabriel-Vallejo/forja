package es.forja.restricciones.filtro;

import java.util.Objects;
import java.util.Set;

import es.forja.restricciones.modelo.Patron;
import es.forja.restricciones.modelo.Region;

/**
 * Ejercicio del catálogo visto por el filtro: solo lo que hace falta para aplicar restricciones.
 * No depende de JPA; el adaptador {@link AdaptadorCatalogo} lo construye desde la entidad.
 *
 * @param nombre texto para los motivos que se enseñan al usuario
 * @param familiaSustitucion puede ser nula en los ejercicios sin familia
 * @param curado solo los curados pueden entrar como sustitutos
 */
public record EjercicioCandidato(
		String idExterno,
		String nombre,
		Patron patron,
		Set<Region> regiones,
		boolean cargaAxial,
		boolean impacto,
		Agarre agarre,
		boolean porEncimaCabeza,
		String familiaSustitucion,
		RangoMovimiento rango,
		String implemento,
		boolean curado) {

	public EjercicioCandidato {
		if (idExterno == null || idExterno.isBlank()) {
			throw new IllegalArgumentException("idExterno es obligatorio");
		}
		Objects.requireNonNull(nombre, "nombre es obligatorio");
		Objects.requireNonNull(patron, "patron es obligatorio");
		Objects.requireNonNull(agarre, "agarre es obligatorio");
		Objects.requireNonNull(rango, "rango es obligatorio");
		Objects.requireNonNull(implemento, "implemento es obligatorio");
		regiones = Set.copyOf(regiones);
	}

}
