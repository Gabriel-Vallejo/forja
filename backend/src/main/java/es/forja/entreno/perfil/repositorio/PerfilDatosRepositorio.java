package es.forja.entreno.perfil.repositorio;

import java.time.Instant;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import es.forja.comun.repositorio.Conversiones;
import es.forja.entreno.perfil.modelo.DatosPerfil;
import es.forja.entreno.perfil.modelo.Sexo;

/** Acceso a {@code perfil_datos}: sexo, experiencia y objetivo, una fila por usuario. */
@Repository
public class PerfilDatosRepositorio {

	private final JdbcClient jdbc;

	public PerfilDatosRepositorio(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public Optional<DatosPerfil> buscar(long usuarioId) {
		return jdbc.sql("SELECT sexo, experiencia, objetivo FROM perfil_datos WHERE usuario_id = :u")
				.param("u", usuarioId).query((rs, n) -> new DatosPerfil(rs.getString("sexo"),
						rs.getString("experiencia"), rs.getString("objetivo")))
				.optional();
	}

	/** Sexo guardado; {@code NO_INDICA} si el usuario no ha guardado nada. */
	public Sexo sexo(long usuarioId) {
		return buscar(usuarioId).map(d -> Sexo.desdeCodigo(d.sexo())).orElse(Sexo.NO_INDICA);
	}

	public void guardar(long usuarioId, DatosPerfil datos, Instant ahora) {
		jdbc.sql("""
				INSERT INTO perfil_datos (usuario_id, sexo, experiencia, objetivo, actualizado_en)
				VALUES (:u, :sexo, :experiencia, :objetivo, :ahora)
				ON DUPLICATE KEY UPDATE sexo = VALUES(sexo), experiencia = VALUES(experiencia),
				    objetivo = VALUES(objetivo), actualizado_en = VALUES(actualizado_en)""").param("u", usuarioId)
				.param("sexo", datos.sexo()).param("experiencia", datos.experiencia())
				.param("objetivo", datos.objetivo()).param("ahora", Conversiones.aSql(ahora)).update();
	}
}
