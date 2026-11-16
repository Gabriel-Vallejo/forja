package es.forja.entreno.plan.modelo;

import com.fasterxml.jackson.annotation.JsonValue;

/** Estado de progresión de un ejercicio (RF-26). */
public enum Estado {
	/** Repite la carga: todavía no se cumple la regla. */
	ACUMULANDO("acumulando"),
	/** Se cumplió la regla: sube la carga (o, con peso corporal, el rango de repeticiones). */
	SUBIR_CARGA("subir_carga"),
	/** El e1RM no mejora: paso intermedio que siempre se resuelve en {@link #DELOAD}. */
	ESTANCADO("estancado"),
	/** La siguiente sesión es de descarga: menos carga y menos series. */
	DELOAD("deload");

	private final String codigo;

	Estado(String codigo) {
		this.codigo = codigo;
	}

	@JsonValue
	public String codigo() {
		return codigo;
	}
}
