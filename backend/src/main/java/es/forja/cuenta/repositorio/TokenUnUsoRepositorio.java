package es.forja.cuenta.repositorio;

import java.time.Instant;
import java.util.Optional;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import es.forja.cuenta.modelo.TipoTokenUnUso;
import es.forja.cuenta.modelo.TokenUnUso;

@Repository
public class TokenUnUsoRepositorio {

	private static final RowMapper<TokenUnUso> FILA = (rs, n) -> new TokenUnUso(rs.getLong("id"),
			rs.getLong("usuario_id"), tipo(rs.getString("tipo")), Conversiones.instante(rs, "caduca_en"),
			Conversiones.instante(rs, "usado_en"));

	private final JdbcClient jdbc;

	public TokenUnUsoRepositorio(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	private static TipoTokenUnUso tipo(String codigo) {
		return TipoTokenUnUso.valueOf(codigo.toUpperCase(java.util.Locale.ROOT));
	}

	public void insertar(long usuarioId, TipoTokenUnUso tipo, byte[] hash, Instant caducaEn) {
		jdbc.sql("""
				INSERT INTO token_un_uso (usuario_id, tipo, hash, caduca_en)
				VALUES (:usuario, :tipo, :hash, :caduca)""").param("usuario", usuarioId)
				.param("tipo", tipo.codigo()).param("hash", hash).param("caduca", Conversiones.aSql(caducaEn))
				.update();
	}

	public Optional<TokenUnUso> porHash(byte[] hash) {
		return jdbc.sql("SELECT id, usuario_id, tipo, caduca_en, usado_en FROM token_un_uso WHERE hash = :hash")
				.param("hash", hash).query(FILA).optional();
	}

	/** Lo gasta solo si seguía sin usar; devuelve false si otra petición se adelantó. */
	public boolean marcarUsado(long id, Instant ahora) {
		return jdbc.sql("UPDATE token_un_uso SET usado_en = :ahora WHERE id = :id AND usado_en IS NULL")
				.param("ahora", Conversiones.aSql(ahora)).param("id", id).update() == 1;
	}
}
