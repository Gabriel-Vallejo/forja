package es.forja.consentimiento.repositorio;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import es.forja.comun.repositorio.Conversiones;
import es.forja.consentimiento.modelo.Consentimiento;
import es.forja.consentimiento.modelo.RegistroConsentimiento;
import es.forja.consentimiento.modelo.TipoConsentimiento;

@Repository
public class ConsentimientoRepositorio {

	private static final RowMapper<Consentimiento> FILA = (rs, n) -> new Consentimiento(
			TipoConsentimiento.desdeCodigo(rs.getString("tipo")), rs.getString("version_texto"),
			Conversiones.instante(rs, "dado_en"));

	private final JdbcClient jdbc;

	public ConsentimientoRepositorio(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public List<Consentimiento> vivos(long usuarioId) {
		return jdbc.sql("""
				SELECT tipo, version_texto, dado_en FROM consentimiento
				WHERE usuario_id = :usuario AND revocado_en IS NULL ORDER BY dado_en, id""")
				.param("usuario", usuarioId).query(FILA).list();
	}

	/** Todos los consentimientos del usuario, vivos y retirados, del más antiguo al más reciente. */
	public List<RegistroConsentimiento> historial(long usuarioId) {
		return jdbc.sql("""
				SELECT tipo, version_texto, dado_en, revocado_en FROM consentimiento
				WHERE usuario_id = :usuario ORDER BY dado_en, id""").param("usuario", usuarioId)
				.query((rs, n) -> new RegistroConsentimiento(TipoConsentimiento.desdeCodigo(rs.getString("tipo")),
						rs.getString("version_texto"), Conversiones.instante(rs, "dado_en"),
						Conversiones.instante(rs, "revocado_en")))
				.list();
	}

	public Optional<Consentimiento> vivo(long usuarioId, TipoConsentimiento tipo) {
		return jdbc.sql("""
				SELECT tipo, version_texto, dado_en FROM consentimiento
				WHERE usuario_id = :usuario AND tipo = :tipo AND revocado_en IS NULL""")
				.param("usuario", usuarioId).param("tipo", tipo.codigo()).query(FILA).optional();
	}

	/** Falla con DuplicateKeyException si ya hay uno vivo de ese tipo (índice único de la migración V3). */
	public void insertar(long usuarioId, TipoConsentimiento tipo, String versionTexto, Instant ahora) {
		jdbc.sql("""
				INSERT INTO consentimiento (usuario_id, tipo, version_texto, dado_en)
				VALUES (:usuario, :tipo, :version, :ahora)""").param("usuario", usuarioId)
				.param("tipo", tipo.codigo()).param("version", versionTexto)
				.param("ahora", Conversiones.aSql(ahora)).update();
	}

	/** Marca como revocado el consentimiento vivo; devuelve cuántas filas cambió (0 si no había). */
	public int revocar(long usuarioId, TipoConsentimiento tipo, Instant ahora) {
		return jdbc.sql("""
				UPDATE consentimiento SET revocado_en = :ahora
				WHERE usuario_id = :usuario AND tipo = :tipo AND revocado_en IS NULL""")
				.param("ahora", Conversiones.aSql(ahora)).param("usuario", usuarioId).param("tipo", tipo.codigo())
				.update();
	}
}
