package es.forja.exportacion.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

import es.forja.exportacion.servicio.ServicioExportacion;
import es.forja.sesiones.api.SoporteSesiones;
import tools.jackson.databind.JsonNode;

/** «Descargar mis datos» (RGPD, arts. 15 y 20): todo lo del usuario, descifrado, y nada de nadie más. */
class ExportacionApiTest extends SoporteSesiones {

	private static final String RUTA = "/api/v1/cuenta/datos";

	/**
	 * Dónde sale cada tabla con datos del usuario (puntero JSON dentro de la exportación). Si añades una tabla que
	 * cuelga de {@code usuario}, el test de cobertura falla hasta que la pongas aquí y en la exportación.
	 */
	private static final Map<String, String> SECCIONES = Map.ofEntries(
			Map.entry("usuario", "/cuenta"),
			Map.entry("dispositivo", "/dispositivos"),
			Map.entry("consentimiento", "/consentimientos"),
			Map.entry("perfil_datos", "/perfilDatos"),
			Map.entry("perfil_medidas", "/medidas"),
			Map.entry("perfil_entreno", "/perfilEntreno"),
			Map.entry("perfil_entreno_material", "/perfilEntreno/material"),
			Map.entry("test_movilidad", "/movilidad/tobillo"),
			Map.entry("restriccion", "/limitaciones"),
			Map.entry("bloqueo_salud", "/bloqueos"),
			Map.entry("preferencia_plan", "/preferenciasPlan"),
			Map.entry("preferencia_ejercicio", "/preferenciasPlan/exclusiones"),
			Map.entry("preferencia_musculo", "/preferenciasPlan/prioritarios"),
			Map.entry("plan_version", "/planes"),
			Map.entry("ejercicio_plan", "/planes/0/ejercicios"),
			Map.entry("sesion", "/sesiones"),
			Map.entry("serie_realizada", "/sesiones/0/series"));

	/** Hashes de secretos: no son datos que el usuario pueda usar y exportarlos solo añade riesgo. */
	private static final Set<String> SIN_EXPORTAR = Set.of("token_refresco", "token_un_uso");

	/** Las tablas que cuelgan de {@code usuario} por claves foráneas, directa o indirectamente, y la propia. */
	private Set<String> tablasDelUsuario() {
		Set<String> tablas = new LinkedHashSet<>(List.of("usuario"));
		Deque<String> pendientes = new ArrayDeque<>(tablas);
		while (!pendientes.isEmpty()) {
			for (String hija : jdbc.sql("""
					SELECT DISTINCT table_name FROM information_schema.key_column_usage
					WHERE table_schema = DATABASE() AND referenced_table_name = :tabla""")
					.param("tabla", pendientes.pop()).query(String.class).list()) {
				if (tablas.add(hija)) {
					pendientes.push(hija);
				}
			}
		}
		return tablas;
	}

	private ResultActions exportar(Cliente cliente) throws Exception {
		return mvc.perform(get(RUTA).header("Authorization", cliente.cabecera()));
	}

	@Test
	void cadaTablaConDatosDelUsuarioTieneSuSeccionEnLaExportacion() {
		Set<String> esperadas = new LinkedHashSet<>(SECCIONES.keySet());
		esperadas.addAll(SIN_EXPORTAR);
		assertThat(tablasDelUsuario()).containsExactlyInAnyOrderElementsOf(esperadas);
	}

	@Test
	void exportaTodasLasSeccionesDescifradasYNadaDeOtroUsuario() throws Exception {
		Cliente yo = cuentaConSesion("Móvil");
		Rellenado mio = rellenarTodasLasTablas(yo);
		// Otra sesión con una corrección de la serie con dolor y la anulación de esa corrección: el historial también
		// se exporta, no solo las series vigentes.
		UUID correccion = UUID.randomUUID();
		UUID anulacion = UUID.randomUUID();
		guardarSesion(yo, UUID.randomUUID(), sesion(mio.plan(), Instant.now().minusSeconds(30), List.of(
				con(serie(correccion, mio.objetivo().idExterno(), 0, "52.5", 8, 2), "corrigeA", mio.serie().toString()),
				con(con(serie(anulacion, mio.objetivo().idExterno(), 0, "52.5", 8, 2), "corrigeA",
						correccion.toString()), "anulada", true))))
				.andExpect(status().isOk());
		// Un consentimiento retirado sale con su fecha de retirada.
		borrarRecurso(RUTA_CONSENTIMIENTOS + "/suplementos", yo).andExpect(status().isNoContent());
		Cliente otro = clienteConLimitaciones();
		String restriccionAjena = crearRestriccion(otro, restriccion("codo", null, "sin_carga_axial"));

		ResultActions respuesta = exportar(yo).andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"forja-mis-datos-"
						+ LocalDate.now(ZoneOffset.UTC) + ".json\""))
				.andExpect(jsonPath("$.formato").value(ServicioExportacion.FORMATO))
				.andExpect(jsonPath("$.generadoEn").isNotEmpty())
				.andExpect(jsonPath("$.cuenta.correo").value(yo.correo()))
				.andExpect(jsonPath("$.cuenta.creadoEn").isNotEmpty());
		JsonNode datos = cuerpoDe(respuesta);

		SECCIONES.forEach((tabla, puntero) -> {
			JsonNode seccion = datos.at(puntero);
			assertThat(seccion.isMissingNode() || seccion.isNull() || seccion.isContainer() && seccion.isEmpty())
					.as("sección vacía: " + tabla + " → " + puntero).isFalse();
		});
		// Datos de salud descifrados.
		assertThat(datos.at("/limitaciones").findValuesAsString("region")).containsExactlyInAnyOrder("rodilla", "hombro");
		assertThat(datos.at("/medidas/pesoKg").asDouble()).isEqualTo(78.5);
		assertThat(datos.at("/movilidad/tobillo/tramo").asString()).isEqualTo("limitado");
		assertThat(datos.at("/bloqueos/0/origen").asString()).isEqualTo("banderas");
		assertThat(datos.at("/bloqueos/0/nivel").asString()).isNotBlank();
		assertThat(datos.at("/sesiones")).hasSize(2);
		List<JsonNode> series = new ArrayList<>();
		datos.at("/sesiones").forEach(x -> x.get("series").forEach(series::add));
		assertThat(series).hasSize(3);
		assertThat(serie(series, mio.serie()).get("dolor").asInt()).isEqualTo(3);
		assertThat(serie(series, correccion).get("corrigeA").asString()).isEqualTo(mio.serie().toString());
		assertThat(serie(series, anulacion).get("anulada").asBoolean()).isTrue();
		assertThat(datos.at("/consentimientos").findValues("revocadoEn")).anyMatch(f -> !f.isNull());
		assertThat(datos.at("/planes/0/actual").asBoolean()).isTrue();
		// Solo identificadores públicos y nada de secretos ni del otro usuario.
		assertThat(nombresDeCampo(datos)).noneMatch(c -> c.equals("id") || c.endsWith("Id") || c.contains("hash"));
		String texto = datos.toString();
		assertThat(texto).doesNotContain(otro.correo(), restriccionAjena, CONTRASENA);
	}

	@Test
	void sinDatosDeSaludNiPlanLasSeccionesSalenVacias() throws Exception {
		Cliente cliente = cuentaConSesion("PC");

		JsonNode datos = cuerpoDe(exportar(cliente).andExpect(status().isOk()));

		assertThat(datos.at("/limitaciones")).isEmpty();
		assertThat(datos.at("/bloqueos")).isEmpty();
		assertThat(datos.at("/planes")).isEmpty();
		assertThat(datos.at("/sesiones")).isEmpty();
		assertThat(datos.at("/medidas").isNull()).isTrue();
		assertThat(datos.at("/movilidad").isNull()).isTrue();
		assertThat(datos.at("/dispositivos")).hasSize(1);
	}

	@Test
	void sinTokenEs401() throws Exception {
		mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
	}

	@Test
	void pasadoElLimitePorUsuarioEs429ConCodigoYRetryAfter() throws Exception {
		Cliente cliente = cuentaConSesion("Móvil");
		for (int i = 0; i < ServicioExportacion.EXPORTACIONES_POR_VENTANA; i++) {
			exportar(cliente).andExpect(status().isOk());
		}

		exportar(cliente).andExpect(status().isTooManyRequests()).andExpect(header().exists(HttpHeaders.RETRY_AFTER))
				.andExpect(jsonPath("$.codigo").value("demasiadas_peticiones"));
		// El límite es por usuario: otro sigue pudiendo.
		exportar(cuentaConSesion("PC")).andExpect(status().isOk());
	}

	private static JsonNode serie(List<JsonNode> series, UUID uuid) {
		for (JsonNode s : series) {
			if (s.get("uuid").asString().equals(uuid.toString())) {
				return s;
			}
		}
		throw new AssertionError("No está la serie " + uuid);
	}

	private static List<String> nombresDeCampo(JsonNode nodo) {
		List<String> nombres = new ArrayList<>();
		if (nodo.isObject()) {
			nodo.properties().forEach(e -> {
				nombres.add(e.getKey());
				nombres.addAll(nombresDeCampo(e.getValue()));
			});
		} else if (nodo.isArray()) {
			nodo.forEach(hijo -> nombres.addAll(nombresDeCampo(hijo)));
		}
		return nombres;
	}
}
