package es.forja.entreno.rutina.solver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import es.forja.entreno.rutina.modelo.CatalogoRutina;
import es.forja.entreno.rutina.modelo.ConfiguracionRutina;
import es.forja.entreno.rutina.modelo.EjercicioRutina;
import es.forja.entreno.rutina.modelo.EntradaRutina;
import es.forja.entreno.rutina.modelo.PlanRutina;
import es.forja.entreno.rutina.modelo.PlanRutina.Alternativa;
import es.forja.entreno.rutina.solver.Resolutor.Resultado;
import es.forja.entreno.rutina.solver.Resolutor.Solucion;

/**
 * Genera la rutina semanal con CP-SAT (ADR 0002): filtro previo, modelo, solución, traza y «por qué este y no
 * aquel». Sin Spring y sin estado: la misma entrada da siempre el mismo plan.
 */
public class GeneradorRutina {

	/** Tope de re-resoluciones del «por qué este y no aquel» en una misma petición: cada una cuesta CPU. */
	public static final int MAXIMO_RESOLUCIONES_ALTERNATIVAS = 10;
	static final int ALTERNATIVAS_POR_EJERCICIO = 2;

	private final ConfiguracionRutina config;

	public GeneradorRutina(ConfiguracionRutina config) {
		this.config = Objects.requireNonNull(config, "config es obligatoria");
	}

	public PlanRutina generar(CatalogoRutina catalogo, EntradaRutina entrada) {
		FiltroPrevio.Resultado filtro = FiltroPrevio.aplicar(catalogo.ejercicios(), entrada);
		List<String> notas = new ArrayList<>();
		int minimo = config.sesion().ejerciciosMinimos();
		ModeloRutina modelo = new ModeloRutina(config, entrada, filtro.candidatos(), minimo);
		Resultado resultado = Resolutor.resolver(modelo, config.solver());
		if (resultado.inviable()) {
			// Con pocos candidatos el mínimo de ejercicios por sesión puede no caber; sin él, el modelo siempre
			// tiene solución (todo a cero) y la holgura cuenta lo que falta.
			notas.add(Textos.minimoRebajado(minimo));
			minimo = 0;
			modelo = new ModeloRutina(config, entrada, filtro.candidatos(), minimo);
			resultado = Resolutor.resolver(modelo, config.solver());
		}
		Solucion solucion = resultado.solucion()
				.orElseThrow(() -> new IllegalStateException("El solver no encontró ningún plan en el límite fijado"));
		Traza traza = new Traza(config, entrada, modelo, solucion, catalogo.nombresMusculo());
		String resumen = PlanRutina.OPTIMO.equals(solucion.estado()) ? Textos.RESUMEN_OPTIMO
				: Textos.RESUMEN_FACTIBLE;
		return new PlanRutina(solucion.estado(), resumen, solucion.objetivo(), traza.dias(filtro.anotaciones()),
				traza.volumen(), traza.holguras(), traza.contribuciones(),
				alternativas(entrada, filtro.candidatos(), minimo, traza, solucion), filtro.descartes(), notas);
	}

	/**
	 * Para cada elegido, hasta {@value #ALTERNATIVAS_POR_EJERCICIO} candidatos de su patrón que no están en el plan
	 * (familia primero): se prohíbe el elegido, se fuerza la alternativa y se vuelve a resolver con el mismo límite.
	 */
	private List<Alternativa> alternativas(EntradaRutina entrada, List<EjercicioRutina> candidatos, int minimo,
			Traza traza, Solucion base) {
		List<Integer> elegidos = traza.elegidos();
		List<Alternativa> alternativas = new ArrayList<>();
		for (int e : elegidos) {
			for (int a : cercanas(candidatos, elegidos, e)) {
				if (alternativas.size() == MAXIMO_RESOLUCIONES_ALTERNATIVAS) {
					return alternativas;
				}
				alternativas.add(comparar(entrada, candidatos, minimo, base, e, a));
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

	private Alternativa comparar(EntradaRutina entrada, List<EjercicioRutina> candidatos, int minimo, Solucion base,
			int elegido, int alternativa) {
		ModeloRutina modelo = new ModeloRutina(config, entrada, candidatos, minimo);
		modelo.sugerir(base.series());
		modelo.prohibir(elegido);
		modelo.forzar(alternativa);
		Resultado resultado = Resolutor.resolver(modelo, config.solver());
		Long diferencia = resultado.solucion().map(s -> base.objetivo() - s.objetivo()).orElse(null);
		EjercicioRutina alt = candidatos.get(alternativa);
		return new Alternativa(candidatos.get(elegido).idExterno(), alt.idExterno(), alt.nombre(), diferencia,
				Textos.alternativa(alt.nombre(), diferencia, resultado.inviable()));
	}
}
