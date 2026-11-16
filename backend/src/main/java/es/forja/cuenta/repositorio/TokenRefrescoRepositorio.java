package es.forja.cuenta.repositorio;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import es.forja.comun.repositorio.Conversiones;
import es.forja.cuenta.modelo.TokenRefresco;

@Repository
public class TokenRefrescoRepositorio {

	private static final RowMapper<TokenRefresco> FILA = (rs, n) -> new TokenRefresco(rs.getLong("id"),
			rs.getLong("dispositivo_id"), Conversiones.uuid(rs, "familia"), Conversiones.instante(rs, "caduca_en"),
			Conversiones.instante(rs, "usado_en"), Conversiones.instante(rs, "revocado_en"));

	private final JdbcClient jdbc;

	public TokenRefrescoRepositorio(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public void insertar(long dispositivoId, UUID familia, byte[] hash, Instant caducaEn) {
		jdbc.sql("""
				INSERT INTO token_refresco (dispositivo_id, familia, hash, caduca_en)
				VALUES (:dispositivo, :familia, :hash, :caduca)""").param("dispositivo", dispositivoId)
				.param("familia", Conversiones.aBytes(familia)).param("hash", hash)
				.param("caduca", Conversiones.aSql(caducaEn)).update();
	}

	public Optional<TokenRefresco> porHash(byte[] hash) {
		return jdbc.sql("""
				SELECT id, dispositivo_id, familia, caduca_en, usado_en, revocado_en
				FROM token_refresco WHERE hash = :hash""").param("hash", hash).query(FILA).optional();
	}

	/** Lo marca como usado solo si seguía vivo; devuelve false si otra petición se adelantó. */
	public boolean marcarUsado(long id, Instant ahora) {
		return jdbc.sql("""
				UPDATE token_refresco SET usado_en = :ahora
				WHERE id = :id AND usado_en IS NULL AND revocado_en IS NULL""")
				.param("ahora", Conversiones.aSql(ahora)).param("id", id).update() == 1;
	}

	public void revocarFamilia(UUID familia, Instant ahora) {
		jdbc.sql("UPDATE token_refresco SET revocado_en = :ahora WHERE familia = :familia AND revocado_en IS NULL")
				.param("ahora", Conversiones.aSql(ahora)).param("familia", Conversiones.aBytes(familia)).update();
	}

	public void revocarDeDispositivo(long dispositivoId, Instant ahora) {
		jdbc.sql("""
				UPDATE token_refresco SET revocado_en = :ahora
				WHERE dispositivo_id = :dispositivo AND revocado_en IS NULL""")
				.param("ahora", Conversiones.aSql(ahora)).param("dispositivo", dispositivoId).update();
	}

	public void revocarDeUsuario(long usuarioId, Instant ahora) {
		jdbc.sql("""
				UPDATE token_refresco t JOIN dispositivo d ON d.id = t.dispositivo_id
				SET t.revocado_en = :ahora WHERE d.usuario_id = :usuario AND t.revocado_en IS NULL""")
				.param("ahora", Conversiones.aSql(ahora)).param("usuario", usuarioId).update();
	}
}
