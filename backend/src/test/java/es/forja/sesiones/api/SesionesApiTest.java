package es.forja.sesiones.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import es.forja.comun.cifrado.Cifrador;
import es.forja.comun.repositorio.Conversiones;
import tools.jackson.databind.JsonNode;

/** PUT /sesiones/{uuid}, GET /sesiones y la progresión en el servidor (ADR 0008). */
class SesionesApiTest extends SoporteSesiones {

	@Autowired
	private Cifrador cifrador;

	private Instant haceHoras(long horas) {
		return reloj.instant().minus(Duration.ofHours(horas));
	}

	private String texto(ResultActions respuesta) throws Exception {
		return respuesta.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
	}

	/** Plan generado y el primer ejercicio con incremento, para entrenarlo. */
	private record Escenario(Cliente cliente, String plan, Objetivo objetivo) {
	}

	private Escenario escenario() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");
		String plan = generarPlan(cliente);
		return new Escenario(cliente, plan, ejercicioConIncremento(cliente));
	}

	private List<Map<String, Object>> seriesDeTrabajo(Objetivo o, String peso, int repeticiones, Integer rir) {
		return java.util.stream.IntStream.range(0, o.series())
				.mapToObj(i -> serie(UUID.randomUUID(), o.idExterno(), i, peso, repeticiones, rir)).toList();
	}

	// ---- idempotencia y «solo se añade» ----

	@Test
	void reenviarDosVecesLaMismaSesionNoDuplicaNiCambiaLaRespuesta() throws Exception {
		Escenario e = escenario();
		UUID sesion = UUID.randomUUID();
		Map<String, Object> cuerpo = sesion(e.plan(), haceHoras(1), seriesDeTrabajo(e.objetivo(), "50", 8, 2));

		String primera = texto(guardarSesion(e.cliente(), sesion, cuerpo).andExpect(status().isOk()));
		String segunda = texto(guardarSesion(e.cliente(), sesion, cuerpo).andExpect(status().isOk()));

		assertThat(segunda).isEqualTo(primera);
		assertThat(sesionesGuardadas(e.cliente().correo())).isEqualTo(1);
		assertThat(seriesGuardadas(e.cliente().correo())).isEqualTo(e.objetivo().series());
	}

	@Test
	void registrarCorregirUnaSerieYAnularOtraDejaEnLaVistaSoloLoValido() throws Exception {
		Escenario e = escenario();
		Instant cuando = haceHoras(1);
		UUID sesion = UUID.randomUUID();
		UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
		Map<String, Object> sa = serie(a, e.objetivo().idExterno(), 0, "50", 8, 2);
		Map<String, Object> sb = serie(b, e.objetivo().idExterno(), 1, "50", 8, 2);
		Map<String, Object> sc = serie(c, e.objetivo().idExterno(), 2, "50", 8, 2);
		guardarSesion(e.cliente(), sesion, sesion(e.plan(), cuando, List.of(sa, sb, sc)))
				.andExpect(status().isOk());

		UUID a2 = UUID.randomUUID(), anulaB = UUID.randomUUID();
		Map<String, Object> correccion = con(con(serie(a2, e.objetivo().idExterno(), 3, "55", 8, 2), "corrigeA", a.toString()), "orden", 0);
		Map<String, Object> anulacion = con(con(serie(anulaB, e.objetivo().idExterno(), 4, "50", 8, 2),
				"corrigeA", b.toString()), "anulada", true);
		guardarSesion(e.cliente(), sesion, sesion(e.plan(), cuando, List.of(sa, sb, sc, correccion, anulacion)))
				.andExpect(status().isOk());

		assertThat(vigentesDe(sesion)).containsExactlyInAnyOrder(a2, c);
		assertThat(seriesGuardadas(e.cliente().correo())).isEqualTo(5);
		consultar(RUTA_SESIONES, e.cliente()).andExpect(status().isOk())
				.andExpect(jsonPath("$.sesiones[0].series.length()").value(2));
	}

	@Test
	void unaSerieExistenteConContenidoDistintoDa409YNoCambiaNada() throws Exception {
		Escenario e = escenario();
		Instant cuando = haceHoras(1);
		UUID sesion = UUID.randomUUID();
		UUID uuidOriginal = UUID.randomUUID();
		Map<String, Object> original = serie(uuidOriginal, e.objetivo().idExterno(), 0, "50", 8, 2);
		guardarSesion(e.cliente(), sesion, sesion(e.plan(), cuando, List.of(original)))
				.andExpect(status().isOk());

		guardarSesion(e.cliente(), sesion, sesion(e.plan(), cuando, List.of(con(original, "pesoKg", new BigDecimal("60")))))
				.andExpect(status().isConflict());

		assertThat(contar("SELECT peso_kg FROM serie_realizada WHERE uuid = ?", (Object) Conversiones.aBytes(uuidOriginal)))
				.isEqualTo(50);
	}

	@Test
	void unReenvioSinUnaSerieQueYaEstabaNoLaBorra() throws Exception {
		Escenario e = escenario();
		Instant cuando = haceHoras(1);
		UUID sesion = UUID.randomUUID();
		Map<String, Object> a = serie(UUID.randomUUID(), e.objetivo().idExterno(), 0, "50", 8, 2);
		Map<String, Object> b = serie(UUID.randomUUID(), e.objetivo().idExterno(), 1, "50", 8, 2);
		guardarSesion(e.cliente(), sesion, sesion(e.plan(), cuando, List.of(a, b))).andExpect(status().isOk());

		guardarSesion(e.cliente(), sesion, sesion(e.plan(), cuando, List.of(a))).andExpect(status().isOk());

		assertThat(seriesGuardadas(e.cliente().correo())).isEqualTo(2);
	}

	@Test
	void unaSegundaCorreccionDeLaMismaSerieDa409() throws Exception {
		Escenario e = escenario();
		Instant cuando = haceHoras(1);
		UUID sesion = UUID.randomUUID();
		UUID a = UUID.randomUUID();
		Map<String, Object> sa = serie(a, e.objetivo().idExterno(), 0, "50", 8, 2);
		Map<String, Object> c1 = con(serie(UUID.randomUUID(), e.objetivo().idExterno(), 1, "51", 8, 2), "corrigeA", a.toString());
		Map<String, Object> c2 = con(serie(UUID.randomUUID(), e.objetivo().idExterno(), 2, "52", 8, 2), "corrigeA", a.toString());
		guardarSesion(e.cliente(), sesion, sesion(e.plan(), cuando, List.of(sa, c1))).andExpect(status().isOk());

		guardarSesion(e.cliente(), sesion, sesion(e.plan(), cuando, List.of(sa, c1, c2)))
				.andExpect(status().isConflict());
	}

	// ---- plan versionado ----

	@Test
	void unaSesionDeUnaVersionAntiguaDelPlanSeAceptaYCuentaParaLaProgresion() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");
		String antiguo = generarPlan(cliente);
		String nuevo = generarPlan(cliente);
		Objetivo o = ejercicioConIncremento(cliente);
		assertThat(nuevo).isNotEqualTo(antiguo);

		guardarSesion(cliente, UUID.randomUUID(),
				sesion(antiguo, haceHoras(1), seriesDeTrabajo(o, "50", o.repeticionesMax(), o.rirObjetivo())))
						.andExpect(status().isOk());

		assertThat(cargaDe(cliente, o.idExterno()).decimalValue())
				.isEqualByComparingTo(BigDecimal.valueOf(50 + o.incrementoKg()));
	}

	@Test
	void regenerarElPlanNoCambiaLaVersionDeLasSesionesYaRegistradas() throws Exception {
		Escenario e = escenario();
		UUID sesion = UUID.randomUUID();
		guardarSesion(e.cliente(), sesion, sesion(e.plan(), haceHoras(1), seriesDeTrabajo(e.objetivo(), "50", 8, 2)))
				.andExpect(status().isOk());

		String nuevo = generarPlan(e.cliente());

		String versionDeLaSesion = jdbc.sql("""
				SELECT LOWER(CONCAT_WS('-', HEX(SUBSTR(p.uuid,1,4)), HEX(SUBSTR(p.uuid,5,2)), HEX(SUBSTR(p.uuid,7,2)),
				HEX(SUBSTR(p.uuid,9,2)), HEX(SUBSTR(p.uuid,11,6)))) FROM sesion x JOIN plan_version p
				ON p.id = x.plan_version_id WHERE x.uuid = :s""").param("s", Conversiones.aBytes(sesion))
				.query(String.class).single();
		assertThat(versionDeLaSesion).isEqualTo(e.plan());
		assertThat(planActual(e.cliente()).get("uuid").asString()).isEqualTo(nuevo);
	}

	@Test
	void unaVersionDePlanDeOtroUsuarioDa422() throws Exception {
		Escenario e = escenario();
		Escenario otro = escenario();

		guardarSesion(e.cliente(), UUID.randomUUID(),
				sesion(otro.plan(), haceHoras(1), seriesDeTrabajo(e.objetivo(), "50", 8, 2)))
						.andExpect(status().isUnprocessableContent());
	}

	@Test
	void sePuedeEntrenarSinPlan() throws Exception {
		Escenario e = escenario();

		guardarSesion(e.cliente(), UUID.randomUUID(),
				sesion(null, haceHoras(1), seriesDeTrabajo(e.objetivo(), "50", 8, 2))).andExpect(status().isOk());
	}

	@Test
	void elPlanActualEsElUltimoYLlevaLosObjetivosPorDefectoDeApplicationYml() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");
		consultar(RUTA_PLAN_ACTUAL, cliente).andExpect(status().isNotFound());
		generarPlan(cliente);
		String ultimo = generarPlan(cliente);

		JsonNode plan = planActual(cliente);

		assertThat(plan.get("uuid").asString()).isEqualTo(ultimo);
		List<Objetivo> objetivos = objetivos(plan);
		assertThat(objetivos).isNotEmpty().allSatisfy(o -> {
			boolean compuesto = o.repeticionesMax() == 10 && o.repeticionesMin() == 6 && o.rirObjetivo() == 2;
			boolean aislado = o.repeticionesMax() == 15 && o.repeticionesMin() == 10 && o.rirObjetivo() == 1;
			assertThat(compuesto || aislado).isTrue();
			assertThat(o.incrementoKg()).isIn(0.0, 2.0, 2.5, 5.0);
			assertThat(o.series()).isPositive();
		});
		assertThat(objetivos).anySatisfy(o -> assertThat(o.incrementoKg()).isEqualTo(2.5));
		assertThat(objetivos).anySatisfy(o -> assertThat(o.cargaKg().isNull()).isTrue());
	}

	@Test
	void laCargaInicialDelPlanNuevoEsLaDeLaUltimaSesionDeEseEjercicio() throws Exception {
		Escenario e = escenario();
		guardarSesion(e.cliente(), UUID.randomUUID(), sesion(e.plan(), haceHoras(1),
				seriesDeTrabajo(e.objetivo(), "47.5", 6, 3))).andExpect(status().isOk());

		generarPlan(e.cliente());

		assertThat(cargaDe(e.cliente(), e.objetivo().idExterno()).decimalValue()).isEqualByComparingTo("47.5");
		String guardada = jdbc.sql("""
				SELECT ep.carga_kg FROM ejercicio_plan ep JOIN plan_version p ON p.id = ep.plan_version_id
				JOIN ejercicio j ON j.id = ep.ejercicio_id JOIN usuario u ON u.id = p.usuario_id
				WHERE u.correo = :c AND j.id_externo = :e ORDER BY p.id DESC LIMIT 1""")
				.param("c", e.cliente().correo()).param("e", e.objetivo().idExterno()).query(String.class).single();
		assertThat(new BigDecimal(guardada)).isEqualByComparingTo("47.5");
	}

	// ---- progresión ----

	@Test
	void laCargaSubeElIncrementoSiSeCumpleElRangoAltoConElRirObjetivo() throws Exception {
		Escenario e = escenario();
		Objetivo o = e.objetivo();

		guardarSesion(e.cliente(), UUID.randomUUID(),
				sesion(e.plan(), haceHoras(1), seriesDeTrabajo(o, "50", o.repeticionesMax(), o.rirObjetivo())))
						.andExpect(status().isOk());

		assertThat(cargaDe(e.cliente(), o.idExterno()).decimalValue())
				.isEqualByComparingTo(BigDecimal.valueOf(50 + o.incrementoKg()));
	}

	@Test
	void laCargaSeRepiteSiNoSeLlegaAlRangoAlto() throws Exception {
		Escenario e = escenario();
		Objetivo o = e.objetivo();

		guardarSesion(e.cliente(), UUID.randomUUID(),
				sesion(e.plan(), haceHoras(1), seriesDeTrabajo(o, "50", o.repeticionesMax() - 1, o.rirObjetivo())))
						.andExpect(status().isOk());

		assertThat(cargaDe(e.cliente(), o.idExterno()).decimalValue()).isEqualByComparingTo("50");
	}

	@Test
	void elCalentamientoNoCuentaParaLaProgresion() throws Exception {
		Escenario e = escenario();
		Objetivo o = e.objetivo();
		List<Map<String, Object>> series = new java.util.ArrayList<>(
				seriesDeTrabajo(o, "50", o.repeticionesMax() - 1, o.rirObjetivo()));
		// Un calentamiento más pesado y que «cumple» no debe subir la carga ni cambiar la base.
		series.add(con(con(serie(UUID.randomUUID(), o.idExterno(), 9, "100", o.repeticionesMax(), 0),
				"calentamiento", true), "orden", 99));

		guardarSesion(e.cliente(), UUID.randomUUID(), sesion(e.plan(), haceHoras(1), series))
				.andExpect(status().isOk());

		assertThat(cargaDe(e.cliente(), o.idExterno()).decimalValue()).isEqualByComparingTo("50");
	}

	@Test
	void laSesionMasRecientePorRealizadaEnMandaAunqueLleguePrimero() throws Exception {
		Escenario e = escenario();
		Objetivo o = e.objetivo();
		guardarSesion(e.cliente(), UUID.randomUUID(),
				sesion(e.plan(), haceHoras(1), seriesDeTrabajo(o, "60", o.repeticionesMax() - 1, o.rirObjetivo())))
						.andExpect(status().isOk());

		guardarSesion(e.cliente(), UUID.randomUUID(),
				sesion(e.plan(), haceHoras(30), seriesDeTrabajo(o, "40", o.repeticionesMax(), o.rirObjetivo())))
						.andExpect(status().isOk());

		assertThat(cargaDe(e.cliente(), o.idExterno()).decimalValue()).isEqualByComparingTo("60");
	}

	@Test
	void unaSerieCorregidaSeUsaConSuValorCorregidoEnLaProgresion() throws Exception {
		Escenario e = escenario();
		Instant cuando = haceHoras(1);
		Objetivo o = e.objetivo();
		UUID sesion = UUID.randomUUID();
		// Las series previstas, todas buenas menos la primera, que se apuntó mal (1 repetición).
		List<Map<String, Object>> series = new java.util.ArrayList<>(
				seriesDeTrabajo(o, "50", o.repeticionesMax(), o.rirObjetivo()));
		Map<String, Object> mala = serie(UUID.randomUUID(), o.idExterno(), 0, "50", 1, 2);
		series.set(0, mala);
		guardarSesion(e.cliente(), sesion, sesion(e.plan(), cuando, series)).andExpect(status().isOk());
		assertThat(cargaDe(e.cliente(), o.idExterno()).decimalValue()).isEqualByComparingTo("50");

		Map<String, Object> buena = con(
				serie(UUID.randomUUID(), o.idExterno(), o.series(), "50", o.repeticionesMax(), o.rirObjetivo()),
				"corrigeA", mala.get("uuid"));
		series.add(buena);
		guardarSesion(e.cliente(), sesion, sesion(e.plan(), cuando, series)).andExpect(status().isOk());

		assertThat(cargaDe(e.cliente(), o.idExterno()).decimalValue())
				.isEqualByComparingTo(BigDecimal.valueOf(50 + o.incrementoKg()));
	}

	// ---- reloj ----

	@Test
	void conMasDe24HorasDeDesfaseSeGuardaMarcadaParaRevision() throws Exception {
		Escenario e = escenario();
		UUID desfasada = UUID.randomUUID(), puntual = UUID.randomUUID();

		guardarSesion(e.cliente(), desfasada, sesion(e.plan(), haceHoras(25), seriesDeTrabajo(e.objetivo(), "50", 8, 2)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.relojDesfasado").value(true));
		guardarSesion(e.cliente(), puntual, sesion(e.plan(), haceHoras(23), seriesDeTrabajo(e.objetivo(), "50", 8, 2)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.relojDesfasado").value(false));

		assertThat(contar("SELECT reloj_desfasado FROM sesion WHERE uuid = ?", (Object) Conversiones.aBytes(desfasada)))
				.isEqualTo(1);
		assertThat(contar("SELECT COUNT(*) FROM sesion WHERE uuid = ? AND recibida_en > realizada_en",
				(Object) Conversiones.aBytes(desfasada))).isEqualTo(1);
	}

	// ---- dolor ----

	private Map<String, Object> serieConDolor(Objetivo o, UUID uuid, int dolor) {
		return con(serie(uuid, o.idExterno(), 0, "50", 8, 2), "dolor", dolor);
	}

	@Test
	void sinConsentimientoElDolorNoSeGuarda() throws Exception {
		Escenario e = escenario();
		UUID serie = UUID.randomUUID();

		guardarSesion(e.cliente(), UUID.randomUUID(),
				sesion(e.plan(), haceHoras(1), List.of(serieConDolor(e.objetivo(), serie, 7))))
						.andExpect(status().isOk());

		assertThat(contar("SELECT COUNT(*) FROM serie_realizada WHERE dolor IS NOT NULL AND uuid = ?",
				(Object) Conversiones.aBytes(serie))).isZero();
	}

	@Test
	void conConsentimientoElDolorSeGuardaCifradoYSeDescifraBien() throws Exception {
		Cliente cliente = clienteConLimitaciones();
		String plan = generarPlan(cliente);
		Objetivo o = ejercicioConIncremento(cliente);
		UUID serie = UUID.randomUUID();

		guardarSesion(cliente, UUID.randomUUID(), sesion(plan, haceHoras(1), List.of(serieConDolor(o, serie, 7))))
				.andExpect(status().isOk());

		byte[] guardado = jdbc.sql("SELECT dolor FROM serie_realizada WHERE uuid = :u")
				.param("u", Conversiones.aBytes(serie)).query(byte[].class).single();
		assertThat(guardado).isNotEqualTo("7".getBytes(StandardCharsets.UTF_8));
		assertThat(guardado.length).isGreaterThan(20);
		assertThat(new String(cifrador.descifrar("serie_realizada", "dolor", serie, guardado),
				StandardCharsets.UTF_8)).isEqualTo("7");
		consultar(RUTA_SESIONES, cliente).andExpect(jsonPath("$.sesiones[0].series[0].dolor").value(7));
	}

	@Test
	void sinConsentimientoElDolorGuardadoNoSeEnseña() throws Exception {
		Cliente cliente = clienteConLimitaciones();
		String plan = generarPlan(cliente);
		Objetivo o = ejercicioConIncremento(cliente);
		guardarSesion(cliente, UUID.randomUUID(), sesion(plan, haceHoras(1), List.of(serieConDolor(o, UUID.randomUUID(), 5))))
				.andExpect(status().isOk());

		borrarRecurso("/api/v1/consentimientos/limitaciones", cliente).andExpect(status().isNoContent());

		consultar(RUTA_SESIONES, cliente).andExpect(jsonPath("$.sesiones[0].series[0].dolor").doesNotExist());
		assertThat(contar("SELECT COUNT(*) FROM serie_realizada s JOIN sesion x ON x.uuid = s.sesion_uuid "
				+ "JOIN usuario u ON u.id = x.usuario_id WHERE u.correo = ? AND s.dolor IS NOT NULL",
				cliente.correo())).isZero();
	}

	@Test
	void unEjercicioQueDueleEnDosSesionesSaleEnDolorRepetidoYSinConsentimientoNada() throws Exception {
		Cliente cliente = clienteConLimitaciones();
		String plan = generarPlan(cliente);
		Objetivo o = ejercicioConIncremento(cliente);
		for (long horas : new long[] { 50, 26, 2 }) {
			int dolor = horas == 26 ? 3 : 7;
			guardarSesion(cliente, UUID.randomUUID(),
					sesion(plan, haceHoras(horas), List.of(serieConDolor(o, UUID.randomUUID(), dolor))))
							.andExpect(status().isOk());
		}

		consultar(RUTA_SESIONES + "/dolor-repetido", cliente).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].idExterno").value(o.idExterno()))
				.andExpect(jsonPath("$[0].sesionesConDolor").value(2))
				.andExpect(jsonPath("$[0].sesionesMiradas").value(3));

		borrarRecurso("/api/v1/consentimientos/limitaciones", cliente).andExpect(status().isNoContent());
		consultar(RUTA_SESIONES + "/dolor-repetido", cliente).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	// ---- aislamiento entre usuarios ----

	@Test
	void laSesionOLaSerieDeOtroUsuarioNoSeVeNiSeCorrige() throws Exception {
		Escenario mia = escenario();
		Escenario ajena = escenario();
		UUID sesionAjena = UUID.randomUUID();
		UUID serieAjena = UUID.randomUUID();
		guardarSesion(ajena.cliente(), sesionAjena, sesion(ajena.plan(), haceHoras(1),
				List.of(serie(serieAjena, ajena.objetivo().idExterno(), 0, "50", 8, 2)))).andExpect(status().isOk());

		// Mismo uuid de sesión: 404 sin revelar que existe.
		guardarSesion(mia.cliente(), sesionAjena, sesion(mia.plan(), haceHoras(1),
				seriesDeTrabajo(mia.objetivo(), "50", 8, 2))).andExpect(status().isNotFound());
		// Corregir una serie ajena: es como si no existiera.
		Map<String, Object> intento = con(serie(UUID.randomUUID(), mia.objetivo().idExterno(), 0, "50", 8, 2),
				"corrigeA", serieAjena.toString());
		guardarSesion(mia.cliente(), UUID.randomUUID(), sesion(mia.plan(), haceHoras(1), List.of(intento)))
				.andExpect(status().isUnprocessableContent());
		// Y no sale en mi lista.
		consultar(RUTA_SESIONES, mia.cliente()).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
		assertThat(seriesGuardadas(mia.cliente().correo())).isZero();
	}

	@Test
	void unaSerieConElUuidDeOtraSesionDa409() throws Exception {
		Escenario e = escenario();
		UUID serie = UUID.randomUUID();
		guardarSesion(e.cliente(), UUID.randomUUID(), sesion(e.plan(), haceHoras(2),
				List.of(serie(serie, e.objetivo().idExterno(), 0, "50", 8, 2)))).andExpect(status().isOk());

		guardarSesion(e.cliente(), UUID.randomUUID(), sesion(e.plan(), haceHoras(1),
				List.of(serie(serie, e.objetivo().idExterno(), 0, "50", 8, 2)))).andExpect(status().isConflict());
	}

	// ---- validación y listado ----

	@Test
	void validaRangosLimitesYEjerciciosDesconocidos() throws Exception {
		Escenario e = escenario();
		String id = e.objetivo().idExterno();
		Map<String, Object> buena = serie(UUID.randomUUID(), id, 0, "50", 8, 2);
		List<Map<String, Object>> demasiadas = java.util.stream.IntStream.range(0, 201)
				.mapToObj(i -> serie(UUID.randomUUID(), id, i, "50", 8, 2)).toList();

		for (Map<String, Object> mala : List.of(con(buena, "pesoKg", new BigDecimal("1000.5")),
				con(buena, "pesoKg", new BigDecimal("-1")), con(buena, "repeticiones", 101), con(buena, "rir", 11),
				con(buena, "rpe", new BigDecimal("0.5")), con(buena, "dolor", 11), con(buena, "uuid", null),
				con(buena, "pesoKg", new BigDecimal("50.123")))) {
			guardarSesion(e.cliente(), UUID.randomUUID(), sesion(e.plan(), haceHoras(1), List.of(mala)))
					.andExpect(status().isBadRequest());
		}
		guardarSesion(e.cliente(), UUID.randomUUID(), sesion(e.plan(), haceHoras(1), demasiadas))
				.andExpect(status().isBadRequest());
		guardarSesion(e.cliente(), UUID.randomUUID(),
				sesion(e.plan(), haceHoras(1), List.of(con(buena, "ejercicio", "no_existe"))))
						.andExpect(status().isUnprocessableContent());
		guardarSesion(e.cliente(), UUID.randomUUID(), sesion(e.plan(), haceHoras(1), List.of(buena, buena)))
				.andExpect(status().isBadRequest());
		Map<String, Object> anulaSinOriginal = con(buena, "anulada", true);
		guardarSesion(e.cliente(), UUID.randomUUID(), sesion(e.plan(), haceHoras(1), List.of(anulaSinOriginal)))
				.andExpect(status().isUnprocessableContent());
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.put(RUTA_SESIONES + "/no-es-un-uuid").header("Authorization", e.cliente().cabecera())
				.contentType("application/json").content("{}")).andExpect(status().isBadRequest());
		assertThat(sesionesGuardadas(e.cliente().correo())).isZero();
	}

	@Test
	void unCuerpoDemasiadoGrandeDa413() throws Exception {
		Escenario e = escenario();
		Map<String, Object> cuerpo = sesion(e.plan(), haceHoras(1), seriesDeTrabajo(e.objetivo(), "50", 8, 2));
		cuerpo.put("relleno", "x".repeat(300_000));

		guardarSesion(e.cliente(), UUID.randomUUID(), cuerpo).andExpect(status().isContentTooLarge());
	}

	@Test
	void elTopeDelCuerpoValeParaCualquierRutaNoSoloSesiones() throws Exception {
		String enorme = "{\"correo\":\"" + "x".repeat(300_000) + "\"}";

		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
				.contentType("application/json").content(enorme)).andExpect(status().isContentTooLarge());
	}

	@Test
	void sinSesionDa401() throws Exception {
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.put(RUTA_SESIONES + "/" + UUID.randomUUID()).contentType("application/json").content("{}"))
				.andExpect(status().isUnauthorized());
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(RUTA_PLAN_ACTUAL))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void laListaSePaginaYFiltraPorDesde() throws Exception {
		Escenario e = escenario();
		Instant vieja = haceHoras(10), nueva = haceHoras(2);
		guardarSesion(e.cliente(), UUID.randomUUID(), sesion(e.plan(), vieja, seriesDeTrabajo(e.objetivo(), "50", 8, 2)))
				.andExpect(status().isOk());
		guardarSesion(e.cliente(), UUID.randomUUID(), sesion(e.plan(), nueva, seriesDeTrabajo(e.objetivo(), "55", 8, 2)))
				.andExpect(status().isOk());

		JsonNode primera = cuerpoDe(consultar(RUTA_SESIONES + "?tamano=1", e.cliente()).andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(2)).andExpect(jsonPath("$.sesiones.length()").value(1)));
		assertThat(Instant.parse(primera.get("sesiones").get(0).get("realizadaEn").asString()))
				.isEqualTo(vieja.truncatedTo(java.time.temporal.ChronoUnit.MICROS));
		consultar(RUTA_SESIONES + "?tamano=1&pagina=1", e.cliente()).andExpect(jsonPath("$.sesiones.length()").value(1))
				.andExpect(jsonPath("$.sesiones[0].series.length()").value(e.objetivo().series()));
		consultar(RUTA_SESIONES + "?desde=" + haceHoras(5), e.cliente()).andExpect(jsonPath("$.total").value(1));
		consultar(RUTA_SESIONES + "?tamano=1000", e.cliente()).andExpect(status().isBadRequest());
	}
}
