package es.forja.cuenta.repositorio;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import es.forja.comun.repositorio.Conversiones;
import es.forja.cuenta.modelo.Usuario;

@Repository
public class UsuarioRepositorio {

	private static final String COLUMNAS = """
			id, uuid, correo, hash_contrasena, verificado, anio_nacimiento, mayor_de_edad, fallos_seguidos,
			bloqueado_hasta""";

	private static final RowMapper<Usuario> FILA = (rs, n) -> new Usuario(rs.getLong("id"),
			Conversiones.uuid(rs, "uuid"), rs.getString("correo"), rs.getString("hash_contrasena"),
			rs.getBoolean("verificado"), rs.getInt("anio_nacimiento"), rs.getBoolean("mayor_de_edad"),
			rs.getInt("fallos_seguidos"), Conversiones.instante(rs, "bloqueado_hasta"));

	private final JdbcClient jdbc;

	public UsuarioRepositorio(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** Inserta una cuenta nueva y devuelve su id; falla con DuplicateKeyException si el correo ya existe. */
	public long insertar(UUID uuid, String correo, String hashContrasena, int anioNacimiento,
			boolean mayorDeEdad, Instant ahora) {
		KeyHolder claves = new GeneratedKeyHolder();
		jdbc.sql("""
				INSERT INTO usuario (uuid, correo, hash_contrasena, anio_nacimiento, mayor_de_edad, creado_en)
				VALUES (:uuid, :correo, :hash, :anio, :mayor, :creado)""")
				.param("uuid", Conversiones.aBytes(uuid)).param("correo", correo).param("hash", hashContrasena)
				.param("anio", anioNacimiento).param("mayor", mayorDeEdad)
				.param("creado", Conversiones.aSql(ahora)).update(claves);
		return claves.getKey().longValue();
	}

	public Optional<Usuario> porCorreo(String correoNormalizado) {
		return jdbc.sql("SELECT " + COLUMNAS + " FROM usuario WHERE correo = :correo")
				.param("correo", correoNormalizado).query(FILA).optional();
	}

	public Optional<Usuario> porUuid(UUID uuid) {
		return jdbc.sql("SELECT " + COLUMNAS + " FROM usuario WHERE uuid = :uuid")
				.param("uuid", Conversiones.aBytes(uuid)).query(FILA).optional();
	}

	public Optional<Usuario> porId(long id) {
		return jdbc.sql("SELECT " + COLUMNAS + " FROM usuario WHERE id = :id").param("id", id).query(FILA)
				.optional();
	}

	public void guardarFallos(long id, int fallosSeguidos, Instant bloqueadoHasta) {
		jdbc.sql("UPDATE usuario SET fallos_seguidos = :fallos, bloqueado_hasta = :hasta WHERE id = :id")
				.param("fallos", fallosSeguidos).param("hasta", Conversiones.aSql(bloqueadoHasta))
				.param("id", id).update();
	}

	/**
	 * Suma un fallo con un UPDATE atómico y devuelve el contador resultante. Hay que llamarlo dentro de una
	 * transacción: el bloqueo de fila del UPDATE hace que la lectura siguiente vea todos los fallos.
	 */
	public int sumarFallo(long id) {
		jdbc.sql("UPDATE usuario SET fallos_seguidos = fallos_seguidos + 1 WHERE id = :id").param("id", id).update();
		return jdbc.sql("SELECT fallos_seguidos FROM usuario WHERE id = :id").param("id", id).query(Integer.class)
				.single();
	}

	public void bloquearHasta(long id, Instant hasta) {
		jdbc.sql("UPDATE usuario SET bloqueado_hasta = :hasta WHERE id = :id")
				.param("hasta", Conversiones.aSql(hasta)).param("id", id).update();
	}

	public void marcarVerificado(long id) {
		jdbc.sql("UPDATE usuario SET verificado = 1 WHERE id = :id").param("id", id).update();
	}

	/** Cambia la contraseña y levanta cualquier bloqueo por fallos. */
	public void cambiarContrasena(long id, String hashContrasena) {
		jdbc.sql("""
				UPDATE usuario SET hash_contrasena = :hash, fallos_seguidos = 0, bloqueado_hasta = NULL
				WHERE id = :id""").param("hash", hashContrasena).param("id", id).update();
	}

	public void borrar(long id) {
		jdbc.sql("DELETE FROM usuario WHERE id = :id").param("id", id).update();
	}
}
