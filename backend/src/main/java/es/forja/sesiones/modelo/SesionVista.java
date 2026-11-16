package es.forja.sesiones.modelo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Una sesión con sus series vigentes (sin las corregidas ni las anuladas).
 *
 * @param relojDesfasado más de 24 h entre {@code realizadaEn} (móvil) y {@code recibidaEn} (servidor): para revisar
 */
public record SesionVista(UUID uuid, UUID planVersion, Integer diaPlan, Instant realizadaEn, Instant recibidaEn,
		boolean relojDesfasado, List<SerieVista> series) {

	public SesionVista {
		series = List.copyOf(series);
	}

	/** @param dolor nulo si no se guardó o si el usuario ya no tiene el consentimiento de limitaciones */
	public record SerieVista(UUID uuid, String ejercicio, int orden, BigDecimal pesoKg, int repeticiones,
			Integer rir, BigDecimal rpe, boolean calentamiento, Integer dolor, UUID corrigeA, boolean anulada,
			Instant realizadaEn) {
	}
}
