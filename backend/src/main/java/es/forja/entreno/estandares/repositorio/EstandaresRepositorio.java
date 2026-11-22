package es.forja.entreno.estandares.repositorio;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Lee {@code estandar_fuerza}: los percentiles de OpenPowerlifting que deja la semilla R__estandares_fuerza.sql. */
@Repository
public class EstandaresRepositorio {

	/**
	 * Un grupo con sus 99 percentiles, del 1 al 99 en orden.
	 *
	 * @param metrica {@code kg_1rm} (carga total) o {@code kg_lastre} (kg añadidos al peso corporal)
	 * @param pesoHastaKg {@code null} en la última franja, sin techo
	 */
	public record TablaEstandar(String levantamiento, String metrica, String sexo, BigDecimal pesoDesdeKg,
			@Nullable BigDecimal pesoHastaKg, int muestra, List<BigDecimal> kg) {
	}

	private record Fila(String levantamiento, String metrica, String sexo, BigDecimal pesoDesdeKg, @Nullable BigDecimal pesoHastaKg,
			int muestra, BigDecimal kg) {
	}

	private final JdbcClient jdbc;

	public EstandaresRepositorio(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public List<TablaEstandar> todas() {
		List<Fila> filas = jdbc.sql("""
				SELECT levantamiento, metrica, sexo, peso_desde_kg, peso_hasta_kg, muestra, kg FROM estandar_fuerza
				ORDER BY levantamiento, sexo, peso_desde_kg, percentil""")
				.query((rs, n) -> new Fila(rs.getString("levantamiento"), rs.getString("metrica"), rs.getString("sexo"),
						rs.getBigDecimal("peso_desde_kg"), rs.getBigDecimal("peso_hasta_kg"), rs.getInt("muestra"),
						rs.getBigDecimal("kg")))
				.list();
		// El ORDER BY deja juntas las filas de cada grupo y sus percentiles en orden.
		Map<List<Object>, List<Fila>> grupos = filas.stream().collect(Collectors.groupingBy(
				f -> List.of(f.levantamiento(), f.sexo(), f.pesoDesdeKg()), LinkedHashMap::new, Collectors.toList()));
		return grupos.values().stream().map(g -> {
			Fila f = g.getFirst();
			return new TablaEstandar(f.levantamiento(), f.metrica(), f.sexo(), f.pesoDesdeKg(), f.pesoHastaKg(), f.muestra(),
					g.stream().map(Fila::kg).toList());
		}).toList();
	}
}
