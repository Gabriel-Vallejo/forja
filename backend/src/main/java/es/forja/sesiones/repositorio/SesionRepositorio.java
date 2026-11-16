package es.forja.sesiones.repositorio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import es.forja.comun.repositorio.Conversiones;
import es.forja.entreno.plan.modelo.HistorialSesiones;
import es.forja.entreno.plan.modelo.SerieTrabajo;
import es.forja.entreno.plan.modelo.SesionEjercicio;

/** Acceso a {@code sesion}, {@code serie_realizada} y la vista {@code serie_vigente}. Solo ve el dolor cifrado. */
@Repository
public class SesionRepositorio implements HistorialSesiones {

	public record FilaSesion(UUID uuid, long usuarioId, Long planVersionId, Integer diaPlan, Instant realizadaEn,
			Instant recibidaEn, boolean relojDesfasado) {
	}

	/** Serie tal cual está en la base, con el usuario dueño de su sesión. */
	public record FilaSerie(UUID uuid, UUID sesionUuid, long usuarioId, long ejercicioId, int orden, BigDecimal pesoKg,
			int repeticiones, Integer rir, BigDecimal rpe, boolean calentamiento, byte[] dolor, UUID corrigeA,
			boolean anulada, Instant realizadaEn) {
	}

	public record CabeceraVista(UUID uuid, UUID planVersion, Integer diaPlan, Instant realizadaEn, Instant recibidaEn,
			boolean relojDesfasado) {
	}

	public record SerieVigente(UUID sesionUuid, UUID uuid, String ejercicio, int orden, BigDecimal pesoKg,
			int repeticiones, Integer rir, BigDecimal rpe, boolean calentamiento, byte[] dolor, UUID corrigeA,
			boolean anulada, Instant realizadaEn) {
	}

	private static final String COLUMNAS_SERIE = """
			s.uuid, s.sesion_uuid, x.usuario_id, s.ejercicio_id, s.orden, s.peso_kg, s.repeticiones, s.rir, s.rpe,
			s.calentamiento, s.dolor, s.corrige_a, s.anulada, s.realizada_en""";

	private static final RowMapper<FilaSerie> SERIE = (rs, n) -> new FilaSerie(Conversiones.uuid(rs, "uuid"),
			Conversiones.uuid(rs, "sesion_uuid"), rs.getLong("usuario_id"), rs.getLong("ejercicio_id"),
			rs.getInt("orden"), rs.getBigDecimal("peso_kg"), rs.getInt("repeticiones"), entero(rs, "rir"),
			rs.getBigDecimal("rpe"), rs.getBoolean("calentamiento"), rs.getBytes("dolor"),
			uuidONulo(rs.getBytes("corrige_a")), rs.getBoolean("anulada"), Conversiones.instante(rs, "realizada_en"));

	private static final String CABECERA_VISTA = """
			SELECT x.uuid, p.uuid AS plan_uuid, x.dia_plan, x.realizada_en, x.recibida_en, x.reloj_desfasado
			FROM sesion x LEFT JOIN plan_version p ON p.id = x.plan_version_id""";

	private static final RowMapper<CabeceraVista> CABECERA = (rs, n) -> new CabeceraVista(Conversiones.uuid(rs, "uuid"),
			uuidONulo(rs.getBytes("plan_uuid")), entero(rs, "dia_plan"), Conversiones.instante(rs, "realizada_en"),
			Conversiones.instante(rs, "recibida_en"), rs.getBoolean("reloj_desfasado"));

	private final JdbcClient jdbc;

	public SesionRepositorio(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	private static Integer entero(java.sql.ResultSet rs, String columna) throws java.sql.SQLException {
		int valor = rs.getInt(columna);
		return rs.wasNull() ? null : valor;
	}

	private static UUID uuidONulo(byte[] bytes) {
		return bytes == null ? null : Conversiones.aUuid(bytes);
	}

	/** La sesión sea de quien sea: quien la usa decide si el dueño puede saber que existe. */
	public Optional<FilaSesion> sesion(UUID uuid) {
		return jdbc.sql("""
				SELECT uuid, usuario_id, plan_version_id, dia_plan, realizada_en, recibida_en, reloj_desfasado
				FROM sesion WHERE uuid = :uuid""").param("uuid", Conversiones.aBytes(uuid))
				.query((rs, n) -> new FilaSesion(Conversiones.uuid(rs, "uuid"), rs.getLong("usuario_id"),
						rs.getObject("plan_version_id", Long.class), entero(rs, "dia_plan"),
						Conversiones.instante(rs, "realizada_en"), Conversiones.instante(rs, "recibida_en"),
						rs.getBoolean("reloj_desfasado")))
				.optional();
	}

	public void insertarSesion(UUID uuid, long usuarioId, Long planVersionId, Integer diaPlan, Instant realizadaEn,
			Instant recibidaEn, boolean relojDesfasado) {
		jdbc.sql("""
				INSERT INTO sesion (uuid, usuario_id, plan_version_id, dia_plan, realizada_en, recibida_en,
				                    reloj_desfasado)
				VALUES (:uuid, :usuario, :plan, :dia, :realizada, :recibida, :desfasado)""")
				.param("uuid", Conversiones.aBytes(uuid)).param("usuario", usuarioId).param("plan", planVersionId)
				.param("dia", diaPlan).param("realizada", Conversiones.aSql(realizadaEn))
				.param("recibida", Conversiones.aSql(recibidaEn)).param("desfasado", relojDesfasado).update();
	}

	/** Las series que ya existen con esos uuid, sean de la sesión y del usuario que sean. */
	public Map<UUID, FilaSerie> series(Collection<UUID> uuids) {
		Map<UUID, FilaSerie> series = new HashMap<>();
		jdbc.sql("SELECT " + COLUMNAS_SERIE + " FROM serie_realizada s JOIN sesion x ON x.uuid = s.sesion_uuid "
				+ "WHERE s.uuid IN (:uuids)").param("uuids", uuids.stream().map(Conversiones::aBytes).toList())
				.query(SERIE).list().forEach(s -> series.put(s.uuid(), s));
		return series;
	}

	public boolean existeSerieDeUsuario(long usuarioId, UUID uuid) {
		return jdbc.sql("""
				SELECT COUNT(*) FROM serie_realizada s JOIN sesion x ON x.uuid = s.sesion_uuid
				WHERE s.uuid = :uuid AND x.usuario_id = :usuario""").param("uuid", Conversiones.aBytes(uuid))
				.param("usuario", usuarioId).query(Long.class).single() > 0;
	}

	/** Falla con DuplicateKeyException si el uuid ya existe o si {@code corrige_a} ya tiene una corrección. */
	public void insertarSerie(FilaSerie s) {
		jdbc.sql("""
				INSERT INTO serie_realizada (uuid, sesion_uuid, ejercicio_id, orden, peso_kg, repeticiones, rir, rpe,
				                             calentamiento, dolor, corrige_a, anulada, realizada_en)
				VALUES (:uuid, :sesion, :ejercicio, :orden, :peso, :repeticiones, :rir, :rpe, :calentamiento,
				        :dolor, :corrige, :anulada, :realizada)""").param("uuid", Conversiones.aBytes(s.uuid()))
				.param("sesion", Conversiones.aBytes(s.sesionUuid())).param("ejercicio", s.ejercicioId())
				.param("orden", s.orden()).param("peso", s.pesoKg()).param("repeticiones", s.repeticiones())
				.param("rir", s.rir()).param("rpe", s.rpe()).param("calentamiento", s.calentamiento())
				.param("dolor", s.dolor()).param("corrige", s.corrigeA() == null ? null : Conversiones.aBytes(s.corrigeA()))
				.param("anulada", s.anulada()).param("realizada", Conversiones.aSql(s.realizadaEn())).update();
	}

	/** Código externo → id interno de cada ejercicio del catálogo que exista. */
	public Map<String, Long> idsDeEjercicios(Collection<String> idsExternos) {
		Map<String, Long> ids = new HashMap<>();
		jdbc.sql("SELECT id, id_externo FROM ejercicio WHERE id_externo IN (:ids)").param("ids", idsExternos)
				.query((rs, n) -> ids.put(rs.getString("id_externo"), rs.getLong("id"))).list();
		return ids;
	}

	public Optional<CabeceraVista> cabecera(long usuarioId, UUID uuid) {
		return jdbc.sql(CABECERA_VISTA + " WHERE x.usuario_id = :usuario AND x.uuid = :uuid")
				.param("usuario", usuarioId).param("uuid", Conversiones.aBytes(uuid)).query(CABECERA).optional();
	}

	public List<CabeceraVista> cabeceras(long usuarioId, Instant desde, int tamano, long desplazamiento) {
		return jdbc.sql(CABECERA_VISTA + """
				 WHERE x.usuario_id = :usuario AND x.realizada_en >= :desde
				ORDER BY x.realizada_en, x.uuid LIMIT :tamano OFFSET :desplazamiento""")
				.param("usuario", usuarioId).param("desde", Conversiones.aSql(desde)).param("tamano", tamano)
				.param("desplazamiento", desplazamiento).query(CABECERA).list();
	}

	public long contar(long usuarioId, Instant desde) {
		return jdbc.sql("SELECT COUNT(*) FROM sesion WHERE usuario_id = :usuario AND realizada_en >= :desde")
				.param("usuario", usuarioId).param("desde", Conversiones.aSql(desde)).query(Long.class).single();
	}

	public List<SerieVigente> vigentes(Collection<UUID> sesiones) {
		return jdbc.sql("""
				SELECT v.sesion_uuid, v.uuid, e.id_externo, v.orden, v.peso_kg, v.repeticiones, v.rir, v.rpe,
				       v.calentamiento, v.dolor, v.corrige_a, v.anulada, v.realizada_en
				FROM serie_vigente v JOIN ejercicio e ON e.id = v.ejercicio_id
				WHERE v.sesion_uuid IN (:sesiones) ORDER BY v.orden, v.uuid""")
				.param("sesiones", sesiones.stream().map(Conversiones::aBytes).toList())
				.query((rs, n) -> new SerieVigente(Conversiones.uuid(rs, "sesion_uuid"), Conversiones.uuid(rs, "uuid"),
						rs.getString("id_externo"), rs.getInt("orden"), rs.getBigDecimal("peso_kg"),
						rs.getInt("repeticiones"), entero(rs, "rir"), rs.getBigDecimal("rpe"),
						rs.getBoolean("calentamiento"), rs.getBytes("dolor"), uuidONulo(rs.getBytes("corrige_a")),
						rs.getBoolean("anulada"), Conversiones.instante(rs, "realizada_en")))
				.list();
	}

	/** Borra el dolor guardado de todas las series del usuario: se revocó el consentimiento de limitaciones. */
	public void borrarDolorDeUsuario(long usuarioId) {
		jdbc.sql("""
				UPDATE serie_realizada s JOIN sesion x ON x.uuid = s.sesion_uuid SET s.dolor = NULL
				WHERE x.usuario_id = :usuario AND s.dolor IS NOT NULL""").param("usuario", usuarioId).update();
	}

	/** Todo el historial del ejercicio. shortcut: sin tope; si un usuario acumula años de sesiones, limitar a los últimos meses. */
	@Override
	public List<SesionEjercicio> sesionesDeTrabajo(long usuarioId, long ejercicioId) {
		Map<UUID, List<SerieTrabajo>> series = new LinkedHashMap<>();
		Map<UUID, Instant> fechas = new HashMap<>();
		jdbc.sql("""
				SELECT x.uuid, x.realizada_en, s.peso_kg, s.repeticiones, s.rir
				FROM sesion x JOIN serie_vigente s ON s.sesion_uuid = x.uuid
				WHERE x.usuario_id = :usuario AND s.ejercicio_id = :ejercicio AND s.calentamiento = FALSE
				ORDER BY x.realizada_en, x.uuid, s.orden, s.uuid""").param("usuario", usuarioId)
				.param("ejercicio", ejercicioId).query((rs, n) -> {
					UUID sesion = Conversiones.uuid(rs, "uuid");
					fechas.put(sesion, Conversiones.instante(rs, "realizada_en"));
					series.computeIfAbsent(sesion, k -> new ArrayList<>()).add(new SerieTrabajo(
							rs.getBigDecimal("peso_kg"), rs.getInt("repeticiones"), entero(rs, "rir")));
					return 0;
				}).list();
		return series.entrySet().stream()
				.map(e -> new SesionEjercicio(e.getKey(), fechas.get(e.getKey()), e.getValue())).toList();
	}
}
