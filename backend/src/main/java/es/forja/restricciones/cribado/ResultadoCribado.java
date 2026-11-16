package es.forja.restricciones.cribado;

import java.util.List;

/** Resultado del cribado: qué hacer y, por cada pregunta que lo motiva, el porqué. */
public record ResultadoCribado(Nivel nivel, List<String> motivos) {

	public enum Nivel {
		SEGUIR, CONSULTAR_PROFESIONAL, PARAR_URGENCIAS
	}

	public ResultadoCribado {
		motivos = List.copyOf(motivos);
	}

}
