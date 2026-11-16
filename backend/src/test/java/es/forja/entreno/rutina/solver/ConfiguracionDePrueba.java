package es.forja.entreno.rutina.solver;

import java.util.List;
import java.util.Map;

import es.forja.entreno.rutina.modelo.ConfiguracionRutina;
import es.forja.entreno.rutina.modelo.ConfiguracionRutina.Pesos;

/** La misma configuración que {@code application.yml}; {@code RutinasApiTest} comprueba que no se separan. */
public final class ConfiguracionDePrueba {

	public static final List<String> MUSCULOS_GRANDES = List.of("pecho", "dorsales", "espalda_media", "hombros",
			"cuadriceps", "isquiotibiales", "gluteos");

	private ConfiguracionDePrueba() {
	}

	public static ConfiguracionRutina estandar() {
		return new ConfiguracionRutina(
				new Pesos(3, 1, 10, 1, 2, 100, 300, 0, 0),
				new ConfiguracionRutina.Volumen(10, 20,
						List.of("pecho", "dorsales", "espalda_media", "hombros", "cuadriceps", "isquiotibiales",
								"gluteos", "biceps", "triceps", "gemelos"),
						Map.of()),
				new ConfiguracionRutina.Tiempos(40, 90, 120),
				new ConfiguracionRutina.Sesion(2, 5, 3, 8, 2),
				new ConfiguracionRutina.Solver(2.0, 1, 42));
	}

	/** La estándar con otro peso de posición estirada. */
	public static ConfiguracionRutina conPesoEstirada(int peso) {
		ConfiguracionRutina c = estandar();
		Pesos p = c.pesos();
		return new ConfiguracionRutina(new Pesos(p.coberturaPrioritarios(), peso, p.variedad(), p.fatiga(),
				p.series(), p.holguraVolumen(), p.holguraFrecuencia(), p.movilidad(), p.palancas()), c.volumen(),
				c.tiempos(), c.sesion(), c.solver());
	}
}
