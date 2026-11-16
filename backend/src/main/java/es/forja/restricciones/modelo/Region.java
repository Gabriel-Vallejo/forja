package es.forja.restricciones.modelo;

/** Región corporal que carga un ejercicio. Mismos códigos que la tabla {@code region}. */
public enum Region implements ConCodigo {

	COLUMNA_LUMBAR("columna lumbar"),
	COLUMNA_CERVICAL("columna cervical"),
	HOMBRO("hombro"),
	CODO("codo"),
	MUNECA("muñeca"),
	CADERA("cadera"),
	RODILLA("rodilla"),
	TOBILLO("tobillo");

	private final String nombre;

	Region(String nombre) {
		this.nombre = nombre;
	}

	/** Nombre para mostrar al usuario, con sus tildes. */
	public String nombre() {
		return nombre;
	}

	public static Region desdeCodigo(String codigo) {
		return ConCodigo.buscar(Region.class, codigo);
	}

}
