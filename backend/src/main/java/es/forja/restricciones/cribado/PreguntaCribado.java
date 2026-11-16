package es.forja.restricciones.cribado;

/**
 * Cuestionario propio de cribado previo al ejercicio, de siete preguntas de sí o no.
 *
 * <p>Redactado para Forja a partir de las categorías del algoritmo de cribado del ACSM (Riebe y
 * cols., 2015). No reproduce el texto del PAR-Q+, cuya licencia no está comprobada.
 */
public enum PreguntaCribado {

	SINTOMAS_EN_ESFUERZO(
			"¿Has notado dolor, opresión o molestia en el pecho, el cuello, la mandíbula o el brazo al hacer esfuerzo?",
			"Notas dolor u opresión al hacer esfuerzo: habla con un profesional sanitario antes de entrenar.",
			"Has notado dolor u opresión al hacer esfuerzo hace poco: no entrenes y acude hoy a urgencias.",
			true),

	DESMAYO_O_MAREO(
			"¿Te has desmayado, o te has mareado con fuerza, al hacer ejercicio?",
			"Te has mareado o desmayado al hacer ejercicio: habla con un profesional sanitario antes de entrenar.",
			"Te has mareado o desmayado al hacer ejercicio hace poco: no entrenes y acude hoy a urgencias.",
			true),

	ENFERMEDAD_CONOCIDA(
			"¿Tienes diagnosticada una enfermedad del corazón, del metabolismo (como la diabetes) o del riñón?",
			"Tienes una enfermedad del corazón, del metabolismo o del riñón: habla con un profesional sanitario antes de entrenar.",
			false),

	MEDICACION(
			"¿Tomas alguna medicación que condicione cómo respondes al ejercicio?",
			"Tomas medicación que condiciona el ejercicio: habla con un profesional sanitario antes de entrenar.",
			false),

	PROBLEMA_MUSCULOESQUELETICO(
			"¿Tienes ahora un problema de hueso, articulación o tejido blando que empeora cuando te mueves o entrenas?",
			"Tienes un problema de hueso, articulación o tejido blando que empeora con la actividad: habla con un profesional sanitario antes de entrenar.",
			false),

	INDICACION_SUPERVISADA(
			"¿Un profesional sanitario te ha indicado que solo hagas actividad física con supervisión?",
			"Te han indicado actividad física supervisada: sigue esa indicación y consulta con tu profesional antes de usar Forja.",
			false),

	EMBARAZO(
			"¿Estás embarazada?",
			"Estás embarazada: habla con tu profesional sanitario antes de entrenar.",
			false);

	private final String texto;
	private final String motivo;
	private final String motivoUrgente;
	private final boolean admiteReciente;

	PreguntaCribado(String texto, String motivo, boolean admiteReciente) {
		this(texto, motivo, motivo, admiteReciente);
	}

	PreguntaCribado(String texto, String motivo, String motivoUrgente, boolean admiteReciente) {
		this.texto = texto;
		this.motivo = motivo;
		this.motivoUrgente = motivoUrgente;
		this.admiteReciente = admiteReciente;
	}

	/** Pregunta tal cual se enseña al usuario. */
	public String texto() {
		return texto;
	}

	/** Por qué se recomienda consultar cuando la respuesta es sí. */
	public String motivo() {
		return motivo;
	}

	/** Motivo cuando el síntoma es reciente; en las preguntas que no lo admiten, igual que {@link #motivo()}. */
	public String motivoUrgente() {
		return motivoUrgente;
	}

	/** Solo los síntomas en esfuerzo y el desmayo pueden marcarse como recientes. */
	public boolean admiteReciente() {
		return admiteReciente;
	}

}
