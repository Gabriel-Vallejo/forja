package es.forja.entreno.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import es.forja.restricciones.bloqueo.ServicioBloqueo;
import es.forja.sesiones.api.SoporteSesiones;
import tools.jackson.databind.JsonNode;

/** Ajustes del plan (PRD v0.1): vista previa sin guardar, aplicar como versión nueva y preferencias al regenerar. */
class AjustesApiTest extends SoporteSesiones {

	private static final String RUTA_VISTA = "/api/v1/planes/actual/ajustes/vista-previa";
	private static final String RUTA_APLICAR = "/api/v1/planes/actual/ajustes";
	private static final String RUTA_PREFERENCIAS = "/api/v1/planes/preferencias";

	/** Genera con los ajustes del perfil y sin prioritarios, que es lo mismo con lo que resuelve la vista previa. */
	private String generarComoElPerfil(Cliente cliente) throws Exception {
		JsonNode perfil = cuerpoDe(consultar("/api/v1/perfil/entreno", cliente).andExpect(status().isOk()));
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("diasPorSemana", perfil.get("diasPorSemana").asInt());
		cuerpo.put("minutosPorSesion", perfil.get("minutosPorSesion").asInt());
		List<String> material = new ArrayList<>();
		perfil.get("material").forEach(m -> material.add(m.asString()));
		cuerpo.put("material", material);
		cuerpo.put("exclusiones", List.of());
		cuerpo.put("prioritarios", List.of());
		return cuerpoDe(enviarAutenticado(RUTA_GENERAR, cliente, cuerpo).andExpect(status().isOk())).get("uuid")
				.asString();
	}

	private static Map<String, Object> ajuste(String tipo, String idExterno) {
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("tipo", tipo);
		cuerpo.put("idExterno", idExterno);
		return cuerpo;
	}

	private ResultActions vistaPrevia(Cliente cliente, Map<String, Object> ajuste) throws Exception {
		return enviarAutenticado(RUTA_VISTA, cliente, ajuste);
	}

	/** Series de la semana por {@code idExterno}, de un plan con {@code dias[].ejercicios[]}. */
	private static Map<String, Integer> seriesSemana(JsonNode plan) {
		Map<String, Integer> series = new TreeMap<>();
		plan.get("dias").forEach(d -> d.get("ejercicios")
				.forEach(e -> series.merge(e.get("idExterno").asString(), e.get("series").asInt(), Integer::sum)));
		return series;
	}

	/** Series por día y {@code idExterno}: el plan entero, no solo la semana. */
	private static Map<Integer, Map<String, Integer>> seriesPorDia(JsonNode plan) {
		Map<Integer, Map<String, Integer>> dias = new TreeMap<>();
		plan.get("dias").forEach(d -> d.get("ejercicios").forEach(e -> dias.computeIfAbsent(d.get("dia").asInt(),
				n -> new TreeMap<>()).put(e.get("idExterno").asString(), e.get("series").asInt())));
		return dias;
	}

	private long versiones(Cliente cliente) {
		return contar("SELECT COUNT(*) FROM plan_version p JOIN usuario u ON u.id = p.usuario_id WHERE u.correo = ?",
				cliente.correo());
	}

	@Test
	void noRecomendarEnLaVistaPreviaNoGuardaYAlAplicarSeGuardaYSeRespetaAlRegenerar() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");
		String version = generarComoElPerfil(cliente);
		String quitado = seriesSemana(planActual(cliente)).keySet().iterator().next();

		JsonNode vista = cuerpoDe(vistaPrevia(cliente, ajuste("no_recomendar", quitado)).andExpect(status().isOk()));

		assertThat(vista.get("version").asString()).isEqualTo(version);
		assertThat(seriesSemana(vista.get("plan"))).doesNotContainKey(quitado);
		assertThat(vista.get("ejercicios").findValuesAsString("idExterno")).contains(quitado);
		assertThat(vista.get("preferencias").get("exclusiones").get(0).asString()).isEqualTo(quitado);
		// La vista previa no guarda nada: ni versión nueva ni preferencias.
		assertThat(versiones(cliente)).isEqualTo(1);
		assertThat(planActual(cliente).get("uuid").asString()).isEqualTo(version);
		consultar(RUTA_PREFERENCIAS, cliente).andExpect(status().isOk())
				.andExpect(jsonPath("$.exclusiones").isEmpty());

		Map<String, Object> aplicar = ajuste("no_recomendar", quitado);
		aplicar.put("version", version);
		String nueva = cuerpoDe(enviarAutenticado(RUTA_APLICAR, cliente, aplicar).andExpect(status().isOk()))
				.get("uuid").asString();

		assertThat(nueva).isNotEqualTo(version);
		JsonNode actual = planActual(cliente);
		assertThat(actual.get("uuid").asString()).isEqualTo(nueva);
		assertThat(seriesSemana(actual)).doesNotContainKey(quitado);
		assertThat(seriesSemana(actual)).isEqualTo(seriesSemana(vista.get("plan")));
		consultar(RUTA_PREFERENCIAS, cliente).andExpect(status().isOk())
				.andExpect(jsonPath("$.exclusiones[0].codigo").value(quitado))
				.andExpect(jsonPath("$.exclusiones[0].nombre").isNotEmpty());

		// «Regenerar» manda exclusiones vacías: la guardada se respeta igual.
		generarComoElPerfil(cliente);
		assertThat(seriesSemana(planActual(cliente))).doesNotContainKey(quitado);
	}

	@Test
	void unaSerieMasSubeLaDelEjercicioYQuedaFijadaAlRegenerar() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");
		String version = generarComoElPerfil(cliente);
		Map<String, Integer> antes = seriesSemana(planActual(cliente));
		String elegido = null;
		JsonNode vista = null;
		for (String id : antes.keySet()) {
			ResultActions r = vistaPrevia(cliente, ajuste("mas", id));
			int estado = r.andReturn().getResponse().getStatus();
			if (estado == 200) {
				elegido = id;
				vista = cuerpoDe(r);
				break;
			}
			// Si no cabe, lo dice con un porqué.
			r.andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.detail").isNotEmpty());
		}

		assertThat(elegido).isNotNull();
		assertThat(seriesSemana(vista.get("plan")).get(elegido)).isEqualTo(antes.get(elegido) + 1);
		assertThat(vista.get("preferencias").get("series").get(elegido).asInt()).isEqualTo(antes.get(elegido) + 1);

		Map<String, Object> aplicar = ajuste("mas", elegido);
		aplicar.put("version", version);
		enviarAutenticado(RUTA_APLICAR, cliente, aplicar).andExpect(status().isOk());
		generarComoElPerfil(cliente);

		assertThat(seriesSemana(planActual(cliente)).get(elegido)).isEqualTo(antes.get(elegido) + 1);
		consultar(RUTA_PREFERENCIAS, cliente).andExpect(status().isOk())
				.andExpect(jsonPath("$.series[0].idExterno").value(elegido))
				.andExpect(jsonPath("$.series[0].series").value(antes.get(elegido) + 1));
	}

	@Test
	void unaSerieMenosLaBajaYEnElMinimoExplicaPorQueNo() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");
		String version = generarComoElPerfil(cliente);
		Map<String, Integer> antes = seriesSemana(planActual(cliente));
		String conMargen = antes.entrySet().stream().filter(e -> e.getValue() > 2).findFirst().orElseThrow().getKey();

		JsonNode vista = cuerpoDe(vistaPrevia(cliente, ajuste("menos", conMargen)).andExpect(status().isOk()));

		assertThat(seriesSemana(vista.get("plan")).get(conMargen)).isEqualTo(antes.get(conMargen) - 1);
		assertThat(vista.get("ejercicios").findValuesAsString("idExterno")).contains(conMargen);
		// Parte del plan actual: nada entra ni sale y, como mucho, dos compensaciones de series.
		assertThat(vista.get("ejercicios")).hasSizeLessThanOrEqualTo(3)
				.allSatisfy(c -> assertThat(c.get("antes").asInt() * c.get("despues").asInt()).isPositive());

		// Aplicar vuelve a resolver y, con la pista y la penalización, sale el plan de la vista previa, día a día.
		Map<String, Object> aplicar = ajuste("menos", conMargen);
		aplicar.put("version", version);
		enviarAutenticado(RUTA_APLICAR, cliente, aplicar).andExpect(status().isOk());
		assertThat(seriesPorDia(planActual(cliente))).isEqualTo(seriesPorDia(vista.get("plan")));

		antes.entrySet().stream().filter(e -> e.getValue() == 2).findFirst().ifPresent(e -> {
			try {
				vistaPrevia(cliente, ajuste("menos", e.getKey())).andExpect(status().isUnprocessableContent())
						.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("No recomendar")));
			} catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		});
	}

	@Test
	void masVariedadCambiaEjerciciosDelPlanActual() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");
		generarComoElPerfil(cliente);
		Map<String, Integer> antes = seriesSemana(planActual(cliente));

		JsonNode vista = cuerpoDe(vistaPrevia(cliente, ajuste("variedad", null)).andExpect(status().isOk()));

		long repetidos = seriesSemana(vista.get("plan")).keySet().stream().filter(antes::containsKey).count();
		assertThat(repetidos).isLessThan(antes.size());
		assertThat(vista.get("preferencias").get("variedad").asBoolean()).isTrue();
	}

	@Test
	void enfocarSubeLasSeriesDelMusculo() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");
		generarComoElPerfil(cliente);
		Map<String, Object> enfocar = ajuste("enfocar", null);
		enfocar.put("musculos", List.of("biceps"));

		JsonNode vista = cuerpoDe(vistaPrevia(cliente, enfocar).andExpect(status().isOk()));

		JsonNode biceps = null;
		for (JsonNode m : vista.get("musculos")) {
			if (m.get("musculo").asString().equals("biceps")) {
				biceps = m;
			}
		}
		assertThat(biceps).isNotNull();
		assertThat(biceps.get("despues").asDouble()).isGreaterThan(biceps.get("antes").asDouble());
		assertThat(vista.get("preferencias").get("prioritarios").get(0).asString()).isEqualTo("biceps");
	}

	@Test
	void conBanderaRojaLaVistaPreviaDa409() throws Exception {
		Cliente cliente = clienteConLimitaciones();
		generarComoElPerfil(cliente);
		Map<String, Object> banderas = new LinkedHashMap<>();
		banderas.put("dolorPorZona", Map.of());
		for (String senal : List.of("dolorEnReposo", "dolorNocturnoQueDespierta", "hormigueoOEntumecimiento",
				"perdidaDeFuerza", "perdidaControlEsfinteresOAnestesiaSillaDeMontar", "fiebreConDolorDeEspalda",
				"traumatismoReciente", "perdidaDePesoSinExplicacion", "dolorToracico", "mareoODesmayo")) {
			banderas.put(senal, senal.equals("dolorToracico"));
		}
		enviarAutenticado("/api/v1/banderas", cliente, banderas).andExpect(status().isOk());

		vistaPrevia(cliente, ajuste("variedad", null)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value(ServicioBloqueo.CODIGO_ERROR));
	}

	@Test
	void aplicarSobreUnPlanQueYaHaCambiadoDa409YSinVersion400() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");
		generarComoElPerfil(cliente);
		Map<String, Object> viejo = ajuste("variedad", null);
		viejo.put("version", UUID.randomUUID().toString());

		enviarAutenticado(RUTA_APLICAR, cliente, viejo).andExpect(status().isConflict());
		enviarAutenticado(RUTA_APLICAR, cliente, ajuste("variedad", null)).andExpect(status().isBadRequest());
		assertThat(versiones(cliente)).isEqualTo(1);
	}

	@Test
	void validaElAjusteYLosTopes() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");
		consultar(RUTA_PREFERENCIAS, cliente).andExpect(status().isOk());
		vistaPrevia(cliente, ajuste("variedad", null)).andExpect(status().isNotFound());
		generarComoElPerfil(cliente);
		Map<String, Integer> plan = seriesSemana(planActual(cliente));
		List<String> fuera = new ArrayList<>();
		rutinasDelCatalogo().stream().filter(id -> !plan.containsKey(id)).limit(4).forEach(fuera::add);

		vistaPrevia(cliente, ajuste("no_recomendar", "No_Existe")).andExpect(status().isNotFound());
		vistaPrevia(cliente, ajuste("inventado", null)).andExpect(status().isBadRequest());
		Map<String, Object> tresMusculos = ajuste("enfocar", null);
		tresMusculos.put("musculos", List.of("pecho", "biceps", "triceps"));
		vistaPrevia(cliente, tresMusculos).andExpect(status().isBadRequest());

		actualizar(RUTA_PREFERENCIAS, cliente, preferencias(fuera, List.of("pecho"), Map.of(), true))
				.andExpect(status().isOk()).andExpect(jsonPath("$.exclusiones.length()").value(4))
				.andExpect(jsonPath("$.prioritarios[0].codigo").value("pecho"))
				.andExpect(jsonPath("$.variedad").value(true));
		// Con 4 quitados no cabe un quinto, y se explica.
		vistaPrevia(cliente, ajuste("no_recomendar", plan.keySet().iterator().next()))
				.andExpect(status().isUnprocessableContent());
		actualizar(RUTA_PREFERENCIAS, cliente, preferencias(List.of(), List.of("no_existe"), Map.of(), false))
				.andExpect(status().isBadRequest());
		actualizar(RUTA_PREFERENCIAS, cliente, preferencias(List.of(fuera.get(0)), List.of(),
				Map.of(fuera.get(0), 3), false)).andExpect(status().isBadRequest());
		actualizar(RUTA_PREFERENCIAS, cliente, preferencias(List.of(), List.of(), Map.of(fuera.get(0), 1), false))
				.andExpect(status().isBadRequest());
		// Quitar una preferencia es mandar el resto.
		actualizar(RUTA_PREFERENCIAS, cliente, preferencias(fuera.subList(1, 4), List.of(), Map.of(), false))
				.andExpect(status().isOk()).andExpect(jsonPath("$.exclusiones.length()").value(3))
				.andExpect(jsonPath("$.prioritarios").isEmpty());
	}

	private List<String> rutinasDelCatalogo() {
		return jdbc.sql("SELECT id_externo FROM ejercicio WHERE origen_etiquetado = 'curado' ORDER BY id_externo")
				.query(String.class).list();
	}

	private static Map<String, Object> preferencias(List<String> exclusiones, List<String> prioritarios,
			Map<String, Integer> series, boolean variedad) {
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("exclusiones", exclusiones);
		cuerpo.put("prioritarios", prioritarios);
		cuerpo.put("series", series);
		cuerpo.put("variedad", variedad);
		return cuerpo;
	}
}
