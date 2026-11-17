package es.forja.sesiones.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import es.forja.entreno.plan.modelo.GrupoMuscular;
import tools.jackson.databind.JsonNode;

/** Músculos, nombre del día, «hoy» y primera sesión en {@code GET /planes/actual}. */
class PlanActualApiTest extends SoporteSesiones {

	private static final Set<String> GRUPOS = Set.of("pecho", "hombros", "brazos", "triceps", "antebrazos",
			"abdomen", "cuadriceps", "aductores", "gemelos", "trapecio", "dorsal", "lumbar", "gluteos", "isquios");
	private static final Set<String> NOMBRES = Set.of("PIERNA", "TORSO", "FULL BODY");

	private static List<JsonNode> ejercicios(JsonNode plan) {
		List<JsonNode> todos = new ArrayList<>();
		plan.get("dias").forEach(d -> d.get("ejercicios").forEach(todos::add));
		return todos;
	}

	private void entrenar(Cliente cliente, String plan, int dia, String ejercicio, long horasAtras) throws Exception {
		var serie = serie(UUID.randomUUID(), ejercicio, 0, "40", 8, 2);
		var cuerpo = con(sesion(plan, reloj.instant().minus(Duration.ofHours(horasAtras)), List.of(serie)), "diaPlan",
				dia);
		guardarSesion(cliente, UUID.randomUUID(), cuerpo).andExpect(status().isOk());
	}

	@Test
	void cadaEjercicioTraeSusMusculosSinRepetirYCadaDiaSuNombre() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");
		generarPlan(cliente);

		JsonNode plan = planActual(cliente);

		for (JsonNode dia : plan.get("dias")) {
			assertThat(dia.get("nombre").asString()).isIn(NOMBRES);
		}
		for (JsonNode ej : ejercicios(plan)) {
			List<String> grupos = new ArrayList<>();
			ej.get("musculos").forEach(m -> {
				grupos.add(m.get("grupo").asString());
				assertThat(m.get("grupo").asString()).isIn(GRUPOS);
				assertThat(m.get("rol").asString()).isIn("primario", "secundario");
			});
			assertThat(grupos).isNotEmpty().doesNotHaveDuplicates();
			assertThat(ej.get("musculos").get(0).get("rol").asString()).isEqualTo("primario");
		}
	}

	@Test
	void sinSesionesTocaElDiaUnoYTodoEsPrimeraSesion() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");
		generarPlan(cliente);

		JsonNode plan = planActual(cliente);

		assertThat(plan.get("diaDeHoy").asInt()).isEqualTo(1);
		assertThat(ejercicios(plan)).allSatisfy(ej -> assertThat(ej.get("primeraSesion").asBoolean()).isTrue());
	}

	@Test
	void despuesDeEntrenarUnDiaTocaElSiguienteYElEjercicioDejaDeSerPrimeraSesion() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");
		String plan = generarPlan(cliente);
		String entrenado = objetivos(planActual(cliente)).get(0).idExterno();

		entrenar(cliente, plan, 1, entrenado, 2);

		JsonNode actual = planActual(cliente);
		assertThat(actual.get("diaDeHoy").asInt()).isEqualTo(2);
		for (JsonNode ej : ejercicios(actual)) {
			assertThat(ej.get("primeraSesion").asBoolean()).isEqualTo(!ej.get("idExterno").asString().equals(entrenado));
		}
	}

	@Test
	void despuesDelUltimoDiaVuelveAlPrimero() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");
		String plan = generarPlan(cliente);
		String ejercicio = objetivos(planActual(cliente)).get(0).idExterno();
		int ultimo = planActual(cliente).get("dias").size();

		entrenar(cliente, plan, ultimo, ejercicio, 1);

		assertThat(planActual(cliente).get("diaDeHoy").asInt()).isEqualTo(1);
	}

	@Test
	void todoMusculoDelCatalogoTieneGrupoEnElCuerpo() {
		List<String> codigos = jdbc.sql("SELECT codigo FROM musculo").query(String.class).list();

		assertThat(codigos).isNotEmpty().allSatisfy(c -> assertThat(GrupoMuscular.delCatalogo(c)).isNotNull());
	}
}
