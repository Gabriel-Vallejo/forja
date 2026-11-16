package es.forja.entreno.plan.modelo;

import java.util.List;

/** Lo que el plan necesita saber de las sesiones registradas; lo implementa el dominio de sesiones. */
public interface HistorialSesiones {

	/**
	 * Series de trabajo vigentes del ejercicio en la sesión más reciente del usuario (por {@code realizada_en})
	 * que lo incluya; vacía si nunca lo ha hecho.
	 */
	List<SerieTrabajo> ultimasSeriesDeTrabajo(long usuarioId, long ejercicioId);
}
