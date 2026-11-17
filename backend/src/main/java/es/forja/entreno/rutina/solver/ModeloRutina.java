package es.forja.entreno.rutina.solver;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * <li>Duras: duración por sesión, ejercicios y series por patrón y día, básico pesado sin repetir en días
 * seguidos, máximo de series por músculo, ejercicios por sesión y sesión mínima (porcentaje del tiempo).</li>
 * <li>Con holgura: mínimo de series y frecuencia mínima de los músculos con meta, y cada patrón principal al
 * menos una vez con un compuesto.</li>
 * <li>Volumen objetivo por músculo, lineal a trozos: premio por serie hasta el objetivo y penalización suave por
 * encima. Diferencia entre la sesión más larga y la más corta, penalizada por minuto.</li>
 * </ul>
 */
final class ModeloRutina {

	static {
		// El nativo de la plataforma, una sola vez por JVM y antes de tocar cualquier clase de OR-Tools.
		Loader.loadNativeLibraries();
	}

	static final int REPETICIONES_SIN_PENALIZAR = 2;
	/** Sin tope de series por día: el aislamiento mezcla músculos (bíceps y tríceps) y el resto apenas carga. */
	static final Set<Patron> SIN_TOPE_DE_SERIES_POR_DIA = EnumSet.of(Patron.AISLAMIENTO_BRAZO,
			Patron.AISLAMIENTO_PIERNA, Patron.CORE, Patron.ACARREO, Patron.PLIOMETRIA, Patron.MOVILIDAD, Patron.CARDIO);
	private static final int SEGUNDOS_POR_MINUTO = 60;
	private static final int DOBLE = 2;
	private static final int CIEN = 100;

	final CpModel cp = new CpModel();
	final List<EjercicioRutina> ejercicios;
	final int dias;
	final IntVar[][] series;
	final BoolVar[][] esta;
	final IntVar[] exceso;
	final Map<String, IntVar> holguraVolumen = new TreeMap<>();
	final Map<String, IntVar> holguraFrecuencia = new TreeMap<>();
	final Map<String, BoolVar> faltaBasico = new TreeMap<>();
	private final Map<String, IntVar> hastaObjetivo = new TreeMap<>();
	private final Map<String, IntVar> sobreObjetivo = new TreeMap<>();
	private final LinearExprBuilder[] duracion;
	private IntVar desequilibrio;
	private final ConfiguracionRutina config;
	private final EntradaRutina entrada;

	/**
	 * @param exigirMinimosPorSesion si se exigen el mínimo de ejercicios y de tiempo por sesión; sin ellos el
	 *            modelo siempre tiene solución (todo a cero)
	 */
	ModeloRutina(ConfiguracionRutina config, EntradaRutina entrada, List<EjercicioRutina> ejercicios,
			boolean exigirMinimosPorSesion) {
		this.config = config;
		this.entrada = entrada;
		this.ejercicios = List.copyOf(ejercicios);
		this.dias = entrada.dias();
		this.series = new IntVar[ejercicios.size()][dias];
		this.esta = new BoolVar[ejercicios.size()][dias];
		this.exceso = new IntVar[ejercicios.size()];
		this.duracion = new LinearExprBuilder[dias];
		crearVariables();
		restriccionesPorDia(exigirMinimosPorSesion);
		equilibrio(exigirMinimosPorSesion);
		basicosPesados();
		volumen();
		frecuencia();
		basicos();
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

	private void restriccionesPorDia(boolean exigirMinimos) {
		ConfiguracionRutina.Tiempos t = config.tiempos();
		int ejerciciosMinimos = exigirMinimos ? config.sesion().ejerciciosMinimos() : 0;
		for (int d = 0; d < dias; d++) {
			LinearExprBuilder duracion = LinearExpr.newBuilder();
			this.duracion[d] = duracion;
			LinearExprBuilder cuantos = LinearExpr.newBuilder();
			Map<Patron, LinearExprBuilder> porPatron = new TreeMap<>();
			Map<Patron, LinearExprBuilder> seriesPorPatron = new TreeMap<>();
			for (int e = 0; e < ejercicios.size(); e++) {
				Patron patron = ejercicios.get(e).patron();
				duracion.addTerm(series[e][d], t.segundosPorSerieConDescanso())
						.addTerm(esta[e][d], t.segundosDeTransicion());
				cuantos.add(esta[e][d]);
				porPatron.computeIfAbsent(patron, p -> LinearExpr.newBuilder()).add(esta[e][d]);
				if (!SIN_TOPE_DE_SERIES_POR_DIA.contains(patron)) {
					seriesPorPatron.computeIfAbsent(patron, p -> LinearExpr.newBuilder()).add(series[e][d]);
				}
			}
			cp.addLessOrEqual(duracion, (long) entrada.minutos() * SEGUNDOS_POR_MINUTO);
			cp.addGreaterOrEqual(cuantos, ejerciciosMinimos);
			cp.addLessOrEqual(cuantos, config.sesion().ejerciciosMaximos());
			porPatron.values().forEach(p -> cp.addLessOrEqual(p, config.sesion().maximoPorPatron()));
			seriesPorPatron.values().forEach(p -> cp.addLessOrEqual(p, config.sesion().seriesMaximasPorPatronYDia()));
		}
	}

	/** Sesión mínima dura y diferencia entre la más larga y la más corta, en minutos, para penalizarla. */
	private void equilibrio(boolean exigirMinimos) {
		long disponible = (long) entrada.minutos() * SEGUNDOS_POR_MINUTO;
		long minima = exigirMinimos ? disponible * config.sesion().porcentajeMinimoDelTiempo() / CIEN : 0;
		IntVar larga = cp.newIntVar(0, disponible, "sesion_larga");
		IntVar corta = cp.newIntVar(0, disponible, "sesion_corta");
		for (LinearExprBuilder d : duracion) {
			cp.addGreaterOrEqual(d, minima);
			cp.addGreaterOrEqual(larga, d);
			cp.addLessOrEqual(corta, d);
		}
		desequilibrio = cp.newIntVar(0, entrada.minutos(), "desequilibrio");
		cp.addGreaterOrEqual(LinearExpr.term(desequilibrio, SEGUNDOS_POR_MINUTO),
				LinearExpr.newBuilder().add(larga).addTerm(corta, -1));
	}

	/** Cada patrón principal, al menos un día con un compuesto; si no puede, {@code faltaBasico} lo absorbe. */
	private void basicos() {
		for (String codigo : config.basicos().patrones()) {
			Patron patron = Patron.desdeCodigo(codigo);
			LinearExprBuilder presencias = LinearExpr.newBuilder();
			for (int e = 0; e < ejercicios.size(); e++) {
				if (ejercicios.get(e).compuesto() && ejercicios.get(e).patron() == patron) {
					presencias.addSum(esta[e]);
				}
			}
			BoolVar falta = cp.newBoolVar("falta_" + codigo);
			faltaBasico.put(codigo, falta);
			cp.addGreaterOrEqual(presencias.add(falta), 1);
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
			objetivoDeVolumen(m, rango);
			if (conMinimo.contains(m)) {
				IntVar holgura = cp.newIntVar(0, (long) DOBLE * rango.minimo(), "hv_" + m);
				holguraVolumen.put(m, holgura);
				cp.addGreaterOrEqual(seriesDobles(m).add(holgura), (long) DOBLE * rango.minimo());
			}
		}
	}

	/** {@code hasta} = min(series, objetivo) y {@code sobre} = max(0, series − objetivo), en series dobles. */
	private void objetivoDeVolumen(String musculo, Rango rango) {
		long objetivo = (long) DOBLE * config.volumen().objetivoDe(musculo);
		IntVar hasta = cp.newIntVar(0, objetivo, "hasta_" + musculo);
		IntVar sobre = cp.newIntVar(0, (long) DOBLE * rango.maximo(), "sobre_" + musculo);
		cp.addLessOrEqual(hasta, seriesDobles(musculo));
		cp.addGreaterOrEqual(LinearExpr.newBuilder().add(sobre).add(objetivo), seriesDobles(musculo));
		hastaObjetivo.put(musculo, hasta);
		sobreObjetivo.put(musculo, sobre);
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
			long porSerie = Terminos.porSerie(config, ejercicios.get(e), entrada.prioritarios(), movilidad, palancas);
			for (int d = 0; d < dias; d++) {
				objetivo.addTerm(series[e][d], porSerie);
			}
			objetivo.addTerm(exceso[e], -p.variedad());
		}
		hastaObjetivo.values().forEach(h -> objetivo.addTerm(h, p.serieHastaObjetivo()));
		sobreObjetivo.values().forEach(s -> objetivo.addTerm(s, -p.serieSobreObjetivo()));
		holguraVolumen.values().forEach(h -> objetivo.addTerm(h, -p.holguraVolumen()));
		holguraFrecuencia.values().forEach(h -> objetivo.addTerm(h, -p.holguraFrecuencia()));
		faltaBasico.values().forEach(f -> objetivo.addTerm(f, -p.holguraBasicos()));
		objetivo.addTerm(desequilibrio, -p.desequilibrioSesiones());
		return objetivo;
	}
}
