package es.forja.entreno.plan.modelo;

import java.util.List;

/** Lo que el plan necesita saber de las sesiones registradas; lo implementa el dominio de sesiones. */
public interface HistorialSesiones {

	/**
	 * Las sesiones del usuario que incluyen el ejercicio, con sus series de trabajo vigentes (sin calentamiento, sin
	 * las corregidas ni las anuladas). Vacía si nunca lo ha hecho. El orden de la lista no importa.
	 */
	List<SesionEjercicio> sesionesDeTrabajo(long usuarioId, long ejercicioId);
}
