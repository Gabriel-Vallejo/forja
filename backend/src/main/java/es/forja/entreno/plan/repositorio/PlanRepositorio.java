package es.forja.entreno.plan.repositorio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import es.forja.comun.repositorio.Conversiones;
import es.forja.entreno.plan.modelo.GrupoMuscular.MusculoCatalogo;
import es.forja.entreno.plan.modelo.RolMuscular;

/** Acceso a {@code plan_version} y {@code ejercicio_plan}; las versiones solo se insertan, nunca se actualizan. */
@Repository
public class PlanRepositorio {

	private static final String COMPUESTO = "compuesto";

	/** Lo que se necesita del catálogo para fijar el objetivo de un ejercicio. */
	public record FichaEjercicio(long id, boolean compuesto, String material) {
	}

	public record CabeceraPlan(long id, UUID uuid, Instant creadoEn, String estado) {
	}

	public record FilaEjercicio(int dia, int orden, long ejercicioId, int series, int repeticionesMin,
			int repeticionesMax, int rirObjetivo, BigDecimal cargaKg, BigDecimal incrementoKg) {
	}

	/** Fila leída: la de arriba más el nombre y el identificador externo del catálogo. */
	public record FilaLeida(FilaEjercicio fila, String idExterno, String nombre) {
	}

	private final JdbcClient jdbc;

	public PlanRepositorio(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** Ficha de cada {@code idExterno} que exista en el catálogo. */
	public Map<String, FichaEjercicio> fichas(Collection<String> idsExternos) {
		Map<String, FichaEjercicio> fichas = new LinkedHashMap<>();
		if (idsExternos.isEmpty()) {
			return fichas; // MySQL no acepta «IN ()»
		}
		jdbc.sql("""
				SELECT e.id, e.id_externo, e.mecanica, m.codigo AS material FROM ejercicio e
				JOIN material m ON m.id = e.implemento_id WHERE e.id_externo IN (:ids)""")
				.param("ids", idsExternos).query((rs, n) -> {
					fichas.put(rs.getString("id_externo"), new FichaEjercicio(rs.getLong("id"),
							COMPUESTO.equals(rs.getString("mecanica")), rs.getString("material")));
					return 0;
				}).list();
		return fichas;
	}

	public long insertarVersion(UUID uuid, long usuarioId, Instant ahora, String estado, String trazaJson) {
		KeyHolder claves = new GeneratedKeyHolder();
		jdbc.sql("""
				INSERT INTO plan_version (uuid, usuario_id, creado_en, estado, traza)
				VALUES (:uuid, :usuario, :ahora, :estado, :traza)""").param("uuid", Conversiones.aBytes(uuid))
				.param("usuario", usuarioId).param("ahora", Conversiones.aSql(ahora)).param("estado", estado)
				.param("traza", trazaJson).update(claves);
		return claves.getKey().longValue();
	}

	public void insertarEjercicio(long planId, FilaEjercicio f) {
		jdbc.sql("""
				INSERT INTO ejercicio_plan (plan_version_id, dia, orden, ejercicio_id, series, repeticiones_min,
				                            repeticiones_max, rir_objetivo, carga_kg, incremento_kg)
				VALUES (:plan, :dia, :orden, :ejercicio, :series, :min, :max, :rir, :carga, :incremento)""")
				.param("plan", planId).param("dia", f.dia()).param("orden", f.orden())
				.param("ejercicio", f.ejercicioId()).param("series", f.series()).param("min", f.repeticionesMin())
				.param("max", f.repeticionesMax()).param("rir", f.rirObjetivo()).param("carga", f.cargaKg())
				.param("incremento", f.incrementoKg()).update();
	}

	/** Todas las versiones del usuario, de la más antigua a la última. */
	public List<CabeceraPlan> versiones(long usuarioId) {
		return jdbc.sql("SELECT id, uuid, creado_en, estado FROM plan_version WHERE usuario_id = :usuario ORDER BY id")
				.param("usuario", usuarioId)
				.query((rs, n) -> new CabeceraPlan(rs.getLong("id"), Conversiones.uuid(rs, "uuid"),
						Conversiones.instante(rs, "creado_en"), rs.getString("estado")))
				.list();
	}

	/** El último plan del usuario. */
	public Optional<CabeceraPlan> ultimo(long usuarioId) {
		return jdbc.sql("""
				SELECT id, uuid, creado_en, estado FROM plan_version WHERE usuario_id = :usuario
				ORDER BY id DESC LIMIT 1""").param("usuario", usuarioId)
				.query((rs, n) -> new CabeceraPlan(rs.getLong("id"), Conversiones.uuid(rs, "uuid"),
						Conversiones.instante(rs, "creado_en"), rs.getString("estado")))
				.optional();
	}

	/** La traza guardada de una versión, en JSON (sin datos de salud: ver {@code TrazaGuardada}). */
	public String traza(long planId) {
		return jdbc.sql("SELECT traza FROM plan_version WHERE id = :plan").param("plan", planId).query(String.class)
				.single();
	}

	/** El id interno de una versión, solo si es del usuario: una ajena no existe a efectos de quien pregunta. */
	public Optional<Long> idDeVersion(long usuarioId, UUID uuid) {
		return jdbc.sql("SELECT id FROM plan_version WHERE usuario_id = :usuario AND uuid = :uuid")
				.param("usuario", usuarioId).param("uuid", Conversiones.aBytes(uuid)).query(Long.class).optional();
	}

	/** Los músculos de todos esos ejercicios en una sola consulta; primarios antes que secundarios. */
	public Map<Long, List<MusculoCatalogo>> musculos(Collection<Long> ejercicioIds) {
		Map<Long, List<MusculoCatalogo>> porEjercicio = new LinkedHashMap<>();
		if (ejercicioIds.isEmpty()) {
			return porEjercicio;
		}
		jdbc.sql("""
				SELECT em.ejercicio_id, m.codigo, em.rol FROM ejercicio_musculo em
				JOIN musculo m ON m.id = em.musculo_id WHERE em.ejercicio_id IN (:ids)
				ORDER BY em.ejercicio_id, em.rol, m.codigo""").param("ids", ejercicioIds).query((rs, n) -> {
					porEjercicio.computeIfAbsent(rs.getLong("ejercicio_id"), k -> new java.util.ArrayList<>())
							.add(new MusculoCatalogo(rs.getString("codigo"), RolMuscular.deCodigo(rs.getString("rol"))));
					return 0;
				}).list();
		return porEjercicio;
	}

	public List<FilaLeida> ejercicios(long planId) {
		return jdbc.sql("""
				SELECT p.dia, p.orden, p.ejercicio_id, p.series, p.repeticiones_min, p.repeticiones_max,
				       p.rir_objetivo, p.carga_kg, p.incremento_kg, e.id_externo,
				       COALESCE(e.nombre_es, e.nombre_original) AS nombre
				FROM ejercicio_plan p JOIN ejercicio e ON e.id = p.ejercicio_id
				WHERE p.plan_version_id = :plan ORDER BY p.dia, p.orden""").param("plan", planId)
				.query((rs, n) -> new FilaLeida(new FilaEjercicio(rs.getInt("dia"), rs.getInt("orden"),
						rs.getLong("ejercicio_id"), rs.getInt("series"), rs.getInt("repeticiones_min"),
						rs.getInt("repeticiones_max"), rs.getInt("rir_objetivo"), rs.getBigDecimal("carga_kg"),
						rs.getBigDecimal("incremento_kg")), rs.getString("id_externo"), rs.getString("nombre")))
				.list();
	}
}
