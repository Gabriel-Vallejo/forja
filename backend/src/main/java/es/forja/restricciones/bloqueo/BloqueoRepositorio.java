package es.forja.restricciones.bloqueo;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import es.forja.comun.repositorio.Conversiones;

/** Acceso a la tabla {@code bloqueo_salud}: solo ve bytes cifrados; quien la usa los cifra y descifra. */
@Repository
public class BloqueoRepositorio {

	public static final String TABLA = "bloqueo_salud";
	public static final String COLUMNA_DATOS = "datos";

	public record FilaCifrada(UUID uuid, OrigenBloqueo origen, byte[] datos) {
	}

	private final JdbcClient jdbc;

	public BloqueoRepositorio(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public void insertar(UUID uuid, long usuarioId, OrigenBloqueo origen, byte[] datos) {
		jdbc.sql("INSERT INTO bloqueo_salud (uuid, usuario_id, origen, datos) VALUES (:uuid, :usuario, :origen, :datos)")
				.param("uuid", Conversiones.aBytes(uuid)).param("usuario", usuarioId).param("origen", origen.columna())
				.param("datos", datos).update();
	}

	public void borrar(long usuarioId, OrigenBloqueo origen) {
		jdbc.sql("DELETE FROM bloqueo_salud WHERE usuario_id = :usuario AND origen = :origen")
				.param("usuario", usuarioId).param("origen", origen.columna()).update();
	}

	public void borrarTodosDeUsuario(long usuarioId) {
		jdbc.sql("DELETE FROM bloqueo_salud WHERE usuario_id = :usuario").param("usuario", usuarioId).update();
	}

	public List<FilaCifrada> deUsuario(long usuarioId) {
		return jdbc.sql("SELECT uuid, origen, datos FROM bloqueo_salud WHERE usuario_id = :usuario ORDER BY id")
				.param("usuario", usuarioId)
				.query((rs, n) -> new FilaCifrada(Conversiones.uuid(rs, "uuid"),
						OrigenBloqueo.valueOf(rs.getString("origen").toUpperCase(java.util.Locale.ROOT)),
						rs.getBytes("datos")))
				.list();
	}
}
