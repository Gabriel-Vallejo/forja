package es.forja.cuenta.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import es.forja.sesiones.api.SoporteSesiones;

/** Criterio de la v0.1 (PRD, ADR 0004 §6): al borrar la cuenta no queda ninguna fila suya en ninguna tabla. */
class BorradoTotalApiTest extends SoporteSesiones {

	private static final List<String> CONSENTIMIENTOS = List.of("fotos_cuerpo", "medidas_salud", "lesiones",
			"limitaciones", "suplementos");

	private List<String> tablasConColumna(String columna) {
		return jdbc.sql("""
				SELECT DISTINCT table_name FROM information_schema.columns
				WHERE table_schema = DATABASE() AND column_name = :columna""").param("columna", columna)
				.query(String.class).list();
	}

	/** Filas del usuario en cada tabla con {@code usuario_id}, y en las que cuelgan de sus dispositivos. */
	private Map<String, Long> filasDelUsuario(long usuarioId, List<Long> dispositivos) {
		Map<String, Long> filas = new HashMap<>();
		for (String tabla : tablasConColumna("usuario_id")) {
			filas.put(tabla, contar("SELECT COUNT(*) FROM " + tabla + " WHERE usuario_id = ?", usuarioId));
		}
		for (String tabla : tablasConColumna("dispositivo_id")) {
			long total = 0;
			for (long dispositivo : dispositivos) {
				total += contar("SELECT COUNT(*) FROM " + tabla + " WHERE dispositivo_id = ?", dispositivo);
			}
			filas.put(tabla, total);
		}
		filas.put("usuario", contar("SELECT COUNT(*) FROM usuario WHERE id = ?", usuarioId));
		return filas;
	}

	private static Map<String, Object> banderasConDolorToracico() {
		Map<String, Object> cuerpo = new java.util.LinkedHashMap<>();
		cuerpo.put("dolorPorZona", Map.of());
		for (String senal : List.of("dolorEnReposo", "dolorNocturnoQueDespierta", "hormigueoOEntumecimiento",
				"perdidaDeFuerza", "perdidaControlEsfinteresOAnestesiaSillaDeMontar", "fiebreConDolorDeEspalda",
				"traumatismoReciente", "perdidaDePesoSinExplicacion", "dolorToracico", "mareoODesmayo")) {
			cuerpo.put(senal, senal.equals("dolorToracico"));
		}
		return cuerpo;
	}

	@Test
	void borrarLaCuentaNoDejaNingunaFilaSuyaEnNingunaTabla() throws Exception {
		String correo = crearCuenta();
		Cliente movil = iniciarSesion(correo, "Móvil", null);
		iniciarSesion(correo, "PC", null);
		enviar("/api/v1/auth/recuperar", Map.of("correo", correo));
		for (String tipo : CONSENTIMIENTOS) {
			darConsentimiento(movil, tipo).andExpect(status().isOk());
		}
		crearRestriccion(movil, restriccion("rodilla", "sentadilla", "sin_carga_axial"));
		crearRestriccion(movil, restriccion("hombro", "empuje_vertical", "sin_por_encima_cabeza"));
		// Plan versionado y una sesión con una serie con dolor cifrado: las tablas de la v4 también se vacían.
		String plan = generarPlan(movil);
		Objetivo objetivo = ejercicioConIncremento(movil);
		Map<String, Object> serie = serie(java.util.UUID.randomUUID(), objetivo.idExterno(), 0, "50", 8, 2);
		serie.put("dolor", 3);
		guardarSesion(movil, java.util.UUID.randomUUID(),
				sesion(plan, java.time.Instant.now().minusSeconds(60), List.of(serie))).andExpect(status().isOk());
		// Después de generar el plan: con bandera roja activa ya no se genera.
		// Bandera roja: deja un bloqueo guardado, que también se borra con la cuenta.
		enviarAutenticado("/api/v1/banderas", movil, banderasConDolorToracico()).andExpect(status().isOk());
		long usuarioId = contar("SELECT id FROM usuario WHERE correo = ?", correo);
		List<Long> dispositivos = jdbc.sql("SELECT id FROM dispositivo WHERE usuario_id = :u").param("u", usuarioId)
				.query(Long.class).list();
		Map<String, Long> antes = filasDelUsuario(usuarioId, dispositivos);
		assertThat(antes).containsKeys("usuario", "dispositivo", "token_refresco", "token_un_uso", "consentimiento",
				"restriccion", "bloqueo_salud");
		assertThat(antes.get("consentimiento")).isEqualTo(CONSENTIMIENTOS.size());
		assertThat(antes.get("restriccion")).isEqualTo(2);
		assertThat(antes).containsKeys("plan_version", "sesion");
		assertThat(seriesGuardadas(correo)).isEqualTo(1);
		assertThat(contar("SELECT COUNT(*) FROM ejercicio_plan ep JOIN plan_version p ON p.id = ep.plan_version_id "
				+ "WHERE p.usuario_id = ?", usuarioId)).isPositive();
		assertThat(antes.values()).allSatisfy(n -> assertThat(n).isPositive());

		mvc.perform(delete("/api/v1/cuenta").header("Authorization", movil.cabecera())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("contrasena", CONTRASENA)))).andExpect(status().isNoContent());

		Map<String, Long> despues = filasDelUsuario(usuarioId, dispositivos);
		assertThat(despues.keySet()).isEqualTo(antes.keySet());
		assertThat(despues.values()).allSatisfy(n -> assertThat(n).isZero());
		// Las tablas sin usuario_id propio (cuelgan de sesion y plan_version) quedan sin filas de ese usuario.
		assertThat(contar("SELECT COUNT(*) FROM serie_realizada s LEFT JOIN sesion x ON x.uuid = s.sesion_uuid "
				+ "WHERE x.uuid IS NULL")).isZero();
		assertThat(contar("SELECT COUNT(*) FROM ejercicio_plan ep LEFT JOIN plan_version p "
				+ "ON p.id = ep.plan_version_id WHERE p.id IS NULL")).isZero();
	}
}
