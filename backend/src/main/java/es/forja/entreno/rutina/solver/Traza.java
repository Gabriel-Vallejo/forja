package es.forja.entreno.rutina.solver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import es.forja.entreno.rutina.modelo.ConfiguracionRutina;
import es.forja.entreno.rutina.modelo.ConfiguracionRutina.Rango;
import es.forja.entreno.rutina.modelo.EjercicioRutina;
import es.forja.entreno.rutina.modelo.EntradaRutina;
import es.forja.entreno.rutina.modelo.PlanRutina.ContribucionEjercicio;
import es.forja.entreno.rutina.modelo.PlanRutina.DiaPlan;
import es.forja.entreno.rutina.modelo.PlanRutina.EjercicioPlan;
import es.forja.entreno.rutina.modelo.PlanRutina.Holgura;
import es.forja.entreno.rutina.modelo.PlanRutina.VolumenMusculo;
import es.forja.entreno.rutina.solver.Resolutor.Solucion;

/**
 * Lee la solución y arma el plan por día, el volumen frente a su rango, las holguras con su mensaje y la
 * contribución de cada ejercicio al objetivo. Todo se calcula desde las series del plan, no desde las variables de
 * holgura, para que el mensaje diga siempre lo que el usuario verá.
 */
final class Traza {

	private static final double MITAD = 2.0;
	private static final int DOBLE = 2;

	private final ConfiguracionRutina config;
	private final EntradaRutina entrada;
	private final ModeloRutina modelo;
	private final Solucion solucion;
	private final Map<String, String> nombresMusculo;

	Traza(ConfiguracionRutina config, EntradaRutina entrada, ModeloRutina modelo, Solucion solucion,
			Map<String, String> nombresMusculo) {
		this.config = config;
		this.entrada = entrada;
		this.modelo = modelo;
		this.solucion = solucion;
		this.nombresMusculo = nombresMusculo;
	}

	/** Series de la semana del ejercicio {@code e}. */
	int seriesSemana(int e) {
		return IntStream.of(solucion.series()[e]).sum();
	}

	List<Integer> elegidos() {
		return IntStream.range(0, modelo.ejercicios.size()).filter(e -> seriesSemana(e) > 0).boxed().toList();
	}

	/** Al presentar el día: compuestos primero, luego aislados y, a igualdad, la posición estirada. */
	private static final Comparator<EjercicioRutina> ORDEN_DEL_DIA = Comparator
			.comparing((EjercicioRutina e) -> !e.compuesto()).thenComparing(e -> !e.posicionEstirada())
			.thenComparing(EjercicioRutina::idExterno);

	List<DiaPlan> dias(Map<String, List<String>> anotaciones) {
		ConfiguracionRutina.Tiempos t = config.tiempos();
		List<Integer> orden = IntStream.range(0, modelo.ejercicios.size()).boxed()
				.sorted(Comparator.comparing(modelo.ejercicios::get, ORDEN_DEL_DIA)).toList();
		List<DiaPlan> dias = new ArrayList<>();
		for (int d = 0; d < modelo.dias; d++) {
			List<EjercicioPlan> ejercicios = new ArrayList<>();
			int segundos = 0;
			for (int e : orden) {
				int series = solucion.series()[e][d];
				if (series > 0) {
					EjercicioRutina ej = modelo.ejercicios.get(e);
					ejercicios.add(new EjercicioPlan(ej.idExterno(), ej.nombre(), ej.patron().nombre(), series,
							ej.posicionEstirada(), anotaciones.getOrDefault(ej.idExterno(), List.of())));
					segundos += series * t.segundosPorSerieConDescanso() + t.segundosDeTransicion();
				}
			}
			dias.add(new DiaPlan(d + 1, segundos, ejercicios));
		}
		return dias;
	}

	double seriesEfectivas(String musculo) {
		int dobles = 0;
		for (int e = 0; e < modelo.ejercicios.size(); e++) {
			dobles += modelo.ejercicios.get(e).pesoDoble(musculo) * seriesSemana(e);
		}
		return dobles / MITAD;
	}

	List<VolumenMusculo> volumen() {
		List<String> conMinimo = config.volumen().musculosConMinimo();
		return modelo.musculos().stream().map(m -> {
			Rango rango = config.volumen().rango(m);
			return new VolumenMusculo(m, nombre(m), seriesEfectivas(m),
					conMinimo.contains(m) ? rango.minimo() : null, conMinimo.contains(m) ? rango.objetivo() : null,
					rango.maximo(), Textos.FUENTE_VOLUMEN);
		}).toList();
	}

	List<Holgura> holguras() {
		List<Holgura> holguras = new ArrayList<>();
		for (String m : config.volumen().musculosConMinimo()) {
			int minimo = config.volumen().rango(m).minimo();
			double series = seriesEfectivas(m);
			if (series < minimo) {
				holguras.add(new Holgura(m, Textos.VOLUMEN, minimo - series, Textos.holguraVolumen(entrada.dias(),
						entrada.minutos(), minimo, nombre(m), series)));
			}
			int entrenado = diasEntrenado(m);
			if (entrenado < entrada.frecuenciaMinima()) {
				holguras.add(new Holgura(m, Textos.FRECUENCIA, entrada.frecuenciaMinima() - entrenado,
						Textos.holguraFrecuencia(entrada.dias(), entrada.minutos(), entrada.frecuenciaMinima(),
								nombre(m), entrenado)));
			}
		}
		return holguras;
	}

	List<ContribucionEjercicio> contribuciones() {
		return elegidos().stream().map(e -> {
			EjercicioRutina ej = modelo.ejercicios.get(e);
			int diasUsados = (int) IntStream.of(solucion.series()[e]).filter(s -> s > 0).count();
			int diasDeMas = Math.max(0, diasUsados - ModeloRutina.REPETICIONES_SIN_PENALIZAR);
			return new ContribucionEjercicio(ej.idExterno(), ej.nombre(), Terminos.contribucion(config, ej,
					entrada.prioritarios(), seriesSemana(e), diasDeMas, parteDelVolumen(ej, seriesSemana(e))));
		}).toList();
	}

	/** Lo que aporta el ejercicio al término de volumen objetivo, repartido por series dobles en cada músculo. */
	private long parteDelVolumen(EjercicioRutina ej, int series) {
		double parte = 0;
		for (Map.Entry<String, Integer> m : ej.musculos().entrySet()) {
			double dobles = DOBLE * seriesEfectivas(m.getKey());
			long objetivo = (long) DOBLE * config.volumen().objetivoDe(m.getKey());
			double valor = config.pesos().serieHastaObjetivo() * Math.min(dobles, objetivo)
					- config.pesos().serieSobreObjetivo() * Math.max(0, dobles - objetivo);
			parte += valor * m.getValue() * series / dobles;
		}
		return Math.round(parte);
	}

	/** Días en que el músculo sale como primario en algún ejercicio. */
	private int diasEntrenado(String musculo) {
		int dias = 0;
		for (int d = 0; d < modelo.dias; d++) {
			for (int e = 0; e < modelo.ejercicios.size(); e++) {
				if (solucion.series()[e][d] > 0
						&& modelo.ejercicios.get(e).pesoDoble(musculo) == EjercicioRutina.PESO_PRIMARIO) {
					dias++;
					break;
				}
			}
		}
		return dias;
	}

	private String nombre(String musculo) {
		return nombresMusculo.getOrDefault(musculo, musculo);
	}
}
