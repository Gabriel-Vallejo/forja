package es.forja.restricciones.modelo;

/** Situación de la limitación; el histórico sin limitación se guarda pero no restringe nada. */
public enum Estado implements ConCodigo {
	ACTUAL, ULTIMOS_12_MESES, HISTORICO_SIN_LIMITACION
}
