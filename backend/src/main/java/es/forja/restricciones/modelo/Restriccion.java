package es.forja.restricciones.modelo;

import java.util.Objects;

/**
 * Limitación funcional que el usuario declara (no la patología: ADR 0007). Dato de salud (ADR 0004).
 *
 * <p>{@code region} o {@code patron} pueden ser nulos, pero no los dos. {@code parametro} es una
 * categoría o un RPE, nunca grados ni kilos. {@code textoOriginal} es la cita literal cuando la
 * restricción viene del texto libre.
 */
public record Restriccion(
		Region region,
		Lado lado,
		Patron patron,
		TipoRestriccion tipo,
		String parametro,
		Estado estado,
		Origen origen,
		String textoOriginal) {

	public Restriccion {
		Objects.requireNonNull(lado, "lado es obligatorio");
		Objects.requireNonNull(tipo, "tipo es obligatorio");
		Objects.requireNonNull(estado, "estado es obligatorio");
		Objects.requireNonNull(origen, "origen es obligatorio");
		if (region == null && patron == null) {
			throw new IllegalArgumentException("La restricción necesita una región o un patrón");
		}
	}

	/** El histórico sin limitación se guarda, pero el filtro lo ignora. */
	public boolean restringe() {
		return estado != Estado.HISTORICO_SIN_LIMITACION;
	}

}
