package es.forja.entreno.catalogo.modelo;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Material con el que se hace un ejercicio (barra, mancuerna, máquina...). Tabla de referencia sembrada en V1. */
@Entity
@Table(name = "material")
public class Material {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	private String codigo;

	private String nombre;

	protected Material() {
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
