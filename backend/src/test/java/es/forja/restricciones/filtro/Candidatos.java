package es.forja.restricciones.filtro;

import java.util.Set;

import es.forja.restricciones.modelo.Estado;
import es.forja.restricciones.modelo.Lado;
import es.forja.restricciones.modelo.Origen;
import es.forja.restricciones.modelo.Patron;
import es.forja.restricciones.modelo.Region;
import es.forja.restricciones.modelo.Restriccion;
import es.forja.restricciones.modelo.TipoRestriccion;

/** Ayudas para armar catálogos y restricciones de prueba, con datos sintéticos. */
final class Candidatos {

	private Candidatos() {
	}

	/** Candidato curado, sin carga axial, impacto, agarre ni trabajo por encima de la cabeza, de rango completo. */
	static EjercicioCandidato base(String id, Patron patron, String familia, String implemento) {
		return new EjercicioCandidato(id, "Ejercicio " + id, patron, Set.of(), false, false, Agarre.NINGUNO,
				false, familia, RangoMovimiento.COMPLETO, implemento, true);
	}

	static EjercicioCandidato conRegiones(EjercicioCandidato e, Region... regiones) {
		return new EjercicioCandidato(e.idExterno(), e.nombre(), e.patron(), Set.of(regiones), e.cargaAxial(),
				e.impacto(), e.agarre(), e.porEncimaCabeza(), e.familiaSustitucion(), e.rango(), e.implemento(),
				e.curado());
	}

	static EjercicioCandidato axial(EjercicioCandidato e) {
		return new EjercicioCandidato(e.idExterno(), e.nombre(), e.patron(), e.regiones(), true, e.impacto(),
				e.agarre(), e.porEncimaCabeza(), e.familiaSustitucion(), e.rango(), e.implemento(), e.curado());
	}

	static EjercicioCandidato reducido(EjercicioCandidato e) {
		return new EjercicioCandidato(e.idExterno(), e.nombre(), e.patron(), e.regiones(), e.cargaAxial(),
				e.impacto(), e.agarre(), e.porEncimaCabeza(), e.familiaSustitucion(), RangoMovimiento.REDUCIDO,
				e.implemento(), e.curado());
	}

	static EjercicioCandidato noCurado(EjercicioCandidato e) {
		return new EjercicioCandidato(e.idExterno(), e.nombre(), e.patron(), e.regiones(), e.cargaAxial(),
				e.impacto(), e.agarre(), e.porEncimaCabeza(), e.familiaSustitucion(), e.rango(), e.implemento(),
				false);
	}

	static Restriccion sobrePatron(Patron patron, TipoRestriccion tipo) {
		return new Restriccion(null, Lado.AMBOS, patron, tipo, null, Estado.ACTUAL, Origen.USUARIO, null);
	}

	static Restriccion sobreRegion(Region region, TipoRestriccion tipo) {
		return new Restriccion(region, Lado.AMBOS, null, tipo, null, Estado.ACTUAL, Origen.USUARIO, null);
	}

	static Restriccion deProfesional(Restriccion r) {
		return new Restriccion(r.region(), r.lado(), r.patron(), r.tipo(), r.parametro(), r.estado(),
				Origen.PROFESIONAL, r.textoOriginal());
	}

	static Restriccion conEstado(Restriccion r, Estado estado) {
		return new Restriccion(r.region(), r.lado(), r.patron(), r.tipo(), r.parametro(), estado, r.origen(),
				r.textoOriginal());
	}

	static Restriccion conParametro(Restriccion r, String parametro) {
		return new Restriccion(r.region(), r.lado(), r.patron(), r.tipo(), parametro, r.estado(), r.origen(),
				r.textoOriginal());
	}

}
