package es.forja.entreno.rutina.repositorio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import es.forja.entreno.rutina.modelo.EjercicioRutina;

/** Lo que la rutina necesita del catálogo y que la entidad {@code Ejercicio} no mapea: músculos y nombres. */
@Repository
public class CatalogoRutinaRepositorio {

	private static final String CURADO = "curado";
	private static final String COMPUESTO = "compuesto";
	private static final String PRIMARIO = "primario";

	/** Ficha de un ejercicio curado; {@code musculos} con el peso doble de {@link EjercicioRutina}. */
	public record FichaCurado(String idExterno, boolean aptoFuerza, boolean compuesto, boolean posicionEstirada,
			String nombreMaterial, Map<String, Integer> musculos) {
	}

	private record Fila(String idExterno, boolean aptoFuerza, boolean compuesto, boolean posicionEstirada,
			String nombreMaterial, String musculo, String rol) {
	}

	private final JdbcClient jdbc;

	public CatalogoRutinaRepositorio(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** Los ejercicios curados con sus músculos, por {@code idExterno}. */
	public Map<String, FichaCurado> curados() {
		List<Fila> filas = jdbc.sql("""
				SELECT e.id_externo, e.apto_fuerza, e.mecanica, e.posicion_estirada, mat.nombre AS material,
				       m.codigo AS musculo, h.rol
				FROM ejercicio e
				JOIN material mat ON mat.id = e.implemento_id
				LEFT JOIN ejercicio_musculo h ON h.ejercicio_id = e.id
				LEFT JOIN musculo m ON m.id = h.musculo_id
				WHERE e.origen_etiquetado = :curado
				ORDER BY e.id_externo, m.codigo""").param("curado", CURADO)
				.query((rs, n) -> new Fila(rs.getString("id_externo"), rs.getBoolean("apto_fuerza"),
						COMPUESTO.equals(rs.getString("mecanica")), rs.getBoolean("posicion_estirada"),
						rs.getString("material"), rs.getString("musculo"), rs.getString("rol")))
				.list();
		Map<String, FichaCurado> fichas = new LinkedHashMap<>();
		for (Fila f : filas) {
			FichaCurado ficha = fichas.computeIfAbsent(f.idExterno(), id -> new FichaCurado(id, f.aptoFuerza(),
					f.compuesto(), f.posicionEstirada(), f.nombreMaterial(), new LinkedHashMap<>()));
			if (f.musculo() != null) {
				ficha.musculos().put(f.musculo(), PRIMARIO.equals(f.rol()) ? EjercicioRutina.PESO_PRIMARIO
						: EjercicioRutina.PESO_SECUNDARIO);
			}
		}
		return fichas;
	}

	/** Código → nombre de cada músculo. */
	public Map<String, String> nombresMusculo() {
		Map<String, String> nombres = new LinkedHashMap<>();
		jdbc.sql("SELECT codigo, nombre FROM musculo ORDER BY codigo")
				.query((rs, n) -> Map.entry(rs.getString("codigo"), rs.getString("nombre"))).list()
				.forEach(e -> nombres.put(e.getKey(), e.getValue()));
		return nombres;
	}

	public Set<String> codigosMaterial() {
		return Set.copyOf(jdbc.sql("SELECT codigo FROM material").query(String.class).list());
	}
}
