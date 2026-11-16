package es.forja.entreno.rutina.modelo;

import java.util.Map;
import java.util.Objects;

import es.forja.restricciones.filtro.EjercicioCandidato;
import es.forja.restricciones.modelo.Patron;

/**
 * Ejercicio del catálogo visto por el generador de la rutina: lo que mira el filtro más lo que necesita el modelo.
 *
 * @param musculos código del músculo → peso doble: {@value #PESO_PRIMARIO} si es primario y
 *            {@value #PESO_SECUNDARIO} si es secundario (Pelland 2026), para trabajar en enteros
 * @param nombreMaterial nombre del implemento para los motivos de descarte
 */
public record EjercicioRutina(EjercicioCandidato candidato, boolean aptoFuerza, boolean compuesto,
		boolean posicionEstirada, String nombreMaterial, Map<String, Integer> musculos) {

	public static final int PESO_PRIMARIO = 2;
	public static final int PESO_SECUNDARIO = 1;

	public EjercicioRutina {
		Objects.requireNonNull(candidato, "candidato es obligatorio");
		Objects.requireNonNull(nombreMaterial, "nombreMaterial es obligatorio");
		musculos = Map.copyOf(musculos);
	}

	public String idExterno() {
		return candidato.idExterno();
	}

	public String nombre() {
		return candidato.nombre();
	}

	public Patron patron() {
		return candidato.patron();
	}

	/** Básico pesado: compuesto con carga axial. No se repite en días seguidos. */
	public boolean basicoPesado() {
		return compuesto && candidato.cargaAxial();
	}

	/** Lo que trabaja el músculo como primario, 0 si no lo trabaja. */
	public int pesoDoble(String musculo) {
		return musculos.getOrDefault(musculo, 0);
	}
}
