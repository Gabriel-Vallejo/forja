package es.forja.restricciones.cribado;

import java.util.ArrayList;
import java.util.List;

import es.forja.restricciones.cribado.ResultadoCribado.Nivel;

/**
 * Reglas del cribado. Cualquier «sí» recomienda hablar con un profesional; dolor en el pecho o
 * desmayo en esfuerzo recientes mandan a urgencias. Lo decide este código, nunca un modelo de lenguaje.
 */
public class Cribado {

	public ResultadoCribado evaluar(RespuestasCribado respuestas) {
		List<String> motivos = new ArrayList<>();
		Nivel nivel = Nivel.SEGUIR;
		for (PreguntaCribado p : PreguntaCribado.values()) {
			if (!respuestas.si(p)) {
				continue;
			}
			boolean urgente = respuestas.recientes().contains(p);
			motivos.add(urgente ? p.motivoUrgente() : p.motivo());
			nivel = urgente ? Nivel.PARAR_URGENCIAS : peor(nivel, Nivel.CONSULTAR_PROFESIONAL);
		}
		return new ResultadoCribado(nivel, motivos);
	}

	private static Nivel peor(Nivel actual, Nivel nuevo) {
		return actual == Nivel.PARAR_URGENCIAS ? actual : nuevo;
	}

}
