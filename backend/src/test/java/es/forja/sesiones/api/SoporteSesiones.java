package es.forja.sesiones.api;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.test.web.servlet.ResultActions;

import es.forja.comun.repositorio.Conversiones;
import es.forja.restricciones.api.SoporteLimitaciones;
import tools.jackson.databind.JsonNode;

/** Atajos de los tests del plan versionado y del registro de sesiones (ADR 0008). */
public abstract class SoporteSesiones extends SoporteLimitaciones {

	protected static final String RUTA_GENERAR = "/api/v1/rutinas/generar";
	protected static final String RUTA_PLAN_ACTUAL = "/api/v1/planes/actual";
	protected static final String RUTA_SESIONES = "/api/v1/sesiones";
	private static final List<String> MATERIAL_COMPLETO = List.of("barra", "mancuerna", "maquina", "polea",
			"peso_corporal", "kettlebell", "banda", "barra_ez", "balon_medicinal", "fitball", "rodillo", "otro");

	/** Lo que el plan dice de un ejercicio para entrenar: el objetivo y la regla de la siguiente. */
	protected record Objetivo(String idExterno, int series, int repeticionesMin, int repeticionesMax,
			int rirObjetivo, double incrementoKg, JsonNode cargaKg) {
	}

	/** Genera un plan de 3 días y devuelve el uuid de su versión. */
	protected String generarPlan(Cliente cliente) throws Exception {
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("diasPorSemana", 3);
		cuerpo.put("minutosPorSesion", 60);
		cuerpo.put("material", MATERIAL_COMPLETO);
		cuerpo.put("exclusiones", List.of());
		cuerpo.put("prioritarios", List.of("pecho"));
		return cuerpoDe(enviarAutenticado(RUTA_GENERAR, cliente, cuerpo).andExpect(status().isOk())).get("uuid")
				.asString();
	}

	protected JsonNode planActual(Cliente cliente) throws Exception {
		return cuerpoDe(consultar(RUTA_PLAN_ACTUAL, cliente).andExpect(status().isOk()));
	}

	protected static List<Objetivo> objetivos(JsonNode plan) {
		List<Objetivo> objetivos = new ArrayList<>();
		plan.get("dias").forEach(d -> d.get("ejercicios").forEach(e -> objetivos.add(aObjetivo(e))));
		return objetivos;
	}

	protected static Objetivo aObjetivo(JsonNode e) {
		return new Objetivo(e.get("idExterno").asString(), e.get("series").asInt(),
				e.get("repeticionesMin").asInt(), e.get("repeticionesMax").asInt(), e.get("rirObjetivo").asInt(),
				e.get("incrementoKg").asDouble(), e.get("cargaKg"));
	}

	/** El objetivo de un ejercicio con incremento mayor que 0, para poder ver subir la carga. */
	protected Objetivo ejercicioConIncremento(Cliente cliente) throws Exception {
		return objetivos(planActual(cliente)).stream().filter(o -> o.incrementoKg() > 0).findFirst().orElseThrow();
	}

	protected static Map<String, Object> sesion(String planVersion, Instant realizadaEn,
			List<Map<String, Object>> series) {
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("planVersion", planVersion);
		cuerpo.put("diaPlan", 1);
		cuerpo.put("realizadaEn", realizadaEn.toString());
		cuerpo.put("series", series);
		return cuerpo;
	}

	/** Serie de trabajo sintética. */
	protected static Map<String, Object> serie(UUID uuid, String ejercicio, int orden, String pesoKg,
			int repeticiones, Integer rir) {
		Map<String, Object> serie = new LinkedHashMap<>();
		serie.put("uuid", uuid.toString());
		serie.put("ejercicio", ejercicio);
		serie.put("orden", orden);
		serie.put("pesoKg", new java.math.BigDecimal(pesoKg));
		serie.put("repeticiones", repeticiones);
		serie.put("rir", rir);
		serie.put("rpe", null);
		serie.put("calentamiento", false);
		serie.put("dolor", null);
		serie.put("corrigeA", null);
		serie.put("anulada", false);
		serie.put("realizadaEn", Instant.now().toString());
		return serie;
	}

	protected static Map<String, Object> con(Map<String, Object> serie, String campo, Object valor) {
		Map<String, Object> copia = new LinkedHashMap<>(serie);
		copia.put(campo, valor);
		return copia;
	}

	protected ResultActions guardarSesion(Cliente cliente, UUID sesion, Map<String, Object> cuerpo)
			throws Exception {
		return actualizar(RUTA_SESIONES + "/" + sesion, cliente, cuerpo);
	}

	protected long seriesGuardadas(String correo) {
		return contar("""
				SELECT COUNT(*) FROM serie_realizada s JOIN sesion x ON x.uuid = s.sesion_uuid
				JOIN usuario u ON u.id = x.usuario_id WHERE u.correo = ?""", correo);
	}

	protected long sesionesGuardadas(String correo) {
		return contar("SELECT COUNT(*) FROM sesion x JOIN usuario u ON u.id = x.usuario_id WHERE u.correo = ?",
				correo);
	}

	protected List<UUID> vigentesDe(UUID sesion) {
		return jdbc.sql("SELECT uuid FROM serie_vigente WHERE sesion_uuid = :s ORDER BY orden, uuid")
				.param("s", Conversiones.aBytes(sesion)).query((rs, n) -> Conversiones.uuid(rs, "uuid")).list();
	}

	protected JsonNode cargaDe(Cliente cliente, String idExterno) throws Exception {
		return objetivos(planActual(cliente)).stream().filter(o -> o.idExterno().equals(idExterno)).findFirst()
				.orElseThrow().cargaKg();
	}
}
