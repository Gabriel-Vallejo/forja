package es.forja.entreno.catalogo.modelo;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Patrón de movimiento (sentadilla, bisagra, empuje...). Tabla de referencia sembrada en V1. */
@Entity
@Table(name = "patron_movimiento")
public class PatronMovimiento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	private String codigo;

	private String nombre;

	protected PatronMovimiento() {
		// lo exige JPA
	}

	public Integer getId() {
		return id;
	}

	public String getCodigo() {
		return codigo;
	}

	public String getNombre() {
		return nombre;
	}

}
