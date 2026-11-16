package es.forja.restricciones.cribado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CribadoTest {

	static final Pattern PALABRAS_PROHIBIDAS = Pattern.compile(
			"rehabilit|alivia|corrige|previene lesiones|detecta (desequilibrios|atrofias)|seguro para tu lesi",
			Pattern.CASE_INSENSITIVE);

	private final Cribado cribado = new Cribado();

	private static Map<PreguntaCribado, Boolean> todoNo() {
		Map<PreguntaCribado, Boolean> respuestas = new EnumMap<>(PreguntaCribado.class);
		for (PreguntaCribado p : PreguntaCribado.values()) {
			respuestas.put(p, false);
		}
		return respuestas;
	}

	private static Map<PreguntaCribado, Boolean> soloSi(PreguntaCribado... preguntas) {
		Map<PreguntaCribado, Boolean> respuestas = todoNo();
		for (PreguntaCribado p : preguntas) {
			respuestas.put(p, true);
		}
		return respuestas;
	}

	@Test
	void hayExactamenteSietePreguntasConTexto() {
		assertThat(PreguntaCribado.values()).hasSize(7);
		for (PreguntaCribado p : PreguntaCribado.values()) {
			assertThat(p.texto()).endsWith("?").isNotBlank();
			assertThat(p.motivo()).isNotBlank();
		}
	}

	@Test
	void conTodoNoPuedesSeguir() {
		ResultadoCribado r = cribado.evaluar(new RespuestasCribado(todoNo(), Set.of()));

		assertThat(r.nivel()).isEqualTo(ResultadoCribado.Nivel.SEGUIR);
		assertThat(r.motivos()).isEmpty();
	}

	@ParameterizedTest
	@EnumSource(PreguntaCribado.class)
	void cualquierSiPideConsultarAUnProfesionalConSuMotivo(PreguntaCribado pregunta) {
		ResultadoCribado r = cribado.evaluar(new RespuestasCribado(soloSi(pregunta), Set.of()));

		assertThat(r.nivel()).isEqualTo(ResultadoCribado.Nivel.CONSULTAR_PROFESIONAL);
		assertThat(r.motivos()).containsExactly(pregunta.motivo());
	}

	@Test
	void variosSiAcumulanUnMotivoPorPreguntaEnElOrdenDelCuestionario() {
		ResultadoCribado r = cribado.evaluar(new RespuestasCribado(
				soloSi(PreguntaCribado.EMBARAZO, PreguntaCribado.MEDICACION), Set.of()));

		assertThat(r.nivel()).isEqualTo(ResultadoCribado.Nivel.CONSULTAR_PROFESIONAL);
		assertThat(r.motivos()).containsExactly(PreguntaCribado.MEDICACION.motivo(),
				PreguntaCribado.EMBARAZO.motivo());
	}

	@Test
	void dolorToracicoRecienteEnEsfuerzoParaYMandaAUrgencias() {
		ResultadoCribado r = cribado.evaluar(new RespuestasCribado(
				soloSi(PreguntaCribado.SINTOMAS_EN_ESFUERZO), Set.of(PreguntaCribado.SINTOMAS_EN_ESFUERZO)));

		assertThat(r.nivel()).isEqualTo(ResultadoCribado.Nivel.PARAR_URGENCIAS);
		assertThat(r.motivos()).contains(PreguntaCribado.SINTOMAS_EN_ESFUERZO.motivoUrgente());
	}

	@Test
	void desmayoRecienteParaYMandaAUrgencias() {
		ResultadoCribado r = cribado.evaluar(new RespuestasCribado(
				soloSi(PreguntaCribado.DESMAYO_O_MAREO, PreguntaCribado.MEDICACION),
				Set.of(PreguntaCribado.DESMAYO_O_MAREO)));

		assertThat(r.nivel()).isEqualTo(ResultadoCribado.Nivel.PARAR_URGENCIAS);
		assertThat(r.motivos()).contains(PreguntaCribado.DESMAYO_O_MAREO.motivoUrgente(),
				PreguntaCribado.MEDICACION.motivo());
	}

	@Test
	void sinRecienteElMismoSiSoloPideConsultar() {
		ResultadoCribado r = cribado.evaluar(new RespuestasCribado(
				soloSi(PreguntaCribado.SINTOMAS_EN_ESFUERZO, PreguntaCribado.DESMAYO_O_MAREO), Set.of()));

		assertThat(r.nivel()).isEqualTo(ResultadoCribado.Nivel.CONSULTAR_PROFESIONAL);
	}

	@Test
	void exigeContestarLasSietePreguntas() {
		Map<PreguntaCribado, Boolean> incompletas = todoNo();
		incompletas.remove(PreguntaCribado.EMBARAZO);

		assertThatThrownBy(() -> new RespuestasCribado(incompletas, Set.of()))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("EMBARAZO");
	}

	@Test
	void rechazaMarcarComoRecienteLoQueNoSeHaContestadoQueSi() {
		assertThatThrownBy(() -> new RespuestasCribado(todoNo(), Set.of(PreguntaCribado.DESMAYO_O_MAREO)))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("reciente");
	}

	@Test
	void soloLasDosPreguntasDeSintomasAdmitenMarcaDeReciente() {
		assertThatThrownBy(() -> new RespuestasCribado(soloSi(PreguntaCribado.EMBARAZO),
				Set.of(PreguntaCribado.EMBARAZO)))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("reciente");
	}

	@Test
	void losTextosNoUsanLenguajeSanitarioProhibido() {
		Set<String> textos = new java.util.HashSet<>();
		for (PreguntaCribado p : EnumSet.allOf(PreguntaCribado.class)) {
			textos.add(p.texto());
			textos.add(p.motivo());
			textos.add(p.motivoUrgente());
		}

		assertThat(textos).noneMatch(t -> PALABRAS_PROHIBIDAS.matcher(t).find());
	}

}
