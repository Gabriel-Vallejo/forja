package es.forja.entreno.rutina.solver;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.ortools.Loader;
import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.LinearExprBuilder;

import es.forja.entreno.rutina.modelo.ConfiguracionRutina;
import es.forja.entreno.rutina.modelo.ConfiguracionRutina.Rango;
import es.forja.entreno.rutina.modelo.EjercicioRutina;
import es.forja.entreno.rutina.modelo.EntradaRutina;
import es.forja.restricciones.modelo.Patron;

/**
 * Modelo CP-SAT de la semana (ADR 0002 y enmiendas). Todo en enteros: las series de cada músculo van en unidades
 * dobles (primario 2, secundario 1) para contar los secundarios a 0,5.
 *
 * <ul>
 * <li>Duras: duración por sesión, ejercicios por patrón y día, básico pesado sin repetir en días seguidos, máximo
 * de series por músculo y ejercicios por sesión.</li>
 * <li>Con holgura: mínimo de series y frecuencia mínima de los músculos con meta.</li>
 * </ul>
 */
final class ModeloRutina {

	static {
		// El nativo de la plataforma, una sola vez por JVM y antes de tocar cualquier clase de OR-Tools.
		Loader.loadNativeLibraries();
	}

	static final int REPETICIONES_SIN_PENALIZAR = 2;
	private static final int SEGUNDOS_POR_MINUTO = 60;
	private static final int DOBLE = 2;

	final CpModel cp = new CpModel();
	final List<EjercicioRutina> ejercicios;
	final int dias;
	final IntVar[][] series;
	final BoolVar[][] esta;
	final IntVar[] exceso;
	final Map<String, IntVar> holguraVolumen = new TreeMap<>();
	final Map<String, IntVar> holguraFrecuencia = new TreeMap<>();
	private final ConfiguracionRutina config;
	private final EntradaRutina entrada;

	/**
	 * @param ejerciciosMinimos mínimo de ejercicios por sesión que se exige en esta resolución
	 */
	ModeloRutina(ConfiguracionRutina config, EntradaRutina entrada, List<EjercicioRutina> ejercicios,
			int ejerciciosMinimos) {
		this.config = config;
		this.entrada = entrada;
		this.ejercicios = List.copyOf(ejercicios);
		this.dias = entrada.dias();
		this.series = new IntVar[ejercicios.size()][dias];
		this.esta = new BoolVar[ejercicios.size()][dias];
		this.exceso = new IntVar[ejercicios.size()];
		crearVariables();
		restriccionesPorDia(ejerciciosMinimos);
		basicosPesados();
		volumen();
		frecuencia();
		cp.maximize(objetivo(Map.of(), Map.of()));
	}

	/** Todos los músculos que tocan los candidatos más los que tienen meta, ordenados. */
	SortedSet<String> musculos() {
		SortedSet<String> musculos = new TreeSet<>(config.volumen().musculosConMinimo());
		ejercicios.forEach(e -> musculos.addAll(e.musculos().keySet()));
		return musculos;
	}

	/** Sugiere al solver una solución de partida (la del plan base), para que las re-resoluciones vayan rápido. */
	void sugerir(int[][] seriesBase) {
		for (int e = 0; e < ejercicios.size(); e++) {
			for (int d = 0; d < dias; d++) {
				cp.addHint(series[e][d], seriesBase[e][d]);
				cp.addHint(esta[e][d], seriesBase[e][d] > 0);
			}
		}
	}

	/** Prohíbe un ejercicio en toda la semana. */
	void prohibir(int indice) {
		for (int d = 0; d < dias; d++) {
			cp.addEquality(esta[indice][d], 0);
		}
	}

	/** Obliga a que un ejercicio salga al menos un día. */
	void forzar(int indice) {
		cp.addGreaterOrEqual(LinearExpr.sum(esta[indice]), 1);
	}

	private void crearVariables() {
		ConfiguracionRutina.Sesion s = config.sesion();
		for (int e = 0; e < ejercicios.size(); e++) {
			for (int d = 0; d < dias; d++) {
				series[e][d] = cp.newIntVar(0, s.seriesMaximasPorEjercicio(), "x_" + e + "_" + d);
				esta[e][d] = cp.newBoolVar("y_" + e + "_" + d);
				cp.addLessOrEqual(series[e][d], LinearExpr.term(esta[e][d], s.seriesMaximasPorEjercicio()));
				cp.addGreaterOrEqual(series[e][d], LinearExpr.term(esta[e][d], s.seriesMinimasPorEjercicio()));
			}
			exceso[e] = cp.newIntVar(0, dias, "exceso_" + e);
			cp.addGreaterOrEqual(LinearExpr.newBuilder().add(exceso[e]).add(REPETICIONES_SIN_PENALIZAR).build(),
					LinearExpr.sum(esta[e]));
		}
	}

	private void restriccionesPorDia(int ejerciciosMinimos) {
		ConfiguracionRutina.Tiempos t = config.tiempos();
		for (int d = 0; d < dias; d++) {
			LinearExprBuilder duracion = LinearExpr.newBuilder();
			LinearExprBuilder cuantos = LinearExpr.newBuilder();
			Map<Patron, LinearExprBuilder> porPatron = new TreeMap<>();
			for (int e = 0; e < ejercicios.size(); e++) {
				duracion.addTerm(series[e][d], t.segundosPorSerieConDescanso())
						.addTerm(esta[e][d], t.segundosDeTransicion());
				cuantos.add(esta[e][d]);
				porPatron.computeIfAbsent(ejercicios.get(e).patron(), p -> LinearExpr.newBuilder()).add(esta[e][d]);
			}
			cp.addLessOrEqual(duracion, (long) entrada.minutos() * SEGUNDOS_POR_MINUTO);
			cp.addGreaterOrEqual(cuantos, ejerciciosMinimos);
			cp.addLessOrEqual(cuantos, config.sesion().ejerciciosMaximos());
			porPatron.values().forEach(p -> cp.addLessOrEqual(p, config.sesion().maximoPorPatron()));
		}
	}

	private void basicosPesados() {
		for (int e = 0; e < ejercicios.size(); e++) {
			if (!ejercicios.get(e).basicoPesado()) {
				continue;
			}
			for (int d = 0; d + 1 < dias; d++) {
				cp.addLessOrEqual(LinearExpr.sum(new BoolVar[] { esta[e][d], esta[e][d + 1] }), 1);
			}
		}
	}

	/** Series dobles de la semana para un músculo. */
	LinearExprBuilder seriesDobles(String musculo) {
		LinearExprBuilder suma = LinearExpr.newBuilder();
		for (int e = 0; e < ejercicios.size(); e++) {
			int peso = ejercicios.get(e).pesoDoble(musculo);
			if (peso > 0) {
				for (int d = 0; d < dias; d++) {
					suma.addTerm(series[e][d], peso);
				}
			}
		}
		return suma;
	}

	private void volumen() {
		List<String> conMinimo = config.volumen().musculosConMinimo();
		for (String m : musculos()) {
			Rango rango = config.volumen().rango(m);
			cp.addLessOrEqual(seriesDobles(m), (long) DOBLE * rango.maximo());
			if (conMinimo.contains(m)) {
				IntVar holgura = cp.newIntVar(0, (long) DOBLE * rango.minimo(), "hv_" + m);
				holguraVolumen.put(m, holgura);
				cp.addGreaterOrEqual(seriesDobles(m).add(holgura), (long) DOBLE * rango.minimo());
			}
		}
	}

	private void frecuencia() {
		int minima = entrada.frecuenciaMinima();
		for (String m : config.volumen().musculosConMinimo()) {
			LinearExprBuilder diasEntrenado = LinearExpr.newBuilder();
			for (int d = 0; d < dias; d++) {
				BoolVar entrenado = cp.newBoolVar("f_" + m + "_" + d);
				LinearExprBuilder primarios = LinearExpr.newBuilder();
				for (int e = 0; e < ejercicios.size(); e++) {
					if (ejercicios.get(e).pesoDoble(m) == EjercicioRutina.PESO_PRIMARIO) {
						primarios.add(esta[e][d]);
					}
				}
				cp.addLessOrEqual(entrenado, primarios);
				diasEntrenado.add(entrenado);
			}
			IntVar holgura = cp.newIntVar(0, minima, "hf_" + m);
			holguraFrecuencia.put(m, holgura);
			cp.addGreaterOrEqual(diasEntrenado.add(holgura), minima);
		}
	}

	/**
	 * Objetivo a maximizar. {@code movilidad} y {@code palancas} son la puntuación por {@code idExterno}: aún no
	 * hay datos, así que llegan vacías y con peso 0, sin inventar nada.
	 */
	private LinearExprBuilder objetivo(Map<String, Integer> movilidad, Map<String, Integer> palancas) {
		ConfiguracionRutina.Pesos p = config.pesos();
		LinearExprBuilder objetivo = LinearExpr.newBuilder();
		for (int e = 0; e < ejercicios.size(); e++) {
			long porSerie = Terminos.porSerie(p, ejercicios.get(e), entrada.prioritarios(), movilidad, palancas);
			for (int d = 0; d < dias; d++) {
				objetivo.addTerm(series[e][d], porSerie);
			}
			objetivo.addTerm(exceso[e], -p.variedad());
		}
		holguraVolumen.values().forEach(h -> objetivo.addTerm(h, -p.holguraVolumen()));
		holguraFrecuencia.values().forEach(h -> objetivo.addTerm(h, -p.holguraFrecuencia()));
		return objetivo;
	}
}
