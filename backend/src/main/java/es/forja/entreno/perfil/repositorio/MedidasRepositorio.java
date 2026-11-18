package es.forja.entreno.perfil.repositorio;

import java.time.Instant;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import es.forja.comun.repositorio.Conversiones;

/** Acceso a {@code perfil_medidas}: solo ve bytes cifrados; quien la usa los cifra y descifra. */
@Repository
public class MedidasRepositorio {

	public static final String TABLA = "perfil_medidas";
	public static final String COLUMNA_DATOS = "datos";

	private final JdbcClient jdbc;

	public MedidasRepositorio(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public Optional<byte[]> buscar(long usuarioId) {
		return jdbc.sql("SELECT datos FROM perfil_medidas WHERE usuario_id = :u").param("u", usuarioId)
				.query((rs, n) -> rs.getBytes("datos")).optional();
	}

	public void guardar(long usuarioId, byte[] datos, Instant ahora) {
		jdbc.sql("""
				INSERT INTO perfil_medidas (usuario_id, datos, actualizado_en) VALUES (:u, :datos, :ahora)
				ON DUPLICATE KEY UPDATE datos = VALUES(datos), actualizado_en = VALUES(actualizado_en)""")
				.param("u", usuarioId).param("datos", datos).param("ahora", Conversiones.aSql(ahora)).update();
	}

	public void borrar(long usuarioId) {
		jdbc.sql("DELETE FROM perfil_medidas WHERE usuario_id = :u").param("u", usuarioId).update();
	}
}
