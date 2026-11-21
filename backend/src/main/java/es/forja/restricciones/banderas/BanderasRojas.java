package es.forja.restricciones.banderas;

import static es.forja.restricciones.banderas.BanderaRoja.Nivel.PROFESIONAL_EN_DIAS;
import static es.forja.restricciones.banderas.BanderaRoja.Nivel.URGENCIAS_HOY;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Banderas rojas por reglas deterministas; nunca las decide un modelo de lenguaje (ADR 0007).
 *
 * <p>Lista provisional, pendiente de revisión por un fisioterapeuta antes de la v0.2. Cada regla
 * cita la guía pública de la que sale; las propuestas propias van marcadas. Basta una bandera
 * para parar la generación de la rutina.
 */
public class BanderasRojas {

	private static final String IFOMPT_COLUMNA =
			"IFOMPT, marco internacional de banderas rojas de columna (Finucane y cols., 2020)";
	private static final String IFOMPT_CAUDA =
			IFOMPT_COLUMNA + "; revisión de Dionne y cols. (2019) sobre síndrome de cola de caballo";
	private static final String ACSM_CRIBADO =
			"ACSM, cribado previo a la participación (Riebe y cols., 2015)";
	private static final String NICE_ESPALDA = "NICE NG59, dolor lumbar y ciática: pensar en otros diagnósticos";

	/** Evalúa las respuestas; las banderas de urgencias salen antes que las de profesional en días. */
	public List<BanderaRoja> evaluar(RespuestasGuiadas r) {
		List<BanderaRoja> banderas = new ArrayList<>();
		if (r.perdidaControlEsfinteresOAnestesiaSillaDeMontar()) {
			banderas.add(new BanderaRoja(URGENCIAS_HOY,
					"Has perdido el control de la vejiga o el intestino, o notas la zona genital o anal dormida: no entrenes y acude hoy a urgencias.",
					IFOMPT_CAUDA));
		}
		if (r.dolorToracico()) {
			banderas.add(new BanderaRoja(URGENCIAS_HOY,
					"Notas dolor u opresión en el pecho: no entrenes y acude hoy a urgencias.", ACSM_CRIBADO));
		}
		if (r.mareoODesmayo()) {
			banderas.add(new BanderaRoja(URGENCIAS_HOY,
					"Te has mareado o desmayado: no entrenes y acude hoy a urgencias.", ACSM_CRIBADO));
		}
		agregarNeurologicas(r, banderas);
		agregarGeneralesEnDias(r, banderas);
		banderas.sort(Comparator.comparing(BanderaRoja::nivel));
		return List.copyOf(banderas);
	}

	/** Basta una bandera, de cualquier nivel, para no generar la rutina. */
	public static boolean bloqueaLaRutina(List<BanderaRoja> banderas) {
		return !banderas.isEmpty();
	}

	private static void agregarNeurologicas(RespuestasGuiadas r, List<BanderaRoja> banderas) {
		if (r.perdidaDeFuerza() && r.hormigueoOEntumecimiento()) {
			// Propuesta propia: la pregunta solo recoge la debilidad nueva o que va a más (una crónica ya valorada,
			// como una hipotonía conocida, no cuenta), y con hormigueo se trata como déficit neurológico: urgencias.
			banderas.add(new BanderaRoja(URGENCIAS_HOY,
					"Notas una pérdida de fuerza nueva o que va a más, y hormigueo o falta de sensibilidad: no entrenes y acude hoy a urgencias.",
					IFOMPT_COLUMNA + " (propuesta propia para la combinación)"));
		} else if (r.perdidaDeFuerza()) {
			banderas.add(new BanderaRoja(PROFESIONAL_EN_DIAS,
					"Notas una pérdida de fuerza nueva o que va a más en un brazo o una pierna: consulta con un profesional sanitario en los próximos días antes de entrenar.",
					IFOMPT_COLUMNA));
		} else if (r.hormigueoOEntumecimiento()) {
			banderas.add(new BanderaRoja(PROFESIONAL_EN_DIAS,
					"Notas hormigueo o falta de sensibilidad: consulta con un profesional sanitario en los próximos días antes de entrenar.",
					IFOMPT_COLUMNA));
		}
	}

	private static void agregarGeneralesEnDias(RespuestasGuiadas r, List<BanderaRoja> banderas) {
		if (r.fiebreConDolorDeEspalda()) {
			banderas.add(new BanderaRoja(PROFESIONAL_EN_DIAS,
					"Tienes fiebre y dolor de espalda: consulta con un profesional sanitario en los próximos días antes de entrenar.",
					IFOMPT_COLUMNA + "; " + NICE_ESPALDA));
		}
		if (r.traumatismoReciente() && r.hayDolor()) {
			banderas.add(new BanderaRoja(PROFESIONAL_EN_DIAS,
					"Tienes dolor después de un golpe o una caída recientes: consulta con un profesional sanitario en los próximos días antes de entrenar.",
					IFOMPT_COLUMNA + "; " + NICE_ESPALDA));
		}
		if (r.perdidaDePesoSinExplicacion()) {
			banderas.add(new BanderaRoja(PROFESIONAL_EN_DIAS,
					"Has perdido peso sin explicación: consulta con un profesional sanitario en los próximos días antes de entrenar.",
					IFOMPT_COLUMNA));
		}
		if (r.dolorNocturnoQueDespierta()) {
			banderas.add(new BanderaRoja(PROFESIONAL_EN_DIAS,
					"Un dolor te despierta por la noche: consulta con un profesional sanitario en los próximos días antes de entrenar.",
					NICE_ESPALDA));
		}
		if (r.dolorEnReposo() && r.hayDolor()) {
			banderas.add(new BanderaRoja(PROFESIONAL_EN_DIAS,
					"Tienes dolor aunque estés en reposo: consulta con un profesional sanitario en los próximos días antes de entrenar.",
					NICE_ESPALDA));
		}
	}

}
