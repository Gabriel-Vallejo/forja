package es.forja.entreno.plan.modelo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import es.forja.entreno.plan.modelo.GrupoMuscular.MusculoCatalogo;
import es.forja.entreno.plan.modelo.GrupoMuscular.MusculoEjercicio;

class GrupoMuscularTest {

	@ParameterizedTest
	@CsvSource({ "biceps,BRAZOS", "abdominales,ABDOMEN", "isquiotibiales,ISQUIOS", "lumbares,LUMBAR",
			"dorsales,DORSAL", "espalda_media,DORSAL", "cuello,TRAPECIO", "trapecio,TRAPECIO",
			"abductores,GLUTEOS", "gluteos,GLUTEOS", "pecho,PECHO" })
	void traduceElMusculoDelCatalogoASuGrupoDelCuerpo(String musculo, GrupoMuscular esperado) {
		assertThat(GrupoMuscular.delCatalogo(musculo)).isEqualTo(esperado);
	}

	@Test
	void unMusculoSinGrupoFallaEnVozAlta() {
		assertThatThrownBy(() -> GrupoMuscular.delCatalogo("inventado")).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("inventado");
	}

	@Test
	void agruparNoRepiteGruposYPonePrimariosAntes() {
		List<MusculoEjercicio> grupos = GrupoMuscular.agrupar(List.of(
				new MusculoCatalogo("triceps", RolMuscular.SECUNDARIO),
				new MusculoCatalogo("dorsales", RolMuscular.PRIMARIO),
				new MusculoCatalogo("espalda_media", RolMuscular.PRIMARIO),
				new MusculoCatalogo("hombros", RolMuscular.SECUNDARIO)));

		assertThat(grupos).containsExactly(new MusculoEjercicio(GrupoMuscular.DORSAL, RolMuscular.PRIMARIO),
				new MusculoEjercicio(GrupoMuscular.TRICEPS, RolMuscular.SECUNDARIO),
				new MusculoEjercicio(GrupoMuscular.HOMBROS, RolMuscular.SECUNDARIO));
	}

	@Test
	void siUnGrupoEsPrimarioYSecundarioSeQuedaComoPrimario() {
		List<MusculoEjercicio> grupos = GrupoMuscular.agrupar(List.of(
				new MusculoCatalogo("gluteos", RolMuscular.SECUNDARIO),
				new MusculoCatalogo("abductores", RolMuscular.PRIMARIO)));

		assertThat(grupos).containsExactly(new MusculoEjercicio(GrupoMuscular.GLUTEOS, RolMuscular.PRIMARIO));
	}
}
