package es.forja.restricciones.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CribadoBanderasApiTest extends SoporteLimitaciones {

	private static final List<String> PREGUNTAS = List.of("sintomas_en_esfuerzo", "desmayo_o_mareo",
			"enfermedad_conocida", "medicacion", "problema_musculoesqueletico", "indicacion_supervisada", "embarazo");
	private static final List<String> SENALES = List.of("dolorEnReposo", "dolorNocturnoQueDespierta",
			"hormigueoOEntumecimiento", "perdidaDeFuerza", "perdidaControlEsfinteresOAnestesiaSillaDeMontar",
			"fiebreConDolorDeEspalda", "traumatismoReciente", "perdidaDePesoSinExplicacion", "dolorToracico",
			"mareoODesmayo");

	private static Map<String, Object> cribado(List<String> si, List<String> recientes) {
		Map<String, Boolean> respuestas = new LinkedHashMap<>();
		PREGUNTAS.forEach(p -> respuestas.put(p, si.contains(p)));
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("respuestas", respuestas);
		cuerpo.put("recientes", recientes);
		return cuerpo;
	}

	private static Map<String, Object> guiadas(String... activas) {
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("dolorPorZona", new LinkedHashMap<String, Integer>());
		SENALES.forEach(s -> cuerpo.put(s, List.of(activas).contains(s)));
		return cuerpo;
	}

	@Test
	void cribadoSinSiesDejaSeguirYNoGuardaNada() throws Exception {
		Cliente cliente = clienteConLimitaciones();
		long antes = contar("SELECT COUNT(*) FROM restriccion");

		enviarAutenticado("/api/v1/cribado", cliente, cribado(List.of(), List.of())).andExpect(status().isOk())
				.andExpect(jsonPath("$.nivel").value("seguir")).andExpect(jsonPath("$.motivos", hasSize(0)));

		assertThat(contar("SELECT COUNT(*) FROM restriccion")).isEqualTo(antes);
	}

	@Test
	void cribadoConUnSiRecomiendaConsultarYConSintomaRecienteMandaAUrgencias() throws Exception {
		Cliente cliente = clienteConLimitaciones();

		enviarAutenticado("/api/v1/cribado", cliente, cribado(List.of("medicacion"), List.of()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.nivel").value("consultar_profesional"))
				.andExpect(jsonPath("$.motivos", hasSize(1)));
		enviarAutenticado("/api/v1/cribado", cliente,
				cribado(List.of("sintomas_en_esfuerzo"), List.of("sintomas_en_esfuerzo")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.nivel").value("parar_urgencias"));
	}

	@Test
	void cribadoIncompletoOIncoherenteDa400() throws Exception {
		Cliente cliente = clienteConLimitaciones();
		Map<String, Object> incompleto = cribado(List.of(), List.of());
		@SuppressWarnings("unchecked")
		Map<String, Boolean> respuestas = (Map<String, Boolean>) incompleto.get("respuestas");
		respuestas.remove("embarazo");
		Map<String, Object> pregunta = cribado(List.of(), List.of());
		@SuppressWarnings("unchecked")
		Map<String, Boolean> conRara = (Map<String, Boolean>) pregunta.get("respuestas");
		conRara.put("pregunta_inventada", true);

		enviarAutenticado("/api/v1/cribado", cliente, incompleto).andExpect(status().isBadRequest());
		enviarAutenticado("/api/v1/cribado", cliente, pregunta).andExpect(status().isBadRequest());
		enviarAutenticado("/api/v1/cribado", cliente, cribado(List.of(), List.of("sintomas_en_esfuerzo")))
				.andExpect(status().isBadRequest());
		enviarAutenticado("/api/v1/cribado", cliente, cribado(List.of("medicacion"), List.of("medicacion")))
				.andExpect(status().isBadRequest());
		enviarAutenticado("/api/v1/cribado", cliente, Map.of()).andExpect(status().isBadRequest());
	}

	@Test
	void sinBanderasLaRutinaNoSeBloquea() throws Exception {
		Cliente cliente = clienteConLimitaciones();

		enviarAutenticado("/api/v1/banderas", cliente, guiadas()).andExpect(status().isOk())
				.andExpect(jsonPath("$.bloquea").value(false)).andExpect(jsonPath("$.banderas", hasSize(0)));
	}

	@Test
	void unaBanderaDeUrgenciasBloqueaYSaleLaPrimera() throws Exception {
		Cliente cliente = clienteConLimitaciones();

		enviarAutenticado("/api/v1/banderas", cliente,
				guiadas("perdidaControlEsfinteresOAnestesiaSillaDeMontar", "dolorEnReposo"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.bloquea").value(true))
				.andExpect(jsonPath("$.banderas[0].nivel").value("urgencias_hoy"))
				.andExpect(jsonPath("$.banderas[0].motivo").isNotEmpty())
				.andExpect(jsonPath("$.banderas[0].guia").isNotEmpty());
	}

	@Test
	void elDolorPorZonaUsaCodigosDeRegionYAdmiteDe0A10() throws Exception {
		Cliente cliente = clienteConLimitaciones();
		Map<String, Object> bien = guiadas();
		bien.put("dolorPorZona", Map.of("columna_lumbar", 3));
		Map<String, Object> pasado = guiadas();
		pasado.put("dolorPorZona", Map.of("rodilla", 11));
		Map<String, Object> regionRara = guiadas();
		regionRara.put("dolorPorZona", Map.of("zona_inventada", 3));

		enviarAutenticado("/api/v1/banderas", cliente, bien).andExpect(status().isOk());
		enviarAutenticado("/api/v1/banderas", cliente, pasado).andExpect(status().isBadRequest());
		enviarAutenticado("/api/v1/banderas", cliente, regionRara).andExpect(status().isBadRequest());
	}

	@Test
	void faltarUnaRespuestaDa400EnVezDeContarComoNo() throws Exception {
		Cliente cliente = clienteConLimitaciones();
		Map<String, Object> cuerpo = guiadas();
		cuerpo.remove("dolorToracico");

		enviarAutenticado("/api/v1/banderas", cliente, cuerpo).andExpect(status().isBadRequest());
	}

	@Test
	void banderasNoGuardaNada() throws Exception {
		Cliente cliente = clienteConLimitaciones();
		long antes = contar("SELECT COUNT(*) FROM restriccion");

		enviarAutenticado("/api/v1/banderas", cliente, guiadas("dolorToracico")).andExpect(status().isOk());

		assertThat(contar("SELECT COUNT(*) FROM restriccion")).isEqualTo(antes);
	}
}
