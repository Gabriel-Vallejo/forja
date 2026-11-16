package es.forja.restricciones.repositorio;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import es.forja.comun.repositorio.Conversiones;

/** Acceso a la tabla {@code restriccion}: solo ve bytes cifrados; quien la usa los cifra y descifra. */
@Repository
public class RestriccionRepositorio {

	public static final String TABLA = "restriccion";
	public static final String COLUMNA_DATOS = "datos";

	/** Fila tal cual está en la base: el uuid y los datos cifrados. */
	public record FilaCifrada(UUID uuid, byte[] datos) {
	}

	private static final RowMapper<FilaCifrada> FILA = (rs, n) -> new FilaCifrada(Conversiones.uuid(rs, "uuid"),
			rs.getBytes("datos"));

	private final JdbcClient jdbc;

	public RestriccionRepositorio(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public void insertar(UUID uuid, long usuarioId, byte[] datos, Instant ahora) {
		jdbc.sql("""
				INSERT INTO restriccion (uuid, usuario_id, datos, creado_en, actualizado_en)
				VALUES (:uuid, :usuario, :datos, :ahora, :ahora)""").param("uuid", Conversiones.aBytes(uuid))
				.param("usuario", usuarioId).param("datos", datos).param("ahora", Conversiones.aSql(ahora)).update();
	}

	public List<FilaCifrada> deUsuario(long usuarioId) {
		return jdbc.sql("SELECT uuid, datos FROM restriccion WHERE usuario_id = :usuario ORDER BY id")
				.param("usuario", usuarioId).query(FILA).list();
	}

	/** La restricción solo si es de ese usuario: una ajena no existe a efectos de quien pregunta. */
	public Optional<FilaCifrada> deUsuario(long usuarioId, UUID uuid) {
		return jdbc.sql("SELECT uuid, datos FROM restriccion WHERE usuario_id = :usuario AND uuid = :uuid")
				.param("usuario", usuarioId).param("uuid", Conversiones.aBytes(uuid)).query(FILA).optional();
	}

	public long contar(long usuarioId) {
		return jdbc.sql("SELECT COUNT(*) FROM restriccion WHERE usuario_id = :usuario").param("usuario", usuarioId)
				.query(Long.class).single();
	}

	/** Devuelve cuántas filas cambió: 0 si no existe o es de otro usuario. */
	public int actualizar(long usuarioId, UUID uuid, byte[] datos, Instant ahora) {
		return jdbc.sql("""
				UPDATE restriccion SET datos = :datos, actualizado_en = :ahora
				WHERE usuario_id = :usuario AND uuid = :uuid""").param("datos", datos)
				.param("ahora", Conversiones.aSql(ahora)).param("usuario", usuarioId)
				.param("uuid", Conversiones.aBytes(uuid)).update();
	}

	/** Devuelve cuántas filas borró: 0 si no existe o es de otro usuario. */
	public int borrar(long usuarioId, UUID uuid) {
		return jdbc.sql("DELETE FROM restriccion WHERE usuario_id = :usuario AND uuid = :uuid")
				.param("usuario", usuarioId).param("uuid", Conversiones.aBytes(uuid)).update();
	}

	public void borrarTodasDeUsuario(long usuarioId) {
		jdbc.sql("DELETE FROM restriccion WHERE usuario_id = :usuario").param("usuario", usuarioId).update();
	}
}
