package es.forja.entreno.catalogo.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import es.forja.entreno.catalogo.modelo.Ejercicio;

public interface EjercicioRepositorio extends JpaRepository<Ejercicio, Long> {

	/** Variantes de una familia de sustitución con el origen de etiquetado dado, por nombre en español. */
	List<Ejercicio> findByFamiliaSustitucionAndOrigenEtiquetadoOrderByNombreEsAsc(
			String familiaSustitucion, String origenEtiquetado);

}
