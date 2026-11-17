package es.forja.restricciones.bloqueo;

import java.util.Locale;

/** Gravedad de un bloqueo, de menos a más: el estado devuelve el más grave de los activos. */
public enum NivelBloqueo {
	PROFESIONAL_EN_DIAS, PARAR_URGENCIAS, URGENCIAS_HOY;

	public String codigo() {
		return name().toLowerCase(Locale.ROOT);
	}

	public boolean esUrgente() {
		return this != PROFESIONAL_EN_DIAS;
	}
}
