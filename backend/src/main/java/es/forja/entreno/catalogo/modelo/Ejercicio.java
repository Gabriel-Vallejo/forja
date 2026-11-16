package es.forja.entreno.catalogo.modelo;

import java.util.Set;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Ejercicio del catálogo con sus etiquetas biomecánicas (ADR 0005 y 0007).
 *
 * <p>Solo lectura desde la aplicación: el catálogo lo escribe la migración repetible
 * {@code R__catalogo_semilla.sql}. Los valores enumerados (mecánica, nivel, agarre...) se guardan
 * como texto y los restringe el ENUM de MySQL.
 */
@Entity
@Table(name = "ejercicio")
public class Ejercicio {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String idExterno;
	private String nombreOriginal;
	private String nombreEs;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "patron_id")
	private PatronMovimiento patron;

	private String mecanica;
	private String nivel;
	private String categoria;
	private boolean cargaAxial;
	private boolean impacto;
	private String agarre;
	private String lateralidad;
	private boolean porEncimaCabeza;
	private boolean posicionEstirada;
	private String familiaSustitucion;
	private String rangoMovimiento;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "implemento_id")
	private Material implemento;

	/** Regiones que carga el ejercicio (tabla {@code ejercicio_region}); solo lectura, la escribe la semilla. */
	@Immutable
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "ejercicio_region",
			joinColumns = @JoinColumn(name = "ejercicio_id"),
			inverseJoinColumns = @JoinColumn(name = "region_id"))
	private Set<RegionCorporal> regiones;

	private boolean aptoFuerza;
	private String origenEtiquetado;
	private boolean revisado;
	private String fuente;
	private String licencia;
	private String autor;
	private String versionFuente;

	protected Ejercicio() {
		// lo exige JPA
	}

	public Long getId() {
		return id;
	}

	public String getIdExterno() {
		return idExterno;
	}

	public String getNombreOriginal() {
		return nombreOriginal;
	}

	public String getNombreEs() {
		return nombreEs;
	}

	public PatronMovimiento getPatron() {
		return patron;
	}

	public String getMecanica() {
		return mecanica;
	}

	public String getNivel() {
		return nivel;
	}

	public String getCategoria() {
		return categoria;
	}

	public boolean isCargaAxial() {
		return cargaAxial;
	}

	public boolean isImpacto() {
		return impacto;
	}

	public String getAgarre() {
		return agarre;
	}

	public String getLateralidad() {
		return lateralidad;
	}

	public boolean isPorEncimaCabeza() {
		return porEncimaCabeza;
	}

	public boolean isPosicionEstirada() {
		return posicionEstirada;
	}

	public String getFamiliaSustitucion() {
		return familiaSustitucion;
	}

	public String getRangoMovimiento() {
		return rangoMovimiento;
	}

	public Material getImplemento() {
		return implemento;
	}

	public Set<RegionCorporal> getRegiones() {
		return regiones;
	}

	public boolean isAptoFuerza() {
		return aptoFuerza;
	}

	public String getOrigenEtiquetado() {
		return origenEtiquetado;
	}

	public boolean isRevisado() {
		return revisado;
	}

	public String getFuente() {
		return fuente;
	}

	public String getLicencia() {
		return licencia;
	}

	public String getAutor() {
		return autor;
	}

	public String getVersionFuente() {
		return versionFuente;
	}

}
