package es.forja.cuenta.modelo;

/** Para qué sirve un token de un solo uso; coincide con el ENUM de {@code token_un_uso}. */
public enum TipoTokenUnUso {
	VERIFICACION("verificacion"), RECUPERACION("recuperacion");

	private final String codigo;

	TipoTokenUnUso(String codigo) {
		this.codigo = codigo;
	}

	public String codigo() {
		return codigo;
	}
}
