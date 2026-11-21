package es.forja.entreno.plan.servicio;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import es.forja.entreno.plan.modelo.ParametrosProgresion;

/**
 * {@code forja.progresion.*} de {@code application.yml}: objetivos por defecto de cada ejercicio y lo que sube la
 * carga según el material (ADR 0008 §5).
 *
 * @param incrementosKg código del material → kg que sube si se cumple la regla
 * @param incrementoPorDefectoKg para el material que no está en el mapa
 * @param deload, {@code e1rmRepeticionesMaximas} y {@code pesoCorporalTopeRepeticiones}: la máquina de estados (RF-26)
 * @param seriesPorDefecto para evaluar un ejercicio que no está en el plan
 * @param porEjercicio {@code idExterno} → su propio rango (p. ej. el face pull, de 12 a 20); opcional
 */
@ConfigurationProperties("forja.progresion")
public record PropiedadesProgresion(Objetivo compuestos, Objetivo aislados, Map<String, BigDecimal> incrementosKg,
		BigDecimal incrementoPorDefectoKg, ParametrosProgresion.Deload deload, int e1rmRepeticionesMaximas,
		int pesoCorporalTopeRepeticiones, int seriesPorDefecto, Map<String, Objetivo> porEjercicio) {

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
		porEjercicio = porEjercicio == null ? Map.of() : Map.copyOf(porEjercicio);
	}

	/** El rango propio del ejercicio si lo tiene; si no, el de los compuestos o el de los aislados. */
	public Objetivo objetivo(String idExterno, boolean compuesto) {
		return porEjercicio.getOrDefault(idExterno, compuesto ? compuestos : aislados);
	}

	public ParametrosProgresion parametros() {
		return new ParametrosProgresion(deload, e1rmRepeticionesMaximas, pesoCorporalTopeRepeticiones);
	}

	public BigDecimal incremento(String codigoMaterial) {
		return incrementosKg.getOrDefault(codigoMaterial, incrementoPorDefectoKg);
	}
}
