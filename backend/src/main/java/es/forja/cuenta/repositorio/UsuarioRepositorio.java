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
			id, uuid, correo, hash_contrasena, verificado, anio_nacimiento, mayor_de_edad""";

	private static final RowMapper<Usuario> FILA = (rs, n) -> new Usuario(rs.getLong("id"),
			Conversiones.uuid(rs, "uuid"), rs.getString("correo"), rs.getString("hash_contrasena"),
			rs.getBoolean("verificado"), rs.getInt("anio_nacimiento"), rs.getBoolean("mayor_de_edad"));

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

	/** Cuándo se dio de alta la cuenta; no va en {@link Usuario} porque solo la usa la exportación. */
	public Optional<Instant> creadoEn(long id) {
		return jdbc.sql("SELECT creado_en FROM usuario WHERE id = :id").param("id", id)
				.query((rs, n) -> Conversiones.instante(rs, "creado_en")).optional();
	}

	public Optional<Usuario> porId(long id) {
		return jdbc.sql("SELECT " + COLUMNAS + " FROM usuario WHERE id = :id").param("id", id).query(FILA)
				.optional();
	}

	public void marcarVerificado(long id) {
		jdbc.sql("UPDATE usuario SET verificado = 1 WHERE id = :id").param("id", id).update();
	}

	/** Cambia la contraseña; los fallos de login los olvida {@code FallosLogin}. */
	public void cambiarContrasena(long id, String hashContrasena) {
		jdbc.sql("UPDATE usuario SET hash_contrasena = :hash WHERE id = :id").param("hash", hashContrasena)
				.param("id", id).update();
	}

	public void borrar(long id) {
		jdbc.sql("DELETE FROM usuario WHERE id = :id").param("id", id).update();
	}
}
