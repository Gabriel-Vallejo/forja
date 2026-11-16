package es.forja.entreno.plan.modelo;

/** Las transiciones de la máquina de estados (RF-26), una función pura de lo que dice la última sesión. */
public final class Transicion {

	/**
	 * @param reglaCumplida todas las series de trabajo llegaron al rango alto con el RIR objetivo o menos
	 * @param estancado el e1RM no mejora en la ventana
	 * @param sesionDeload la sesión es la de descarga que tocaba
	 */
	public record Evidencia(boolean reglaCumplida, boolean estancado, boolean sesionDeload) {
	}

	private Transicion() {
	}

	/** Estado tras registrar una sesión con esa evidencia. */
	public static Estado siguiente(Estado actual, Evidencia evidencia) {
		boolean tocabaDeload = actual == Estado.DELOAD || actual == Estado.ESTANCADO;
		if (tocabaDeload && evidencia.sesionDeload()) {
			return Estado.ACUMULANDO;
		}
		if (evidencia.estancado()) {
			return Estado.ESTANCADO;
		}
		return evidencia.reglaCumplida() ? Estado.SUBIR_CARGA : Estado.ACUMULANDO;
	}

	/** {@code estancado} no es un estado en el que se quede nadie: la siguiente sesión es un deload. */
	public static Estado resolver(Estado estado) {
		return estado == Estado.ESTANCADO ? Estado.DELOAD : estado;
	}
}
