package es.forja.entreno.plan.repositorio;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import es.forja.comun.repositorio.Conversiones;
import es.forja.entreno.plan.modelo.PreferenciasGuardadas;
import es.forja.entreno.plan.modelo.PreferenciasGuardadas.Elemento;
import es.forja.entreno.plan.modelo.PreferenciasGuardadas.SeriesFijadas;
import es.forja.entreno.plan.modelo.PreferenciasPlan;

/** Acceso a {@code preferencia_plan}, {@code preferencia_ejercicio} y {@code preferencia_musculo} (V9). */
@Repository
public class PreferenciasRepositorio {

	private static final String NOMBRE_EJERCICIO = "COALESCE(e.nombre_es, e.nombre_original)";

	private final JdbcClient jdbc;

	public PreferenciasRepositorio(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** Las del usuario, con nombres; sin filas, ninguna. */
	public PreferenciasGuardadas de(long usuarioId) {
		List<Elemento> exclusiones = jdbc.sql("SELECT e.id_externo, " + NOMBRE_EJERCICIO + " AS nombre "
				+ "FROM preferencia_ejercicio p JOIN ejercicio e ON e.id = p.ejercicio_id "
				+ "WHERE p.usuario_id = :u AND p.excluido ORDER BY e.id_externo").param("u", usuarioId)
				.query((rs, n) -> new Elemento(rs.getString("id_externo"), rs.getString("nombre"))).list();
		List<SeriesFijadas> series = jdbc.sql("SELECT e.id_externo, " + NOMBRE_EJERCICIO + " AS nombre, "
				+ "p.series_semana FROM preferencia_ejercicio p JOIN ejercicio e ON e.id = p.ejercicio_id "
				+ "WHERE p.usuario_id = :u AND NOT p.excluido ORDER BY e.id_externo").param("u", usuarioId)
				.query((rs, n) -> new SeriesFijadas(rs.getString("id_externo"), rs.getString("nombre"),
						rs.getInt("series_semana"))).list();
		List<Elemento> prioritarios = jdbc.sql("""
				SELECT m.codigo, m.nombre FROM preferencia_musculo p JOIN musculo m ON m.id = p.musculo_id
				WHERE p.usuario_id = :u ORDER BY m.codigo""").param("u", usuarioId)
				.query((rs, n) -> new Elemento(rs.getString("codigo"), rs.getString("nombre"))).list();
		Optional<Boolean> variedad = jdbc.sql("SELECT variedad FROM preferencia_plan WHERE usuario_id = :u")
				.param("u", usuarioId).query(Boolean.class).optional();
		return new PreferenciasGuardadas(exclusiones, prioritarios, series, variedad.orElse(false));
	}

	/**
	 * Reemplaza las del usuario por {@code p}; el llamador abre la transacción y ya ha comprobado que los
	 * ejercicios y músculos existen.
	 */
	public void guardar(long usuarioId, PreferenciasPlan p, Instant ahora) {
		jdbc.sql("DELETE FROM preferencia_ejercicio WHERE usuario_id = :u").param("u", usuarioId).update();
		jdbc.sql("DELETE FROM preferencia_musculo WHERE usuario_id = :u").param("u", usuarioId).update();
		jdbc.sql("""
				INSERT INTO preferencia_plan (usuario_id, variedad, actualizado_en) VALUES (:u, :v, :ahora)
				ON DUPLICATE KEY UPDATE variedad = VALUES(variedad), actualizado_en = VALUES(actualizado_en)""")
				.param("u", usuarioId).param("v", p.variedad()).param("ahora", Conversiones.aSql(ahora)).update();
		p.exclusiones().forEach(id -> insertarEjercicio(usuarioId, id, null));
		p.series().forEach((id, series) -> insertarEjercicio(usuarioId, id, series));
		p.prioritarios().forEach(codigo -> jdbc.sql("""
				INSERT INTO preferencia_musculo (usuario_id, musculo_id)
				SELECT :u, id FROM musculo WHERE codigo = :codigo""").param("u", usuarioId).param("codigo", codigo)
				.update());
	}

	/** Con {@code series} nulo, el ejercicio queda quitado. */
	private void insertarEjercicio(long usuarioId, String idExterno, Integer series) {
		jdbc.sql("""
				INSERT INTO preferencia_ejercicio (usuario_id, ejercicio_id, excluido, series_semana)
				SELECT :u, id, :excluido, :series FROM ejercicio WHERE id_externo = :id""").param("u", usuarioId)
				.param("excluido", series == null).param("series", series).param("id", idExterno).update();
	}
}
