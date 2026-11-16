package es.forja.restricciones.modelo;

/** Quién puso la restricción: el usuario, o un profesional (que la hace dura). */
public enum Origen implements ConCodigo {
	USUARIO, PROFESIONAL;

	public static Origen desdeCodigo(String codigo) {
		return ConCodigo.buscar(Origen.class, codigo);
	}

}
