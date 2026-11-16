package es.forja.cuenta.repositorio;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import es.forja.comun.repositorio.Conversiones;
import es.forja.cuenta.modelo.Dispositivo;
import es.forja.cuenta.modelo.Plataforma;

@Repository
public class DispositivoRepositorio {

	private static final String COLUMNAS = "id, uuid, usuario_id, nombre, plataforma, ultima_actividad, revocado_en";

	private static final RowMapper<Dispositivo> FILA = (rs, n) -> mapear(rs);

	private final JdbcClient jdbc;

	public DispositivoRepositorio(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	private static Dispositivo mapear(java.sql.ResultSet rs) throws SQLException {
		return new Dispositivo(rs.getLong("id"), Conversiones.uuid(rs, "uuid"), rs.getLong("usuario_id"),
				rs.getString("nombre"), Plataforma.deCodigo(rs.getString("plataforma")),
				Conversiones.instante(rs, "ultima_actividad"), Conversiones.instante(rs, "revocado_en"));
	}

	public long insertar(UUID uuid, long usuarioId, String nombre, Plataforma plataforma, Instant ahora) {
		KeyHolder claves = new GeneratedKeyHolder();
		jdbc.sql("""
				INSERT INTO dispositivo (uuid, usuario_id, nombre, plataforma, ultima_actividad)
				VALUES (:uuid, :usuario, :nombre, :plataforma, :ahora)""")
				.param("uuid", Conversiones.aBytes(uuid)).param("usuario", usuarioId).param("nombre", nombre)
				.param("plataforma", plataforma.codigo()).param("ahora", Conversiones.aSql(ahora)).update(claves);
		return claves.getKey().longValue();
	}

	/** Dispositivo vivo (no revocado) de un usuario; vacío si no existe, es de otro o está revocado. */
	public Optional<Dispositivo> vivoDeUsuario(UUID uuid, long usuarioId) {
		return jdbc.sql("SELECT " + COLUMNAS
				+ " FROM dispositivo WHERE uuid = :uuid AND usuario_id = :usuario AND revocado_en IS NULL")
				.param("uuid", Conversiones.aBytes(uuid)).param("usuario", usuarioId).query(FILA).optional();
	}

	public Optional<Dispositivo> porId(long id) {
		return jdbc.sql("SELECT " + COLUMNAS + " FROM dispositivo WHERE id = :id").param("id", id).query(FILA)
				.optional();
	}

	public List<Dispositivo> vivosDeUsuario(long usuarioId) {
		return jdbc.sql("SELECT " + COLUMNAS
				+ " FROM dispositivo WHERE usuario_id = :usuario AND revocado_en IS NULL"
				+ " ORDER BY ultima_actividad DESC, id DESC").param("usuario", usuarioId).query(FILA).list();
	}

	/** Dice si el {@code sid} del token sigue vivo y pertenece al {@code sub}. */
	public boolean estaVivo(UUID dispositivo, UUID usuario) {
		return jdbc.sql("""
				SELECT COUNT(*) FROM dispositivo d JOIN usuario u ON u.id = d.usuario_id
				WHERE d.uuid = :dispositivo AND u.uuid = :usuario AND d.revocado_en IS NULL""")
				.param("dispositivo", Conversiones.aBytes(dispositivo))
				.param("usuario", Conversiones.aBytes(usuario)).query(Integer.class).single() > 0;
	}

	public void actualizarActividad(long id, String nombre, Plataforma plataforma, Instant ahora) {
		jdbc.sql("""
				UPDATE dispositivo SET nombre = :nombre, plataforma = :plataforma, ultima_actividad = :ahora
				WHERE id = :id""").param("nombre", nombre).param("plataforma", plataforma.codigo())
				.param("ahora", Conversiones.aSql(ahora)).param("id", id).update();
	}

	public void tocar(long id, Instant ahora) {
		jdbc.sql("UPDATE dispositivo SET ultima_actividad = :ahora WHERE id = :id")
				.param("ahora", Conversiones.aSql(ahora)).param("id", id).update();
	}

	public void revocar(long id, Instant ahora) {
		jdbc.sql("UPDATE dispositivo SET revocado_en = :ahora WHERE id = :id AND revocado_en IS NULL")
				.param("ahora", Conversiones.aSql(ahora)).param("id", id).update();
	}

	public void revocarTodosDeUsuario(long usuarioId, Instant ahora) {
		jdbc.sql("UPDATE dispositivo SET revocado_en = :ahora WHERE usuario_id = :usuario AND revocado_en IS NULL")
				.param("ahora", Conversiones.aSql(ahora)).param("usuario", usuarioId).update();
	}
}
