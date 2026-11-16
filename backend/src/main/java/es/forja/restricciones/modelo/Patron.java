package es.forja.restricciones.modelo;

/** Patrón de movimiento. Mismos códigos que la tabla {@code patron_movimiento}. */
public enum Patron implements ConCodigo {

	SENTADILLA("sentadilla"),
	BISAGRA("bisagra de cadera"),
	ZANCADA("zancada"),
	EMPUJE_HORIZONTAL("empuje horizontal"),
	EMPUJE_VERTICAL("empuje vertical"),
	TRACCION_HORIZONTAL("tracción horizontal"),
	TRACCION_VERTICAL("tracción vertical"),
	AISLAMIENTO_BRAZO("aislamiento de brazo"),
	AISLAMIENTO_PIERNA("aislamiento de pierna"),
	CORE("core"),
	ACARREO("acarreo"),
	PLIOMETRIA("pliometría"),
	MOVILIDAD("movilidad"),
	CARDIO("cardio");

	private final String nombre;

	Patron(String nombre) {
		this.nombre = nombre;
	}

	/** Nombre para mostrar al usuario, con sus tildes. */
	public String nombre() {
		return nombre;
	}

	public static Patron desdeCodigo(String codigo) {
		return ConCodigo.buscar(Patron.class, codigo);
	}

}
