package es.forja.entreno.rutina.solver;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.SatParameters.SearchBranching;

import es.forja.entreno.rutina.modelo.ConfiguracionRutina;
import es.forja.entreno.rutina.modelo.PlanRutina;

/**
 * Resuelve un {@link ModeloRutina} de forma reproducible (ADR 0002, enmienda): límite de tiempo determinista y no
 * de reloj, número fijo de workers, semilla fija y sin búsqueda aleatoria. Con más de un worker los subsolvers van
 * intercalados, que es lo que hace que den siempre la misma respuesta; con uno, la búsqueda ya es determinista y
 * sin intercalar va unas cuatro veces más rápido en este modelo.
 *
 * <p>La linealización completa y la cartera con reinicios rápidos son las que mejor van en este modelo: con 1 worker
 * cierran el óptimo en menos de un segundo en casi todas las combinaciones de días y minutos del catálogo real.
 */
final class Resolutor {

	private static final int LINEALIZACION_COMPLETA = 2;

	/** Respuesta del solver; vacía si el modelo es inviable o no hubo solución dentro del límite. */
	record Solucion(String estado, long objetivo, int[][] series, Map<String, Long> holguraVolumen,
			Map<String, Long> holguraFrecuencia) {
	}

	/** Lo que pasó al resolver: la solución, o si el modelo es inviable. */
	record Resultado(Optional<Solucion> solucion, boolean inviable) {
	}

	private Resolutor() {
	}

	static Resultado resolver(ModeloRutina modelo, ConfiguracionRutina.Solver parametros) {
		CpSolver solver = new CpSolver();
		solver.getParameters().setMaxDeterministicTime(parametros.tiempoDeterminista())
				.setNumWorkers(parametros.workers()).setRandomSeed(parametros.semilla()).setRandomizeSearch(false)
				.setInterleaveSearch(parametros.workers() > 1).setLinearizationLevel(LINEALIZACION_COMPLETA)
				.setSearchBranching(SearchBranching.PORTFOLIO_WITH_QUICK_RESTART_SEARCH);
		CpSolverStatus estado = solver.solve(modelo.cp);
		if (estado != CpSolverStatus.OPTIMAL && estado != CpSolverStatus.FEASIBLE) {
			return new Resultado(Optional.empty(), estado == CpSolverStatus.INFEASIBLE);
		}
		return new Resultado(Optional.of(leer(solver, modelo, estado)), false);
	}

	private static Solucion leer(CpSolver solver, ModeloRutina modelo, CpSolverStatus estado) {
		int[][] series = new int[modelo.ejercicios.size()][modelo.dias];
		for (int e = 0; e < series.length; e++) {
			for (int d = 0; d < modelo.dias; d++) {
				series[e][d] = (int) solver.value(modelo.series[e][d]);
			}
		}
		Map<String, Long> volumen = new TreeMap<>();
		modelo.holguraVolumen.forEach((m, v) -> volumen.put(m, solver.value(v)));
		Map<String, Long> frecuencia = new TreeMap<>();
		modelo.holguraFrecuencia.forEach((m, v) -> frecuencia.put(m, solver.value(v)));
		String nombre = estado == CpSolverStatus.OPTIMAL ? PlanRutina.OPTIMO : PlanRutina.FACTIBLE;
		return new Solucion(nombre, Math.round(solver.objectiveValue()), series, volumen, frecuencia);
	}
}
