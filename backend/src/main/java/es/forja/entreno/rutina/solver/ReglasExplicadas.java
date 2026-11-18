package es.forja.entreno.rutina.solver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import es.forja.entreno.movilidad.modelo.TestMovilidad;
import es.forja.entreno.movilidad.modelo.Tramo;
import es.forja.entreno.rutina.modelo.ConfiguracionRutina;
import es.forja.entreno.rutina.modelo.EjercicioRutina;
import es.forja.entreno.rutina.modelo.PlanRutina.VolumenMusculo;

/**
 * «Por qué este ejercicio» (PRD v0.1, RF-19): cada regla con la que el solver elige o puntúa un ejercicio, con su
 * plantilla, su fuente y su nivel (ADR 0007: A-D, H = heurística de entrenadores, P = propuesta propia). Toda cita
 * «Apellido Año» de una fuente está en {@code docs/referencias.md} ({@code ReglasExplicadasTest} lo comprueba).
 *
 * <p>Solo explica lo que el solver usó: los términos del objetivo que puntuaron el ejercicio (su contribución en la
 * traza), los filtros que pasó y las restricciones duras del día. Las razones de salud (limitación y movilidad) llegan
 * en el {@link Contexto} solo si el consentimiento sigue vigente y nunca se guardan (ADR 0004).
 */
public final class ReglasExplicadas {

	public static final int MAXIMO = 4;

	public static final String LIMITACION = "limitacion";
	public static final String PRIORITARIO = "prioritario";
	public static final String VOLUMEN = "volumen";
	public static final String MOVILIDAD = "movilidad";
	public static final String COMPUESTO = "compuesto";
	public static final String POSICION_ESTIRADA = "posicion_estirada";
	public static final String FRECUENCIA = "frecuencia";
	public static final String TOPE_PATRON = "tope_patron";
	public static final String MATERIAL = "material";

	/** A-D: nivel de evidencia de la fuente; H: heurística de entrenadores; P: propuesta propia. */
	public enum Nivel {
		A, B, C, D, H, P
	}

	/** @param plantilla texto con huecos {@code %s}/{@code %d} que rellena {@link #porQue} */
	public record Regla(String codigo, String plantilla, String fuente, Nivel nivel) {

		Razon razon(Object... valores) {
			return new Razon(codigo, plantilla.formatted(valores), fuente, nivel.name());
		}
	}

	/** Una razón ya rellena, como la enseña la app. */
	public record Razon(String codigo, String texto, String fuente, String nivel) {
	}

	/** Fuente de la posición estirada de un ejercicio concreto (catálogo curado, {@code curado.yaml}). */
	public record FuenteEstirada(String fuente, Nivel nivel, String detalle) {
	}

	/**
	 * Lo que hace falta para explicar un ejercicio del plan.
	 *
	 * @param terminos su contribución a cada término del objetivo (traza guardada); vacía si no hay
	 * @param seriesSemana sus series en la semana
	 * @param diasEnPlan cuántos días de la semana sale
	 * @param seriesPatronDia series de su patrón en el día que se explica
	 * @param sustituye nombres de los ejercicios a los que sustituye por las limitaciones vigentes (dato de salud)
	 * @param tramos tramos de movilidad vigentes (dato de salud); vacío sin consentimiento
	 */
	public record Contexto(EjercicioRutina ejercicio, Map<String, Long> terminos, List<VolumenMusculo> volumen,
			int seriesSemana, int diasEnPlan, int seriesPatronDia, List<String> sustituye,
			Map<TestMovilidad, Tramo> tramos) {

		public Contexto {
			terminos = Map.copyOf(terminos);
			volumen = List.copyOf(volumen);
			sustituye = List.copyOf(sustituye);
			tramos = Map.copyOf(tramos);
		}

		long termino(String codigo) {
			return terminos.getOrDefault(codigo, 0L);
		}
	}

	private static final Regla REGLA_LIMITACION = new Regla(LIMITACION,
			"Ocupa el sitio de %s y es compatible con la limitación que indicaste.",
			"Escalera de sustitución de Forja (ADR 0007)", Nivel.P);
	private static final Regla REGLA_PRIORITARIO = new Regla(PRIORITARIO,
			"Trabaja un músculo que has marcado como prioritario.", "Propuesta de Forja (ADR 0002)", Nivel.P);
	private static final Regla REGLA_VOLUMEN = new Regla(VOLUMEN,
			"Suma %s series semanales de %s; el plan te deja en %s de un objetivo de %d.",
			"Renaissance Periodization; secundarios a 0,5 por Pelland 2026", Nivel.H);
	private static final Regla REGLA_MOVILIDAD = new Regla(MOVILIDAD, "Encaja con tu tramo de %s (%s): %s",
			"Heurística de entrenadores, no ciencia (ADR 0007)", Nivel.H);
	private static final Regla REGLA_COMPUESTO = new Regla(COMPUESTO,
			"Es un compuesto de %s y cubre uno de los patrones básicos de la semana.",
			"Propuesta de Forja (ADR 0002)", Nivel.P);
	/** La fuente y el nivel de la posición estirada dependen del ejercicio: ver {@link #FUENTES_ESTIRADA}. */
	private static final Regla REGLA_ESTIRADA = new Regla(POSICION_ESTIRADA, "Carga el músculo estirado: %s.",
			"Pedrosa 2026", Nivel.C);
	private static final Regla REGLA_FRECUENCIA = new Regla(FRECUENCIA,
			"Lo haces %d días por semana: repartir las series acorta las sesiones. Es una heurística práctica, no "
					+ "un requisito para ganar músculo.",
			"Schoenfeld 2019; Pelland 2026", Nivel.H);
	private static final Regla REGLA_TOPE = new Regla(TOPE_PATRON,
			"Este día el patrón de %s llega al tope de %d series; por eso no lleva más.",
			"Heurística de entrenadores (ADR 0002)", Nivel.H);
	private static final Regla REGLA_MATERIAL = new Regla(MATERIAL, "Encaja con tu material: %s.",
			"Filtro previo de Forja (ADR 0002)", Nivel.P);

	/** Todas las reglas que puede citar la app. */
	public static final List<Regla> REGLAS = List.of(REGLA_LIMITACION, REGLA_PRIORITARIO, REGLA_VOLUMEN,
			REGLA_MOVILIDAD, REGLA_COMPUESTO, REGLA_ESTIRADA, REGLA_FRECUENCIA, REGLA_TOPE, REGLA_MATERIAL);

	private static final FuenteEstirada PEDROSA = new FuenteEstirada(REGLA_ESTIRADA.fuente(), REGLA_ESTIRADA.nivel(),
			"la ventaja de entrenar estirado aparece cuando la carga pesa con el músculo largo (evidencia indirecta)");
	private static final FuenteEstirada TRICEPS = new FuenteEstirada("Maeo 2023", Nivel.B,
			"con el brazo por encima de la cabeza el tríceps creció más que con el brazo pegado");
	private static final FuenteEstirada GEMELO = new FuenteEstirada("Kinoshita 2023", Nivel.B,
			"de pie, con la rodilla extendida, el gemelo creció más que sentado");

	/** Fuente de la posición estirada por ejercicio, la misma que cita {@code database/catalogo/curado.yaml}. */
	public static final Map<String, FuenteEstirada> FUENTES_ESTIRADA = Map.of(
			"Seated_Leg_Curl", new FuenteEstirada("Maeo 2021", Nivel.B,
					"sentado, con la cadera flexionada, el isquio creció más que en el curl tumbado"),
			"Standing_Dumbbell_Triceps_Extension", TRICEPS,
			"Triceps_Overhead_Extension_with_Rope", TRICEPS,
			"Standing_Calf_Raises", GEMELO,
			"Standing_Dumbbell_Calf_Raise", GEMELO,
			"Incline_Dumbbell_Curl", new FuenteEstirada("Kobayashi 2026", Nivel.B,
					"inclinado, con el hombro extendido, el bíceps trabaja más largo que en el predicador"),
			"Romanian_Deadlift", PEDROSA,
			"Stiff-Legged_Dumbbell_Deadlift", PEDROSA,
			"Dumbbell_Flyes", PEDROSA);

	private static final Locale ES = Locale.forLanguageTag("es-ES");
	/** Sin peso en el objetivo, lo del día va detrás de lo que puntuó y el material, al final. */
	private static final long SIN_PESO = 0;
	private static final long AL_FINAL = -1;
	private static final double MITAD = 2.0;

	private ReglasExplicadas() {
	}

	public static Regla regla(String codigo) {
		return REGLAS.stream().filter(r -> r.codigo().equals(codigo)).findFirst().orElseThrow();
	}

	/** Las {@value #MAXIMO} razones que más pesaron, la primera la más importante. */
	public static List<Razon> porQue(ConfiguracionRutina config, Contexto c) {
		return todas(config, c).stream().limit(MAXIMO).toList();
	}

	/**
	 * Todas, ordenadas: la limitación primero (es una restricción de seguridad, ADR 0007), después cada término del
	 * objetivo por los puntos que aportó, lo del día y el material.
	 */
	static List<Razon> todas(ConfiguracionRutina config, Contexto c) {
		record Pesada(long peso, Razon razon) {
		}
		EjercicioRutina e = c.ejercicio();
		List<Pesada> razones = new ArrayList<>();
		// Una sola razón aunque sustituya a varios (la prensa ocupa el sitio de todas las sentadillas): si no, tapan las demás.
		if (!c.sustituye().isEmpty()) {
			String primero = minusculaInicial(c.sustituye().get(0));
			int resto = c.sustituye().size() - 1;
			razones.add(new Pesada(Long.MAX_VALUE,
					REGLA_LIMITACION.razon(resto == 0 ? primero : primero + " (y " + resto + " más)")));
		}
		long prioritarios = c.termino(Terminos.COBERTURA_PRIORITARIOS);
		if (prioritarios > 0) {
			razones.add(new Pesada(prioritarios, REGLA_PRIORITARIO.razon()));
		}
		long volumen = c.termino(Terminos.VOLUMEN);
		if (volumen > 0) {
			volumen(c).ifPresent(r -> razones.add(new Pesada(volumen, r)));
		}
		ReglasMovilidad.favorecen(e.idExterno(), c.tramos()).forEach(f -> razones.add(new Pesada(
				Math.max(1, c.termino(Terminos.MOVILIDAD)),
				REGLA_MOVILIDAD.razon(f.test().codigo(), f.tramo().codigo(), f.porQue()))));
		long compuesto = c.termino(Terminos.COMPUESTO);
		if (compuesto > 0) {
			razones.add(new Pesada(compuesto, REGLA_COMPUESTO.razon(e.patron().nombre())));
		}
		long estirada = c.termino(Terminos.POSICION_ESTIRADA);
		if (estirada > 0) {
			razones.add(new Pesada(estirada, estirada(e.idExterno())));
		}
		if (c.diasEnPlan() > 1) {
			razones.add(new Pesada(SIN_PESO, REGLA_FRECUENCIA.razon(c.diasEnPlan())));
		}
		int tope = config.sesion().seriesMaximasPorPatronYDia();
		if (!ModeloRutina.SIN_TOPE_DE_SERIES_POR_DIA.contains(e.patron()) && c.seriesPatronDia() >= tope) {
			razones.add(new Pesada(SIN_PESO, REGLA_TOPE.razon(e.patron().nombre(), tope)));
		}
		razones.add(new Pesada(AL_FINAL, REGLA_MATERIAL.razon(e.nombreMaterial().toLowerCase(ES))));
		// sort es estable: a igual peso, el orden de arriba
		return razones.stream().sorted(Comparator.comparingLong(Pesada::peso).reversed()).map(Pesada::razon)
				.toList();
	}

	/** Del primer músculo primario con meta de volumen (por código, para que salga siempre igual). */
	private static Optional<Razon> volumen(Contexto c) {
		EjercicioRutina e = c.ejercicio();
		return c.volumen().stream()
				.filter(v -> v.objetivo() != null && e.pesoDoble(v.musculo()) == EjercicioRutina.PESO_PRIMARIO)
				.min(Comparator.comparing(VolumenMusculo::musculo))
				.map(v -> REGLA_VOLUMEN.razon(Textos.numero(c.seriesSemana() * e.pesoDoble(v.musculo()) / MITAD),
						v.nombre().toLowerCase(ES), Textos.numero(v.series()), v.objetivo()));
	}

	private static Razon estirada(String idExterno) {
		FuenteEstirada f = FUENTES_ESTIRADA.getOrDefault(idExterno, PEDROSA);
		return new Razon(POSICION_ESTIRADA, REGLA_ESTIRADA.plantilla().formatted(f.detalle()), f.fuente(),
				f.nivel().name());
	}

	private static String minusculaInicial(String texto) {
		return texto.isEmpty() ? texto : Character.toLowerCase(texto.charAt(0)) + texto.substring(1);
	}
}
