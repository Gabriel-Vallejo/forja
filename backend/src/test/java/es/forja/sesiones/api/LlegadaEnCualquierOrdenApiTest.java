package es.forja.sesiones.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import es.forja.comun.repositorio.Conversiones;
import tools.jackson.databind.JsonNode;

/**
 * Propiedad con semilla fija (ADR 0008): aplicar las mismas sesiones en cualquier orden de llegada y con reenvíos
 * (completos o parciales) deja el mismo estado final y la misma carga objetivo.
 */
class LlegadaEnCualquierOrdenApiTest extends SoporteSesiones {

	private static final int SESIONES = 5;
	private static final int SERIES_NORMALES = 3;

	/** Una sesión con identificadores lógicos: se materializa con los uuid de cada usuario. */
	private record Plantilla(int indice, Instant realizadaEn, String ejercicio, List<Fila> filas) {
	}

	private record Fila(String peso, int repeticiones, Integer rir, Integer corrigeA, boolean anulada) {
	}

	private static List<Plantilla> plantillas(long semilla, List<Objetivo> objetivos) {
		Random azar = new Random(semilla);
		Instant base = Instant.now().minus(java.time.Duration.ofHours(60)).truncatedTo(ChronoUnit.MICROS);
		List<Plantilla> plantillas = new ArrayList<>();
		for (int i = 0; i < SESIONES; i++) {
			Objetivo o = objetivos.get(i % 3);
			List<Fila> filas = new ArrayList<>();
			for (int j = 0; j < SERIES_NORMALES; j++) {
				filas.add(new Fila(String.valueOf(40 + azar.nextInt(10) * 2.5), 5 + azar.nextInt(8),
						azar.nextInt(4), null, false));
			}
			filas.add(new Fila(String.valueOf(40 + azar.nextInt(10) * 2.5), o.repeticionesMax(), o.rirObjetivo(), 0, false));
			filas.add(new Fila("40", 5, 2, 1, true));
			plantillas.add(new Plantilla(i, base.plus(java.time.Duration.ofHours(i * 12L)), o.idExterno(), filas));
		}
		return plantillas;
	}

	private static Map<String, UUID> identificadores(long semilla, int usuario) {
		Random azar = new Random(semilla * 31 + usuario);
		Map<String, UUID> ids = new HashMap<>();
		for (int i = 0; i < SESIONES; i++) {
			ids.put("s" + i, new UUID(azar.nextLong(), azar.nextLong()));
			for (int j = 0; j < SERIES_NORMALES + 2; j++) {
				ids.put("s" + i + "." + j, new UUID(azar.nextLong(), azar.nextLong()));
			}
		}
		return ids;
	}

	private Map<String, Object> cuerpo(Plantilla p, Map<String, UUID> ids, String plan, int cuantasSeries) {
		List<Map<String, Object>> series = new ArrayList<>();
		for (int j = 0; j < cuantasSeries; j++) {
			Fila f = p.filas().get(j);
			Map<String, Object> s = serie(ids.get("s" + p.indice() + "." + j), p.ejercicio(), j, f.peso(),
					f.repeticiones(), f.rir());
			s.put("realizadaEn", p.realizadaEn().toString());
			if (f.corrigeA() != null) {
				s.put("corrigeA", ids.get("s" + p.indice() + "." + f.corrigeA()).toString());
			}
			s.put("anulada", f.anulada());
			series.add(s);
		}
		return sesion(plan, p.realizadaEn(), series);
	}

	/** Estado final con identificadores lógicos, para poder comparar dos usuarios distintos. */
	private List<String> estado(Cliente cliente, Map<String, UUID> ids) {
		Map<UUID, String> logico = new HashMap<>();
		ids.forEach((nombre, uuid) -> logico.put(uuid, nombre));
		List<String> filas = new ArrayList<>();
		jdbc.sql("""
				SELECT x.uuid AS sesion, x.realizada_en FROM sesion x JOIN usuario u ON u.id = x.usuario_id
				WHERE u.correo = :c""").param("c", cliente.correo())
				.query((rs, n) -> "sesion " + logico.get(Conversiones.uuid(rs, "sesion")) + " "
						+ rs.getTimestamp("realizada_en").toInstant())
				.list().forEach(filas::add);
		jdbc.sql("""
				SELECT s.uuid, s.corrige_a, s.orden, s.peso_kg, s.repeticiones, s.rir, s.anulada,
				       (SELECT COUNT(*) FROM serie_vigente v WHERE v.uuid = s.uuid) AS vigente
				FROM serie_realizada s JOIN sesion x ON x.uuid = s.sesion_uuid JOIN usuario u ON u.id = x.usuario_id
				WHERE u.correo = :c""").param("c", cliente.correo())
				.query((rs, n) -> "serie " + logico.get(Conversiones.uuid(rs, "uuid")) + " "
						+ (rs.getBytes("corrige_a") == null ? "-" : logico.get(Conversiones.uuid(rs, "corrige_a")))
						+ " " + rs.getInt("orden") + " " + rs.getBigDecimal("peso_kg") + " "
						+ rs.getInt("repeticiones") + " " + rs.getObject("rir") + " " + rs.getBoolean("anulada")
						+ " vigente=" + rs.getInt("vigente"))
				.list().forEach(filas::add);
		Collections.sort(filas);
		return filas;
	}

	private Map<String, String> cargas(Cliente cliente) throws Exception {
		JsonNode plan = planActual(cliente);
		Map<String, String> cargas = new TreeMap<>();
		objetivos(plan).forEach(o -> cargas.put(o.idExterno(),
				o.cargaKg().isNull() ? "-" : new BigDecimal(o.cargaKg().asString()).stripTrailingZeros().toPlainString()));
		return cargas;
	}

	@ParameterizedTest
	@ValueSource(longs = { 1, 2, 3, 4, 5 })
	void elMismoConjuntoDeSesionesDejaElMismoEstadoSeaCualSeaElOrdenYLosReenvios(long semilla) throws Exception {
		Cliente ordenado = cuentaConSesion("Pixel");
		Cliente revuelto = cuentaConSesion("Pixel");
		String planOrdenado = generarPlan(ordenado);
		String planRevuelto = generarPlan(revuelto);
		List<Objetivo> objetivos = objetivos(planActual(ordenado)).stream().filter(o -> o.incrementoKg() > 0)
				.limit(3).toList();
		assertThat(objetivos).hasSize(3);
		List<Plantilla> plantillas = plantillas(semilla, objetivos);
		Map<String, UUID> idsOrdenado = identificadores(semilla, 0);
		Map<String, UUID> idsRevuelto = identificadores(semilla, 1);

		for (Plantilla p : plantillas) {
			guardarSesion(ordenado, idsOrdenado.get("s" + p.indice()),
					cuerpo(p, idsOrdenado, planOrdenado, p.filas().size())).andExpect(status().isOk());
		}
		// Cada sesión llega entera una vez como mínimo, con reenvíos completos y parciales en medio.
		Random azar = new Random(semilla);
		List<int[]> envios = new ArrayList<>();
		for (Plantilla p : plantillas) {
			envios.add(new int[] { p.indice(), p.filas().size() });
			for (int extra = azar.nextInt(3); extra > 0; extra--) {
				envios.add(new int[] { p.indice(), 1 + azar.nextInt(p.filas().size()) });
			}
		}
		Collections.shuffle(envios, azar);
		// Un envío parcial (prefijo) puede llegar antes que el completo; el completo de cada sesión sigue ahí.
		for (int[] envio : envios) {
			Plantilla p = plantillas.get(envio[0]);
			guardarSesion(revuelto, idsRevuelto.get("s" + p.indice()),
					cuerpo(p, idsRevuelto, planRevuelto, envio[1])).andExpect(status().isOk());
		}

		assertThat(estado(revuelto, idsRevuelto)).isEqualTo(estado(ordenado, idsOrdenado));
		assertThat(cargas(revuelto)).isEqualTo(cargas(ordenado));
	}
}
