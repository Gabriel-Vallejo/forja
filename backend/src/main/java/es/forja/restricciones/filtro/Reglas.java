package es.forja.restricciones.filtro;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import es.forja.restricciones.modelo.Origen;
import es.forja.restricciones.modelo.Restriccion;
import es.forja.restricciones.modelo.TipoRestriccion;

/** Cuándo una restricción aplica a un ejercicio y cuándo lo incumple. Funciones puras. */
final class Reglas {

	private Reglas() {
	}

	/** La restricción nombra al ejercicio: su patrón, una región que carga, o las dos si dice ambas. */
	static boolean aplica(Restriccion r, EjercicioCandidato e) {
		boolean porPatron = r.patron() == null || r.patron() == e.patron();
		boolean porRegion = r.region() == null || e.regiones().contains(r.region());
		return porPatron && porRegion;
	}

	private static boolean incumple(Restriccion r, EjercicioCandidato e) {
		if (!aplica(r, e)) {
			return false;
		}
		return switch (r.tipo()) {
			case EVITAR -> true;
			case LIMITAR_RANGO -> e.rango() == RangoMovimiento.COMPLETO;
			case SIN_CARGA_AXIAL -> e.cargaAxial();
			case SIN_IMPACTO -> e.impacto();
			case SIN_AGARRE_PESADO -> e.agarre() == Agarre.PESADO;
			case SIN_POR_ENCIMA_CABEZA -> e.porEncimaCabeza();
			// No tienen forma de incumplirse: solo anotan para el solver.
			case LIMITAR_CARGA, TEMPO_CONTROLADO -> false;
		};
	}

	/** Dura: excluye y no se sustituye. Evitar y todo lo que viene de un profesional. */
	private static boolean esDura(Restriccion r) {
		return r.tipo() == TipoRestriccion.EVITAR || r.origen() == Origen.PROFESIONAL;
	}

	/** Restricciones activas que el ejercicio incumple, en el orden en que llegaron. */
	static List<Violacion> violaciones(EjercicioCandidato e, List<Restriccion> activas) {
		List<Violacion> violaciones = new ArrayList<>();
		for (Restriccion r : activas) {
			if (incumple(r, e)) {
				violaciones.add(new Violacion(r, esDura(r)));
			}
		}
		return violaciones;
	}

	/** La violación que manda: la primera dura y, si no hay, la primera blanda. */
	static Optional<Violacion> principal(List<Violacion> violaciones) {
		return violaciones.stream().filter(Violacion::dura).findFirst()
				.or(() -> violaciones.stream().findFirst());
	}

	static boolean cumple(EjercicioCandidato e, List<Restriccion> activas) {
		return violaciones(e, activas).isEmpty();
	}

	/** Avisos para el solver: límite de carga y tempo controlado que aplican al ejercicio. */
	static List<String> anotaciones(EjercicioCandidato e, List<Restriccion> activas) {
		return activas.stream()
				.filter(r -> r.tipo() == TipoRestriccion.LIMITAR_CARGA || r.tipo() == TipoRestriccion.TEMPO_CONTROLADO)
				.filter(r -> aplica(r, e))
				.map(r -> Motivos.describir(r) + (r.parametro() == null ? "" : ": " + r.parametro()))
				.toList();
	}

}
