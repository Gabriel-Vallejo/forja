package es.forja.entreno.perfil.repositorio;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import es.forja.comun.repositorio.Conversiones;
import es.forja.entreno.perfil.modelo.AjustesEntreno;

/** Acceso a {@code perfil_entreno} y su tabla puente de material. */
@Repository
public class PerfilEntrenoRepositorio {

	private record Cabecera(int dias, int minutos) {
	}

	private final JdbcClient jdbc;

	public PerfilEntrenoRepositorio(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public Optional<AjustesEntreno> buscar(long usuarioId) {
		Optional<Cabecera> cabecera = jdbc
				.sql("SELECT dias_por_semana, minutos_por_sesion FROM perfil_entreno WHERE usuario_id = :u")
				.param("u", usuarioId)
				.query((rs, n) -> new Cabecera(rs.getInt("dias_por_semana"), rs.getInt("minutos_por_sesion")))
				.optional();
		return cabecera.map(c -> new AjustesEntreno(c.dias(), c.minutos(), jdbc.sql("""
				SELECT m.codigo FROM perfil_entreno_material p JOIN material m ON m.id = p.material_id
				WHERE p.usuario_id = :u ORDER BY m.id""").param("u", usuarioId).query(String.class).list()));
	}

	/** Upsert; el llamador abre la transacción. Los códigos ya van validados contra la tabla {@code material}. */
	public void guardar(long usuarioId, AjustesEntreno ajustes, Instant ahora) {
		jdbc.sql("""
				INSERT INTO perfil_entreno (usuario_id, dias_por_semana, minutos_por_sesion, actualizado_en)
				VALUES (:u, :dias, :minutos, :ahora)
				ON DUPLICATE KEY UPDATE dias_por_semana = VALUES(dias_por_semana),
				    minutos_por_sesion = VALUES(minutos_por_sesion), actualizado_en = VALUES(actualizado_en)""")
				.param("u", usuarioId).param("dias", ajustes.diasPorSemana())
				.param("minutos", ajustes.minutosPorSesion()).param("ahora", Conversiones.aSql(ahora)).update();
		jdbc.sql("DELETE FROM perfil_entreno_material WHERE usuario_id = :u").param("u", usuarioId).update();
		jdbc.sql("""
				INSERT INTO perfil_entreno_material (usuario_id, material_id)
				SELECT :u, id FROM material WHERE codigo IN (:codigos)""").param("u", usuarioId)
				.param("codigos", ajustes.material().stream().distinct().toList()).update();
	}

	/** Materiales que la app ofrece: todos menos {@code otro}, que no es un material que el usuario pueda tener. */
	public List<Opcion> materialesOfrecidos() {
		return jdbc.sql("SELECT codigo, nombre FROM material WHERE codigo <> 'otro' ORDER BY id")
				.query((rs, n) -> new Opcion(rs.getString("codigo"), rs.getString("nombre"))).list();
	}

	public record Opcion(String codigo, String nombre) {
	}
}
