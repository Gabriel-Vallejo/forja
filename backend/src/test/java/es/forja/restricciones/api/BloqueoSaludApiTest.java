package es.forja.restricciones.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import es.forja.cuenta.servicio.LimitadorIntentos;

/** Una bandera roja o un cribado urgente bloquean «generar plan» hasta que un cuestionario posterior lo levante (RF-04). */
class BloqueoSaludApiTest extends SoporteLimitaciones {

	private static final String RUTA_ESTADO = "/api/v1/banderas/estado";
	private static final String RUTA_GENERAR = "/api/v1/rutinas/generar";
	private static final List<String> PREGUNTAS = List.of("sintomas_en_esfuerzo", "desmayo_o_mareo",
			"enfermedad_conocida", "medicacion", "problema_musculoesqueletico", "indicacion_supervisada", "embarazo");
	private static final List<String> SENALES = List.of("dolorEnReposo", "dolorNocturnoQueDespierta",
			"hormigueoOEntumecimiento", "perdidaDeFuerza", "perdidaControlEsfinteresOAnestesiaSillaDeMontar",
			"fiebreConDolorDeEspalda", "traumatismoReciente", "perdidaDePesoSinExplicacion", "dolorToracico",
			"mareoODesmayo");

	private static Map<String, Object> cribado(String sintomaReciente) {
		return cribado(sintomaReciente, sintomaReciente);
	}

	private static Map<String, Object> cribado(String si, String reciente) {
		Map<String, Boolean> respuestas = new LinkedHashMap<>();
		PREGUNTAS.forEach(p -> respuestas.put(p, p.equals(si)));
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("respuestas", respuestas);
		cuerpo.put("recientes", reciente == null ? List.of() : List.of(reciente));
		return cuerpo;
	}

	private static Map<String, Object> banderas(String... activas) {
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("dolorPorZona", new LinkedHashMap<String, Integer>());
		SENALES.forEach(s -> cuerpo.put(s, List.of(activas).contains(s)));
		return cuerpo;
	}

	private static Map<String, Object> solicitudRutina() {
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("diasPorSemana", 3);
		cuerpo.put("minutosPorSesion", 60);
		cuerpo.put("material", List.of("barra", "mancuerna", "maquina", "polea", "peso_corporal"));
		cuerpo.put("exclusiones", List.of());
		cuerpo.put("prioritarios", List.of("pecho"));
		return cuerpo;
	}

	private void exigirEstado(Cliente cliente, boolean bloquea, String nivel) throws Exception {
		var respuesta = consultar(RUTA_ESTADO, cliente).andExpect(status().isOk())
				.andExpect(jsonPath("$.bloquea").value(bloquea));
		if (nivel == null) {
			respuesta.andExpect(jsonPath("$.nivel").doesNotExist());
		} else {
			respuesta.andExpect(jsonPath("$.nivel").value(nivel));
		}
	}

	@Test
	void sinCuestionariosElEstadoNoBloqueaYNoExigeConsentimiento() throws Exception {
		Cliente sinConsentimiento = cuentaConSesion("Pixel");

		exigirEstado(sinConsentimiento, false, null);
	}

	@Test
	void unaBanderaRojaBloqueaYSeGuardaSoloElNivelCifrado() throws Exception {
		Cliente cliente = clienteConLimitaciones();

		enviarAutenticado("/api/v1/banderas", cliente, banderas("dolorToracico")).andExpect(status().isOk())
				.andExpect(jsonPath("$.bloquea").value(true));

		exigirEstado(cliente, true, "urgencias_hoy");
		assertThat(contar("SELECT COUNT(*) FROM bloqueo_salud")).isPositive();
		byte[] guardado = jdbc.sql("SELECT datos FROM bloqueo_salud LIMIT 1").query(byte[].class).single();
		assertThat(new String(guardado, java.nio.charset.StandardCharsets.ISO_8859_1)).doesNotContain("urgencias_hoy",
				"URGENCIAS_HOY");
	}

	@Test
	void otroUsuarioNoVeElBloqueoAjeno() throws Exception {
		Cliente bloqueado = clienteConLimitaciones();
		Cliente otro = clienteConLimitaciones();
		enviarAutenticado("/api/v1/banderas", bloqueado, banderas("dolorToracico")).andExpect(status().isOk());

		exigirEstado(otro, false, null);
	}

	@Test
	void unCuestionarioSinBanderasLevantaElBloqueoDeBanderas() throws Exception {
		Cliente cliente = clienteConLimitaciones();
		enviarAutenticado("/api/v1/banderas", cliente, banderas("dolorToracico")).andExpect(status().isOk());

		enviarAutenticado("/api/v1/banderas", cliente, banderas()).andExpect(status().isOk())
				.andExpect(jsonPath("$.bloquea").value(false));

		exigirEstado(cliente, false, null);
	}

	@Test
	void dosBanderasSeguidasConservanElNivelMasGrave() throws Exception {
		Cliente cliente = clienteConLimitaciones();
		enviarAutenticado("/api/v1/banderas", cliente, banderas("perdidaDePesoSinExplicacion"))
				.andExpect(status().isOk());
		exigirEstado(cliente, true, "profesional_en_dias");

		enviarAutenticado("/api/v1/banderas", cliente, banderas("dolorToracico")).andExpect(status().isOk());

		exigirEstado(cliente, true, "urgencias_hoy");
		assertThat(contar("SELECT COUNT(*) FROM bloqueo_salud b JOIN usuario u ON u.id = b.usuario_id "
				+ "WHERE u.correo = ?", cliente.correo())).isEqualTo(1);
	}

	@Test
	void unCribadoUrgenteBloqueaYSobreviveAUnCuestionarioDeBanderasLimpio() throws Exception {
		Cliente cliente = clienteConLimitaciones();

		enviarAutenticado("/api/v1/cribado", cliente, cribado("sintomas_en_esfuerzo")).andExpect(status().isOk())
				.andExpect(jsonPath("$.nivel").value("parar_urgencias"));
		exigirEstado(cliente, true, "parar_urgencias");

		enviarAutenticado("/api/v1/banderas", cliente, banderas()).andExpect(status().isOk());

		exigirEstado(cliente, true, "parar_urgencias");
	}

	@Test
	void unCribadoQueNoEsUrgenteLevantaElBloqueoDelCribado() throws Exception {
		Cliente cliente = clienteConLimitaciones();
		enviarAutenticado("/api/v1/cribado", cliente, cribado("sintomas_en_esfuerzo")).andExpect(status().isOk());

		enviarAutenticado("/api/v1/cribado", cliente, cribado(null)).andExpect(status().isOk())
				.andExpect(jsonPath("$.nivel").value("seguir"));

		exigirEstado(cliente, false, null);
	}

	@Test
	void conCribadoYBanderaElEstadoDaElMasGraveYCadaOrigenSeLevantaPorSuCuenta() throws Exception {
		Cliente cliente = clienteConLimitaciones();
		enviarAutenticado("/api/v1/cribado", cliente, cribado("sintomas_en_esfuerzo")).andExpect(status().isOk());
		enviarAutenticado("/api/v1/banderas", cliente, banderas("dolorToracico")).andExpect(status().isOk());
		exigirEstado(cliente, true, "urgencias_hoy");

		enviarAutenticado("/api/v1/banderas", cliente, banderas()).andExpect(status().isOk());
		exigirEstado(cliente, true, "parar_urgencias");

		enviarAutenticado("/api/v1/cribado", cliente, cribado("medicacion", null)).andExpect(status().isOk())
				.andExpect(jsonPath("$.nivel").value("consultar_profesional"));
		exigirEstado(cliente, false, null);
	}

	@Test
	void generarDa409ConCodigoEstableMientrasHayBloqueoYFuncionaAlLevantarlo() throws Exception {
		Cliente cliente = clienteConLimitaciones();
		enviarAutenticado("/api/v1/banderas", cliente, banderas("dolorToracico")).andExpect(status().isOk());

		enviarAutenticado(RUTA_GENERAR, cliente, solicitudRutina()).andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("bandera_roja_activa"))
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("urgencias")));
		assertThat(contar("SELECT COUNT(*) FROM plan_version pv JOIN usuario u ON u.id = pv.usuario_id "
				+ "WHERE u.correo = ?", cliente.correo())).isZero();

		enviarAutenticado("/api/v1/banderas", cliente, banderas()).andExpect(status().isOk());

		enviarAutenticado(RUTA_GENERAR, cliente, solicitudRutina()).andExpect(status().isOk());
	}

	@Test
	void unNivelDeProfesionalEnDiasPideHablarConUnProfesional() throws Exception {
		Cliente cliente = clienteConLimitaciones();
		enviarAutenticado("/api/v1/banderas", cliente, banderas("perdidaDePesoSinExplicacion"))
				.andExpect(status().isOk());

		enviarAutenticado(RUTA_GENERAR, cliente, solicitudRutina()).andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("bandera_roja_activa"))
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("profesional")));
	}

	@Test
	void unGenerarBloqueadoNoGastaIntentosDelLimitador() throws Exception {
		Cliente cliente = clienteConLimitaciones();
		enviarAutenticado("/api/v1/banderas", cliente, banderas("dolorToracico")).andExpect(status().isOk());

		for (int i = 0; i < 30; i++) {
			enviarAutenticado(RUTA_GENERAR, cliente, solicitudRutina()).andExpect(status().isConflict());
		}
	}

	@Test
	void revocarElConsentimientoDeLimitacionesBorraLosBloqueos() throws Exception {
		Cliente cliente = clienteConLimitaciones();
		enviarAutenticado("/api/v1/banderas", cliente, banderas("dolorToracico")).andExpect(status().isOk());
		enviarAutenticado("/api/v1/cribado", cliente, cribado("sintomas_en_esfuerzo")).andExpect(status().isOk());

		borrarRecurso("/api/v1/consentimientos/limitaciones", cliente).andExpect(status().isNoContent());

		assertThat(contar("SELECT COUNT(*) FROM bloqueo_salud b JOIN usuario u ON u.id = b.usuario_id "
				+ "WHERE u.correo = ?", cliente.correo())).isZero();
		exigirEstado(cliente, false, null);
	}

	@Test
	void sinConsentimientoLosCuestionariosSiguenDando403YNoBloquean() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");

		enviarAutenticado("/api/v1/banderas", cliente, banderas("dolorToracico")).andExpect(status().isForbidden());

		exigirEstado(cliente, false, null);
	}
}
