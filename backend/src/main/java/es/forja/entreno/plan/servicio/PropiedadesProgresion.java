package es.forja.entreno.plan.servicio;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code forja.progresion.*} de {@code application.yml}: objetivos por defecto de cada ejercicio y lo que sube la
 * carga según el material (ADR 0008 §5).
 *
 * @param incrementosKg código del material → kg que sube si se cumple la regla
 * @param incrementoPorDefectoKg para el material que no está en el mapa
 */
@ConfigurationProperties("forja.progresion")
public record PropiedadesProgresion(Objetivo compuestos, Objetivo aislados, Map<String, BigDecimal> incrementosKg,
		BigDecimal incrementoPorDefectoKg) {

	/** Rango de repeticiones y RIR objetivo de un tipo de ejercicio. */
	public record Objetivo(int repeticionesMin, int repeticionesMax, int rir) {
		public Objetivo {
			if (repeticionesMin < 1 || repeticionesMin > repeticionesMax || rir < 0) {
				throw new IllegalArgumentException("El objetivo de repeticiones y RIR no es válido.");
			}
		}
	}

	public PropiedadesProgresion {
		incrementosKg = Map.copyOf(incrementosKg);
	}

	public BigDecimal incremento(String codigoMaterial) {
		return incrementosKg.getOrDefault(codigoMaterial, incrementoPorDefectoKg);
	}
}
