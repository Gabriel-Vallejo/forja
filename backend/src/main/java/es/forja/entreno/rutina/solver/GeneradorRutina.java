package es.forja.entreno.rutina.solver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.IntStream;

import es.forja.entreno.rutina.modelo.CatalogoRutina;
import es.forja.entreno.rutina.modelo.ConfiguracionRutina;
import es.forja.entreno.rutina.modelo.EjercicioRutina;
import es.forja.entreno.rutina.modelo.EntradaRutina;
import es.forja.entreno.rutina.modelo.PlanRutina;
import es.forja.entreno.rutina.modelo.PlanRutina.Alternativa;
import es.forja.entreno.rutina.modelo.PlanRutina.Holgura;
import es.forja.entreno.rutina.solver.Resolutor.Resultado;
import es.forja.entreno.rutina.solver.Resolutor.Solucion;
import es.forja.restricciones.modelo.Patron;

/**
 * Genera la rutina semanal con CP-SAT (ADR 0002): filtro previo, modelo, solución, traza y «por qué este y no
 * aquel». Sin Spring y sin estado: la misma entrada da siempre el mismo plan.
 */
public class GeneradorRutina {

	/** Tope de re-resoluciones del «por qué este y no aquel» en una misma petición: cada una cuesta CPU. */
	public static final int MAXIMO_RESOLUCIONES_ALTERNATIVAS = 6;
	static final int ALTERNATIVAS_POR_EJERCICIO = 2;

	private final ConfiguracionRutina config;

	public GeneradorRutina(ConfiguracionRutina config) {
		this.config = Objects.requireNonNull(config, "config es obligatoria");
	}

	public PlanRutina generar(CatalogoRutina catalogo, EntradaRutina solicitada) {
		EntradaRutina entrada = solicitada;
		FiltroPrevio.Resultado filtro = FiltroPrevio.aplicar(catalogo.ejercicios(), entrada);
		List<String> notas = new ArrayList<>();
		boolean minimos = true;
		Map<String, Patron> patrones = new TreeMap<>();
		catalogo.ejercicios().forEach(e -> patrones.put(e.idExterno(), e.patron()));
		ModeloRutina modelo = new ModeloRutina(config, entrada, filtro.candidatos(), minimos, patrones);
		Resultado resultado = Resolutor.resolver(modelo, config.solver(), config.solver().tiempoDeterminista());
		if (resultado.inviable() && !entrada.seriesSemana().isEmpty()) {
			// Las series fijadas son duras: si no caben, mejor un plan sin ellas (y decirlo) que ninguno.
			notas.add(Textos.SERIES_NO_CABEN);
			entrada = entrada.sinSeriesFijadas();
			modelo = new ModeloRutina(config, entrada, filtro.candidatos(), minimos, patrones);
			resultado = Resolutor.resolver(modelo, config.solver(), config.solver().tiempoDeterminista());
		}
		if (resultado.inviable()) {
			// Con pocos candidatos los mínimos por sesión pueden no caber; sin ellos, el modelo siempre tiene
			// solución (todo a cero) y la holgura cuenta lo que falta.
			notas.add(Textos.minimoRebajado(config.sesion().ejerciciosMinimos(),
					config.sesion().porcentajeMinimoDelTiempo()));
			minimos = false;
			modelo = new ModeloRutina(config, entrada, filtro.candidatos(), minimos, patrones);
			resultado = Resolutor.resolver(modelo, config.solver(), config.solver().tiempoDeterminista());
		}
		Solucion solucion = resultado.solucion()
				.orElseThrow(() -> new IllegalStateException("El solver no encontró ningún plan en el límite fijado"));
		Traza traza = new Traza(config, entrada, modelo, solucion, catalogo.nombresMusculo());
		List<Holgura> holguras = new ArrayList<>(traza.holguras());
		holguras.addAll(TrazaBasicos.holguras(config.basicos().patrones(), entrada, catalogo.ejercicios(), filtro,
				traza.elegidos().stream().map(filtro.candidatos()::get).toList()));
		String resumen = PlanRutina.OPTIMO.equals(solucion.estado()) ? Textos.RESUMEN_OPTIMO
				: Textos.RESUMEN_FACTIBLE;
		return new PlanRutina(solucion.estado(), resumen, solucion.objetivo(),
				traza.dias(conMotivosDeMovilidad(filtro.anotaciones(), filtro.candidatos(), entrada)),
				traza.volumen(), holguras, traza.contribuciones(),
				alternativas(entrada, filtro.candidatos(), minimos, traza, solucion), filtro.descartes(), notas);
	}

	/** Las anotaciones del filtro más, en los ejercicios que favorece el tramo de movilidad, el porqué. */
	private static Map<String, List<String>> conMotivosDeMovilidad(Map<String, List<String>> anotaciones,
			List<EjercicioRutina> candidatos, EntradaRutina entrada) {
		Map<String, List<String>> todas = new TreeMap<>(anotaciones);
		for (EjercicioRutina e : candidatos) {
			List<String> motivos = ReglasMovilidad.motivos(e.idExterno(), entrada.movilidad());
			if (!motivos.isEmpty()) {
				List<String> lista = new ArrayList<>(todas.getOrDefault(e.idExterno(), List.of()));
				lista.addAll(motivos);
				todas.put(e.idExterno(), lista);
			}
		}
		return todas;
	}

	/**
	 * Para cada elegido, hasta {@value #ALTERNATIVAS_POR_EJERCICIO} candidatos de su patrón que no están en el plan
	 * (familia primero): se prohíbe el elegido, se fuerza la alternativa y se vuelve a resolver, con el límite de las
	 * alternativas y el plan base como pista.
	 */
	private List<Alternativa> alternativas(EntradaRutina entrada, List<EjercicioRutina> candidatos, boolean minimos,
			Traza traza, Solucion base) {
		List<Integer> elegidos = traza.elegidos();
		List<Alternativa> alternativas = new ArrayList<>();
		for (int e : elegidos) {
			for (int a : cercanas(candidatos, elegidos, e)) {
				if (alternativas.size() == MAXIMO_RESOLUCIONES_ALTERNATIVAS) {
					return alternativas;
				}
				alternativas.add(comparar(entrada, candidatos, minimos, base, e, a));
			}
		}
		return alternativas;
	}

	private static List<Integer> cercanas(List<EjercicioRutina> candidatos, List<Integer> elegidos, int e) {
		EjercicioRutina elegido = candidatos.get(e);
		Comparator<Integer> familiaPrimero = Comparator.comparing(
				i -> !Objects.equals(elegido.candidato().familiaSustitucion(),
						candidatos.get(i).candidato().familiaSustitucion()));
		return IntStream.range(0, candidatos.size()).boxed()
				.filter(i -> !elegidos.contains(i) && candidatos.get(i).patron() == elegido.patron())
				.sorted(familiaPrimero.thenComparing(i -> candidatos.get(i).idExterno()))
				.limit(ALTERNATIVAS_POR_EJERCICIO).toList();
	}

	private Alternativa comparar(EntradaRutina entrada, List<EjercicioRutina> candidatos, boolean minimos,
			Solucion base, int elegido, int alternativa) {
		// Sin la partida: la diferencia es la del plan, no el coste de alejarse del actual.
		ModeloRutina modelo = new ModeloRutina(config, entrada.sinPartida(), candidatos, minimos);
		modelo.sugerir(base.series());
		modelo.prohibir(elegido);
		modelo.forzar(alternativa);
		Resultado resultado = Resolutor.resolver(modelo, config.solver(),
				config.solver().tiempoDeterministaAlternativas());
		Long diferencia = resultado.solucion().map(s -> base.objetivo() - s.objetivo()).orElse(null);
		EjercicioRutina alt = candidatos.get(alternativa);
		return new Alternativa(candidatos.get(elegido).idExterno(), alt.idExterno(), alt.nombre(), diferencia,
				Textos.alternativa(alt.nombre(), diferencia, resultado.inviable(), resultado.solucion()
						.map(sol -> PlanRutina.OPTIMO.equals(sol.estado())).orElse(false)));
	}
}
