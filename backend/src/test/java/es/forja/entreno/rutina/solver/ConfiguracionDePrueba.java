package es.forja.entreno.rutina.solver;

import java.util.List;
import java.util.Map;

import es.forja.entreno.rutina.modelo.ConfiguracionRutina;
import es.forja.entreno.rutina.modelo.ConfiguracionRutina.Pesos;

/** La misma configuración que {@code application.yml}; {@code RutinasApiTest} comprueba que no se separan. */
public final class ConfiguracionDePrueba {

	public static final List<String> MUSCULOS_GRANDES = List.of("pecho", "dorsales", "espalda_media", "hombros",
			"cuadriceps", "isquiotibiales", "gluteos");
	public static final List<String> PATRONES_PRINCIPALES = List.of("sentadilla", "bisagra", "empuje_horizontal",
			"empuje_vertical", "traccion_horizontal", "traccion_vertical");
	private static final int MOVILIDAD = 5;
	private static final int REPETIR_PLAN = 3;

	private ConfiguracionDePrueba() {
	}

	public static ConfiguracionRutina estandar() {
		return new ConfiguracionRutina(
				pesos(2),
				new ConfiguracionRutina.Volumen(10, 14, 20,
						List.of("pecho", "dorsales", "espalda_media", "hombros", "cuadriceps", "isquiotibiales",
								"gluteos", "biceps", "triceps", "gemelos"),
						Map.of()),
				new ConfiguracionRutina.Basicos(PATRONES_PRINCIPALES),
				new ConfiguracionRutina.Tiempos(40, 90, 120),
				new ConfiguracionRutina.Sesion(2, 5, 3, 8, 2, 50, 6),
				new ConfiguracionRutina.Solver(1.0, 0.2, 1, 42));
	}

	/** La estándar con otro peso de posición estirada. */
	public static ConfiguracionRutina conPesoEstirada(int peso) {
		ConfiguracionRutina c = estandar();
		return new ConfiguracionRutina(pesos(peso), c.volumen(), c.basicos(), c.tiempos(), c.sesion(), c.solver());
	}

	private static Pesos pesos(int posicionEstirada) {
		return new Pesos(5, posicionEstirada, 10, 2, 10, 3, 4, 100, 300, 500, 5, MOVILIDAD, 0, REPETIR_PLAN);
	}
}
