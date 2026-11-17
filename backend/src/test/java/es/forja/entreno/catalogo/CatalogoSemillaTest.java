package es.forja.entreno.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.annotation.Transactional;

import es.forja.PruebaConMysql;
import es.forja.entreno.catalogo.modelo.Ejercicio;
import es.forja.entreno.catalogo.repositorio.EjercicioRepositorio;

@PruebaConMysql
@Transactional
class CatalogoSemillaTest {

	private static final int EJERCICIOS_EN_LA_FUENTE = 876;
	private static final int CURADOS_MINIMO = 55;
	private static final int CURADOS_MAXIMO = 70;
	private static final String SEMILLA = "db/migration/R__catalogo_semilla.sql";

	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private Flyway flyway;
	@Autowired
	private DataSource dataSource;
	@Autowired
	private EjercicioRepositorio ejercicios;

	private int contar(String sql) {
		Integer total = jdbc.queryForObject(sql, Integer.class);
		return total == null ? 0 : total;
	}

	@Test
	void flywayAplicaLasMigracionesYLaSemillaRepetibleSinErrores() {
		MigrationInfo[] aplicadas = flyway.info().applied();

		assertThat(aplicadas).extracting(m -> m.getState()).containsOnly(MigrationState.SUCCESS);
		assertThat(Arrays.stream(aplicadas).map(m -> m.getScript()))
				.containsExactlyInAnyOrder("V1__catalogo_ejercicios.sql", "V2__cuenta.sql",
						"V3__consentimiento_y_restricciones.sql", "V4__plan_y_sesiones.sql", "V5__bloqueo_por_bandera.sql",
						"R__catalogo_semilla.sql");
		assertThat(flyway.info().pending()).isEmpty();
	}

	@Test
	void cargaTodosLosEjerciciosDeLaFuenteYUnosSesentaCurados() {
		assertThat(ejercicios.count()).isEqualTo(EJERCICIOS_EN_LA_FUENTE);
		assertThat(contar("SELECT COUNT(*) FROM ejercicio WHERE origen_etiquetado = 'curado'"))
				.isBetween(CURADOS_MINIMO, CURADOS_MAXIMO);
		assertThat(contar("SELECT COUNT(*) FROM ejercicio WHERE revisado = 1")).isZero();
	}

	@Test
	void todoEjercicioCuradoTienePatronFamiliaImplementoYUnMusculoPrimario() {
		assertThat(contar("""
				SELECT COUNT(*) FROM ejercicio e
				WHERE e.origen_etiquetado = 'curado'
				  AND (e.patron_id IS NULL OR e.familia_sustitucion IS NULL OR e.implemento_id IS NULL
				       OR e.nombre_es IS NULL
				       OR NOT EXISTS (SELECT 1 FROM ejercicio_musculo em
				                      WHERE em.ejercicio_id = e.id AND em.rol = 'primario'))
				""")).isZero();
	}

	@Test
	void ningunPesoDeMusculoSeSaleDeUnoOMedio() {
		assertThat(contar("""
				SELECT COUNT(*) FROM ejercicio_musculo
				WHERE (rol = 'primario' AND peso <> 1.0) OR (rol = 'secundario' AND peso <> 0.5)
				""")).isZero();
	}

	@Test
	void elEsquemaRechazaUnPesoQueNoCuadraConElRol() {
		Long id = jdbc.queryForObject("SELECT MIN(id) FROM ejercicio", Long.class);

		assertThatThrownBy(() -> jdbc.update(
				"INSERT INTO ejercicio_musculo (ejercicio_id, musculo_id, rol, peso) "
						+ "SELECT ?, id, 'primario', 0.7 FROM musculo LIMIT 1", id))
				.isInstanceOf(DataAccessException.class)
				.hasMessageContaining("ck_ejercicio_musculo_peso");
	}

	@Test
	void cadaFamiliaCuradaTieneAlMenosDosVariantes() {
		List<String> familiasCojas = jdbc.queryForList("""
				SELECT familia_sustitucion FROM ejercicio
				WHERE origen_etiquetado = 'curado'
				GROUP BY familia_sustitucion HAVING COUNT(*) < 2
				""", String.class);

		assertThat(familiasCojas).isEmpty();
	}

	@Test
	void devuelveLosCuradosDeUnaFamiliaOrdenadosPorNombre() {
		List<Ejercicio> sentadillas = ejercicios
				.findByFamiliaSustitucionAndOrigenEtiquetadoOrderByNombreEsAsc("sentadilla", "curado");

		assertThat(sentadillas).extracting(Ejercicio::getNombreEs).containsExactly(
				"Prensa de piernas",
				"Sentadilla con barra",
				"Sentadilla frontal con barra",
				"Sentadilla goblet",
				"Sentadilla hack en máquina");
	}

	@Test
	void guardaLosAtributosCuradosDeLaSentadillaConBarra() {
		Ejercicio e = ejercicios
				.findByFamiliaSustitucionAndOrigenEtiquetadoOrderByNombreEsAsc("sentadilla", "curado")
				.stream().filter(x -> "Barbell_Squat".equals(x.getIdExterno())).findFirst().orElseThrow();

		assertThat(e.getId()).isPositive();
		assertThat(e.getNombreOriginal()).isEqualTo("Barbell Squat");
		assertThat(e.getPatron().getCodigo()).isEqualTo("sentadilla");
		assertThat(e.getImplemento().getCodigo()).isEqualTo("barra");
		assertThat(e.getMecanica()).isEqualTo("compuesto");
		assertThat(e.getNivel()).isEqualTo("principiante");
		assertThat(e.getCategoria()).isEqualTo("strength");
		assertThat(e.isCargaAxial()).isTrue();
		assertThat(e.isImpacto()).isFalse();
		assertThat(e.getAgarre()).isEqualTo("ninguno");
		assertThat(e.getLateralidad()).isEqualTo("bilateral");
		assertThat(e.isPorEncimaCabeza()).isFalse();
		assertThat(e.isPosicionEstirada()).isFalse();
		assertThat(e.getRangoMovimiento()).isEqualTo("completo");
		assertThat(e.isAptoFuerza()).isTrue();
		assertThat(e.getOrigenEtiquetado()).isEqualTo("curado");
		assertThat(e.isRevisado()).isFalse();
		assertThat(e.getFuente()).isEqualTo("free-exercise-db");
		assertThat(e.getLicencia()).isEqualTo("Unlicense");
		assertThat(e.getAutor()).isNull();
		assertThat(e.getVersionFuente()).isEqualTo("f00c92c7dcf1216a928a52c3706c7ce8e2f71ed5");
		assertThat(e.getPatron().getNombre()).isEqualTo("Sentadilla");
		assertThat(e.getImplemento().getNombre()).isEqualTo("Barra");
		assertThat(e.getPatron().getId()).isPositive();
		assertThat(e.getImplemento().getId()).isPositive();
	}

	@Test
	void marcaLaPosicionEstiradaSoloDondeHayEvidencia() {
		List<String> estirados = jdbc.queryForList(
				"SELECT id_externo FROM ejercicio WHERE posicion_estirada = 1 AND origen_etiquetado = 'curado'",
				String.class);

		assertThat(estirados).contains("Seated_Leg_Curl", "Standing_Calf_Raises", "Incline_Dumbbell_Curl",
				"Triceps_Overhead_Extension_with_Rope", "Romanian_Deadlift", "Dumbbell_Flyes");
		// Sentado, el recto femoral está acortado: la extensión de cuádriceps no es posición estirada.
		assertThat(estirados).doesNotContain("Lying_Leg_Curls", "Seated_Calf_Raise", "Barbell_Curl", "Leg_Extensions");
		assertThat(contar("SELECT COUNT(*) FROM ejercicio WHERE posicion_estirada = 1 AND origen_etiquetado = 'derivado'"))
				.isZero();
	}

	@Test
	void volverAEjecutarLaSemillaNoRehaceLosEjerciciosNiSusIds() throws SQLException {
		List<Long> idsAntes = jdbc.queryForList("SELECT id FROM ejercicio ORDER BY id", Long.class);
		int musculosAntes = contar("SELECT COUNT(*) FROM ejercicio_musculo");
		Connection conexion = DataSourceUtils.getConnection(dataSource);

		ScriptUtils.executeSqlScript(conexion, new ClassPathResource(SEMILLA));

		assertThat(jdbc.queryForList("SELECT id FROM ejercicio ORDER BY id", Long.class)).isEqualTo(idsAntes);
		assertThat(contar("SELECT COUNT(*) FROM ejercicio_musculo")).isEqualTo(musculosAntes);
		assertThat(contar("SELECT COUNT(*) FROM ejercicio_region")).isPositive();
		assertThat(contar("SELECT COUNT(*) FROM ejercicio_material")).isEqualTo(EJERCICIOS_EN_LA_FUENTE);
	}

}
