package es.forja.entreno.perfil.modelo;

import org.jspecify.annotations.Nullable;

/**
 * Respuesta de {@code /api/v1/perfil/medidas}: las medidas tal como se guardaron y el % de grasa estimado, que se
 * calcula al leer y no se guarda. {@code grasaEstimada} es {@code null} si faltan datos.
 */
public record MedidasPerfil(@Nullable Double alturaCm, @Nullable Double pesoKg, @Nullable Double cuelloCm,
		@Nullable Double cinturaCm, @Nullable Double caderaCm, @Nullable GrasaEstimada grasaEstimada) {

	/** Estimación con su margen de error, en puntos porcentuales: {@code porcentaje ± margen}. */
	public record GrasaEstimada(double porcentaje, double margen) {
	}

	public static MedidasPerfil de(MedidasCuerpo m, GrasaEstimada grasa) {
		return new MedidasPerfil(m.alturaCm(), m.pesoKg(), m.cuelloCm(), m.cinturaCm(), m.caderaCm(), grasa);
	}
}
