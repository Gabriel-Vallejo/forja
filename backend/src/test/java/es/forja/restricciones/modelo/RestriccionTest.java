package es.forja.restricciones.modelo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RestriccionTest {

	private static Restriccion con(Region region, Patron patron, TipoRestriccion tipo, Estado estado) {
		return new Restriccion(region, Lado.AMBOS, patron, tipo, null, estado, Origen.USUARIO, null);
	}

	@Test
	void aceptaSoloRegion() {
		Restriccion r = con(Region.HOMBRO, null, TipoRestriccion.EVITAR, Estado.ACTUAL);

		assertThat(r.region()).isEqualTo(Region.HOMBRO);
		assertThat(r.patron()).isNull();
	}

	@Test
	void aceptaSoloPatron() {
		Restriccion r = con(null, Patron.SENTADILLA, TipoRestriccion.SIN_CARGA_AXIAL, Estado.ACTUAL);

		assertThat(r.patron()).isEqualTo(Patron.SENTADILLA);
	}

	@Test
	void rechazaRestriccionSinRegionNiPatron() {
		assertThatThrownBy(() -> con(null, null, TipoRestriccion.EVITAR, Estado.ACTUAL))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("región o un patrón");
	}

	@Test
	void rechazaCamposObligatoriosNulos() {
		assertThatThrownBy(() -> new Restriccion(Region.RODILLA, null, null, TipoRestriccion.EVITAR, null,
				Estado.ACTUAL, Origen.USUARIO, null))
				.isInstanceOf(NullPointerException.class).hasMessageContaining("lado");
		assertThatThrownBy(() -> new Restriccion(Region.RODILLA, Lado.AMBOS, null, null, null,
				Estado.ACTUAL, Origen.USUARIO, null))
				.isInstanceOf(NullPointerException.class).hasMessageContaining("tipo");
		assertThatThrownBy(() -> new Restriccion(Region.RODILLA, Lado.AMBOS, null, TipoRestriccion.EVITAR, null,
				null, Origen.USUARIO, null))
				.isInstanceOf(NullPointerException.class).hasMessageContaining("estado");
		assertThatThrownBy(() -> new Restriccion(Region.RODILLA, Lado.AMBOS, null, TipoRestriccion.EVITAR, null,
				Estado.ACTUAL, null, null))
				.isInstanceOf(NullPointerException.class).hasMessageContaining("origen");
	}

	@Test
	void elHistoricoSinLimitacionNoRestringeNada() {
		assertThat(con(Region.RODILLA, null, TipoRestriccion.EVITAR, Estado.HISTORICO_SIN_LIMITACION).restringe())
				.isFalse();
		assertThat(con(Region.RODILLA, null, TipoRestriccion.EVITAR, Estado.ACTUAL).restringe()).isTrue();
		assertThat(con(Region.RODILLA, null, TipoRestriccion.EVITAR, Estado.ULTIMOS_12_MESES).restringe()).isTrue();
	}

	@Test
	void losCodigosDeLosEnumsCoincidenConLasTablasDelCatalogo() {
		assertThat(Region.COLUMNA_LUMBAR.codigo()).isEqualTo("columna_lumbar");
		assertThat(Patron.EMPUJE_VERTICAL.codigo()).isEqualTo("empuje_vertical");
		assertThat(Patron.values()).hasSize(14);
		assertThat(Region.values()).hasSize(8);
		assertThat(TipoRestriccion.SIN_POR_ENCIMA_CABEZA.codigo()).isEqualTo("sin_por_encima_cabeza");
		assertThat(Estado.HISTORICO_SIN_LIMITACION.codigo()).isEqualTo("historico_sin_limitacion");
	}

	@Test
	void resuelveUnCodigoYRechazaUnoDesconocido() {
		assertThat(Region.desdeCodigo("muneca")).isEqualTo(Region.MUNECA);
		assertThat(Patron.desdeCodigo("traccion_vertical")).isEqualTo(Patron.TRACCION_VERTICAL);
		assertThatThrownBy(() -> Region.desdeCodigo("dedo_gordo"))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("dedo_gordo");
	}

	@Test
	void losNombresVisiblesLlevanTilde() {
		assertThat(Region.MUNECA.nombre()).isEqualTo("muñeca");
		assertThat(Patron.TRACCION_HORIZONTAL.nombre()).isEqualTo("tracción horizontal");
	}

}
