package es.forja.comun.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import es.forja.PruebaConMysql;

@PruebaConMysql
@Transactional
class MigracionCuentaTest {

	private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 16, 12, 0);

	@Autowired
	private JdbcClient jdbc;

	private static byte[] bytes(UUID uuid) {
		return ByteBuffer.allocate(16).putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits())
				.array();
	}

	private long usuario(String correo) {
		jdbc.sql("""
				INSERT INTO usuario (uuid, correo, hash_contrasena, anio_nacimiento, mayor_de_edad, creado_en)
				VALUES (:uuid, :correo, 'hash', 1998, 1, :ahora)""")
				.param("uuid", bytes(UUID.randomUUID())).param("correo", correo).param("ahora", AHORA).update();
		return jdbc.sql("SELECT id FROM usuario WHERE correo = :correo").param("correo", correo).query(Long.class)
				.single();
	}

	private long dispositivo(long usuarioId) {
		jdbc.sql("""
				INSERT INTO dispositivo (uuid, usuario_id, nombre, plataforma, ultima_actividad)
				VALUES (:uuid, :usuario, 'Pixel', 'movil', :ahora)""")
				.param("uuid", bytes(UUID.randomUUID())).param("usuario", usuarioId).param("ahora", AHORA).update();
		return jdbc.sql("SELECT MAX(id) FROM dispositivo").query(Long.class).single();
	}

	private long contar(String tabla) {
		return jdbc.sql("SELECT COUNT(*) FROM " + tabla).query(Long.class).single();
	}

	@Test
	void borrarUnUsuarioBorraEnCascadaDispositivosYTokens() {
		long antesDispositivos = contar("dispositivo");
		long antesRefrescos = contar("token_refresco");
		long antesUnUso = contar("token_un_uso");
		long usuarioId = usuario("cascada@forja.test");
		long dispositivoId = dispositivo(usuarioId);
		jdbc.sql("""
				INSERT INTO token_refresco (dispositivo_id, familia, hash, caduca_en)
				VALUES (:d, :familia, :hash, :ahora)""").param("d", dispositivoId)
				.param("familia", bytes(UUID.randomUUID())).param("hash", new byte[32]).param("ahora", AHORA).update();
		jdbc.sql("""
				INSERT INTO token_un_uso (usuario_id, tipo, hash, caduca_en)
				VALUES (:u, 'verificacion', :hash, :ahora)""").param("u", usuarioId).param("hash", new byte[32])
				.param("ahora", AHORA).update();

		jdbc.sql("DELETE FROM usuario WHERE id = :id").param("id", usuarioId).update();

		assertThat(contar("dispositivo")).isEqualTo(antesDispositivos);
		assertThat(contar("token_refresco")).isEqualTo(antesRefrescos);
		assertThat(contar("token_un_uso")).isEqualTo(antesUnUso);
	}

	@Test
	void elCorreoYElHashDeTokenSonUnicos() {
		long usuarioId = usuario("unico@forja.test");

		assertThatThrownBy(() -> usuario("unico@forja.test")).isInstanceOf(DataAccessException.class);
		jdbc.sql("INSERT INTO token_un_uso (usuario_id, tipo, hash, caduca_en) VALUES (:u, 'recuperacion', :h, :a)")
				.param("u", usuarioId).param("h", new byte[32]).param("a", AHORA).update();
		assertThatThrownBy(() -> jdbc
				.sql("INSERT INTO token_un_uso (usuario_id, tipo, hash, caduca_en) VALUES (:u, 'recuperacion', :h, :a)")
				.param("u", usuarioId).param("h", new byte[32]).param("a", AHORA).update())
				.isInstanceOf(DataAccessException.class);
	}

	@Test
	void laPlataformaYElTipoSoloAdmitenSusValores() {
		long dispositivoUsuario = usuario("enum@forja.test");

		assertThatThrownBy(() -> jdbc.sql("""
				INSERT INTO dispositivo (uuid, usuario_id, nombre, plataforma, ultima_actividad)
				VALUES (:uuid, :u, 'X', 'tablet', :a)""").param("uuid", bytes(UUID.randomUUID()))
				.param("u", dispositivoUsuario).param("a", AHORA).update()).isInstanceOf(DataAccessException.class);
		assertThatThrownBy(() -> jdbc.sql(
				"INSERT INTO token_un_uso (usuario_id, tipo, hash, caduca_en) VALUES (:u, 'otro', :h, :a)")
				.param("u", dispositivoUsuario).param("h", new byte[32]).param("a", AHORA).update())
				.isInstanceOf(DataAccessException.class);
	}
}
