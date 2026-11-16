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

/** Migración V3: un solo consentimiento vivo por usuario y tipo, y cascada desde el usuario. */
@PruebaConMysql
@Transactional
class MigracionConsentimientoTest {

	private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 16, 12, 0);

	@Autowired
	private JdbcClient jdbc;

	private static byte[] bytes(UUID uuid) {
		return ByteBuffer.allocate(16).putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits())
				.array();
	}

	private long usuario() {
		String correo = "m" + UUID.randomUUID() + "@forja.test";
		jdbc.sql("""
				INSERT INTO usuario (uuid, correo, hash_contrasena, anio_nacimiento, mayor_de_edad, creado_en)
				VALUES (:uuid, :correo, 'hash', 1998, 1, :ahora)""")
				.param("uuid", bytes(UUID.randomUUID())).param("correo", correo).param("ahora", AHORA).update();
		return jdbc.sql("SELECT id FROM usuario WHERE correo = :c").param("c", correo).query(Long.class).single();
	}

	private void consentimiento(long usuarioId, String tipo, LocalDateTime revocadoEn) {
		jdbc.sql("""
				INSERT INTO consentimiento (usuario_id, tipo, version_texto, dado_en, revocado_en)
				VALUES (:u, :tipo, 'v1', :ahora, :revocado)""").param("u", usuarioId).param("tipo", tipo)
				.param("ahora", AHORA).param("revocado", revocadoEn).update();
	}

	private long contar(String tabla, long usuarioId) {
		return jdbc.sql("SELECT COUNT(*) FROM " + tabla + " WHERE usuario_id = :u").param("u", usuarioId)
				.query(Long.class).single();
	}

	@Test
	void soloAdmiteUnConsentimientoVivoPorUsuarioYTipo() {
		long usuarioId = usuario();
		consentimiento(usuarioId, "limitaciones", null);

		assertThatThrownBy(() -> consentimiento(usuarioId, "limitaciones", null))
				.isInstanceOf(DataAccessException.class);
		consentimiento(usuarioId, "lesiones", null);
		consentimiento(usuario(), "limitaciones", null);
	}

	@Test
	void losRevocadosSeGuardanYNoImpidenDarloDeNuevo() {
		long usuarioId = usuario();
		consentimiento(usuarioId, "limitaciones", AHORA);
		consentimiento(usuarioId, "limitaciones", AHORA.plusDays(1));

		consentimiento(usuarioId, "limitaciones", null);

		assertThat(contar("consentimiento", usuarioId)).isEqualTo(3);
	}

	@Test
	void elTipoSoloAdmiteLosCincoValores() {
		long usuarioId = usuario();

		assertThatThrownBy(() -> consentimiento(usuarioId, "otro", null)).isInstanceOf(DataAccessException.class);
	}

	@Test
	void borrarElUsuarioBorraConsentimientosYRestricciones() {
		long usuarioId = usuario();
		consentimiento(usuarioId, "limitaciones", null);
		jdbc.sql("""
				INSERT INTO restriccion (uuid, usuario_id, datos, creado_en, actualizado_en)
				VALUES (:uuid, :u, :datos, :ahora, :ahora)""").param("uuid", bytes(UUID.randomUUID()))
				.param("u", usuarioId).param("datos", new byte[] { 1 }).param("ahora", AHORA).update();

		jdbc.sql("DELETE FROM usuario WHERE id = :id").param("id", usuarioId).update();

		assertThat(contar("consentimiento", usuarioId)).isZero();
		assertThat(contar("restriccion", usuarioId)).isZero();
	}
}
