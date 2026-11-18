package es.forja.entreno.movilidad.repositorio;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import es.forja.comun.repositorio.Conversiones;
import es.forja.entreno.movilidad.modelo.Movilidad.ResultadoMovilidad;
import es.forja.entreno.movilidad.modelo.TestMovilidad;
import es.forja.entreno.movilidad.modelo.Tramo;

/** Acceso a {@code test_movilidad}: una fila por usuario y test. */
@Repository
public class MovilidadRepositorio {

	private final JdbcClient jdbc;

	public MovilidadRepositorio(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public Map<TestMovilidad, ResultadoMovilidad> deUsuario(long usuarioId) {
		Map<TestMovilidad, ResultadoMovilidad> resultados = new EnumMap<>(TestMovilidad.class);
		jdbc.sql("SELECT tipo, tramo, hecho_en FROM test_movilidad WHERE usuario_id = :u").param("u", usuarioId)
				.query(rs -> {
					resultados.put(TestMovilidad.desdeCodigo(rs.getString("tipo")),
							new ResultadoMovilidad(rs.getString("tramo"), Conversiones.instante(rs, "hecho_en")));
				});
		return resultados;
	}

	/**
	 * Reemplaza los tests del usuario por {@code tramos}; el llamador abre la transacción. Si el tramo de un test no
	 * cambia, conserva su fecha.
	 */
	public void guardar(long usuarioId, Map<TestMovilidad, Tramo> tramos, Instant ahora) {
		for (TestMovilidad test : TestMovilidad.values()) {
			Tramo tramo = tramos.get(test);
			if (tramo == null) {
				jdbc.sql("DELETE FROM test_movilidad WHERE usuario_id = :u AND tipo = :tipo").param("u", usuarioId)
						.param("tipo", test.codigo()).update();
				continue;
			}
			// MySQL asigna de izquierda a derecha: la fecha se compara con el tramo de antes de cambiarlo.
			jdbc.sql("""
					INSERT INTO test_movilidad (usuario_id, tipo, tramo, hecho_en) VALUES (:u, :tipo, :tramo, :ahora)
					ON DUPLICATE KEY UPDATE hecho_en = IF(tramo = VALUES(tramo), hecho_en, VALUES(hecho_en)),
					    tramo = VALUES(tramo)""").param("u", usuarioId).param("tipo", test.codigo())
					.param("tramo", tramo.codigo()).param("ahora", Conversiones.aSql(ahora)).update();
		}
	}

	public void borrarTodos(long usuarioId) {
		jdbc.sql("DELETE FROM test_movilidad WHERE usuario_id = :u").param("u", usuarioId).update();
	}
}
