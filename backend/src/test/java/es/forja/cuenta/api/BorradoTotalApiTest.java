package es.forja.cuenta.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import es.forja.restricciones.api.SoporteLimitaciones;

/** Criterio de la v0.1 (PRD, ADR 0004 §6): al borrar la cuenta no queda ninguna fila suya en ninguna tabla. */
class BorradoTotalApiTest extends SoporteLimitaciones {

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
		long usuarioId = contar("SELECT id FROM usuario WHERE correo = ?", correo);
		List<Long> dispositivos = jdbc.sql("SELECT id FROM dispositivo WHERE usuario_id = :u").param("u", usuarioId)
				.query(Long.class).list();
		Map<String, Long> antes = filasDelUsuario(usuarioId, dispositivos);
		assertThat(antes).containsKeys("usuario", "dispositivo", "token_refresco", "token_un_uso", "consentimiento",
				"restriccion");
		assertThat(antes.get("consentimiento")).isEqualTo(CONSENTIMIENTOS.size());
		assertThat(antes.get("restriccion")).isEqualTo(2);
		assertThat(antes.values()).allSatisfy(n -> assertThat(n).isPositive());

		mvc.perform(delete("/api/v1/cuenta").header("Authorization", movil.cabecera())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("contrasena", CONTRASENA)))).andExpect(status().isNoContent());

		Map<String, Long> despues = filasDelUsuario(usuarioId, dispositivos);
		assertThat(despues.keySet()).isEqualTo(antes.keySet());
		assertThat(despues.values()).allSatisfy(n -> assertThat(n).isZero());
	}
}
