package es.forja.restricciones.modelo;

/** Qué se le pide al motor para la región o el patrón nombrados (ADR 0007). */
public enum TipoRestriccion implements ConCodigo {

	EVITAR("evitar"),
	LIMITAR_RANGO("limitar rango"),
	LIMITAR_CARGA("limitar carga"),
	SIN_CARGA_AXIAL("sin carga axial"),
	SIN_IMPACTO("sin impacto"),
	SIN_AGARRE_PESADO("sin agarre pesado"),
	SIN_POR_ENCIMA_CABEZA("sin trabajo por encima de la cabeza"),
	TEMPO_CONTROLADO("tempo controlado");

	private final String descripcion;

	TipoRestriccion(String descripcion) {
		this.descripcion = descripcion;
	}

	/** Texto corto para los motivos que se enseñan al usuario. */
	public String descripcion() {
		return descripcion;
	}

}
