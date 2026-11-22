package es.forja.restricciones.banderas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import es.forja.restricciones.banderas.BanderaRoja.Nivel;
import es.forja.restricciones.modelo.Region;

class BanderasRojasTest {

	private static final Pattern PROHIBIDAS = Pattern.compile(
			"rehabilit|alivia|corrige|previene lesiones|detecta (desequilibrios|atrofias)|seguro para tu lesi",
			Pattern.CASE_INSENSITIVE);

	private final BanderasRojas banderas = new BanderasRojas();

	/** Constructor de pruebas: todo a falso salvo lo que se active. */
	private static final class R {
		private Map<Region, Integer> dolor = Map.of();
		private boolean reposo, noche, hormigueo, fuerza, esfinteres, fiebre, trauma, peso, toracico, mareo;

		R dolor(Region region, int valor) {
			dolor = Map.of(region, valor);
			return this;
		}

		R reposo() { reposo = true; return this; }
		R noche() { noche = true; return this; }
		R hormigueo() { hormigueo = true; return this; }
		R fuerza() { fuerza = true; return this; }
		R esfinteres() { esfinteres = true; return this; }
		R fiebre() { fiebre = true; return this; }
		R trauma() { trauma = true; return this; }
		R peso() { peso = true; return this; }
		R toracico() { toracico = true; return this; }
		R mareo() { mareo = true; return this; }

		RespuestasGuiadas build() {
			return new RespuestasGuiadas(dolor, reposo, noche, hormigueo, fuerza, esfinteres, fiebre, trauma,
					peso, toracico, mareo);
		}
	}

	private List<BanderaRoja> evaluar(R r) {
		return banderas.evaluar(r.build());
	}

	@Test
	void sinRespuestasDeRiesgoNoHayBanderas() {
		assertThat(evaluar(new R())).isEmpty();
		assertThat(evaluar(new R().dolor(Region.HOMBRO, 6))).isEmpty();
		assertThat(BanderasRojas.bloqueaLaRutina(evaluar(new R()))).isFalse();
	}

	@Test
	void perdidaDeControlDeEsfinteresOAnestesiaEnSillaDeMontarEsUrgenciasHoy() {
		List<BanderaRoja> r = evaluar(new R().esfinteres());

		assertThat(r).singleElement().satisfies(b -> {
			assertThat(b.nivel()).isEqualTo(Nivel.URGENCIAS_HOY);
			assertThat(b.guia()).contains("IFOMPT");
		});
	}

	@Test
	void dolorToracicoEsUrgenciasHoySegunAcsm() {
		assertThat(evaluar(new R().toracico())).singleElement().satisfies(b -> {
			assertThat(b.nivel()).isEqualTo(Nivel.URGENCIAS_HOY);
			assertThat(b.guia()).contains("ACSM");
		});
	}

	@Test
	void mareoODesmayoEsUrgenciasHoySegunAcsm() {
		assertThat(evaluar(new R().mareo())).singleElement().satisfies(b -> {
			assertThat(b.nivel()).isEqualTo(Nivel.URGENCIAS_HOY);
			assertThat(b.guia()).contains("ACSM");
		});
	}

	@Test
	void perdidaDeFuerzaConHormigueoEsUnaSolaBanderaDeUrgenciasHoy() {
		assertThat(evaluar(new R().fuerza().hormigueo())).singleElement()
				.satisfies(b -> assertThat(b.nivel()).isEqualTo(Nivel.URGENCIAS_HOY));
	}

	@Test
	void perdidaDeFuerzaSolaEsProfesionalEnDias() {
		// Solo lo nuevo o lo que va a más: un déficit crónico ya valorado no es una bandera (decisión 2026-11-22).
		assertThat(evaluar(new R().fuerza())).singleElement().satisfies(b -> {
			assertThat(b.nivel()).isEqualTo(Nivel.PROFESIONAL_EN_DIAS);
			assertThat(b.motivo()).contains("nueva o que va a más");
			assertThat(b.guia()).contains("NICE NG59");
		});
	}

	@Test
	void hormigueoSoloEsProfesionalEnDias() {
		assertThat(evaluar(new R().hormigueo())).singleElement().satisfies(b -> {
			assertThat(b.nivel()).isEqualTo(Nivel.PROFESIONAL_EN_DIAS);
			assertThat(b.motivo()).contains("nuevos o que van a más");
			assertThat(b.guia()).contains("NICE NG59");
		});
	}

	@Test
	void fiebreConDolorDeEspaldaEsProfesionalEnDias() {
		assertThat(evaluar(new R().fiebre())).singleElement().satisfies(b -> {
			assertThat(b.nivel()).isEqualTo(Nivel.PROFESIONAL_EN_DIAS);
			assertThat(b.guia()).contains("IFOMPT");
		});
	}

	@Test
	void traumatismoRecienteSoloCuentaSiHayDolor() {
		assertThat(evaluar(new R().trauma())).isEmpty();
		assertThat(evaluar(new R().trauma().dolor(Region.RODILLA, 3))).singleElement()
				.satisfies(b -> assertThat(b.nivel()).isEqualTo(Nivel.PROFESIONAL_EN_DIAS));
	}

	@Test
	void perdidaDePesoSinExplicacionEsProfesionalEnDias() {
		assertThat(evaluar(new R().peso())).singleElement()
				.satisfies(b -> assertThat(b.nivel()).isEqualTo(Nivel.PROFESIONAL_EN_DIAS));
	}

	@Test
	void dolorQueDespiertaPorLaNocheEsProfesionalEnDias() {
		assertThat(evaluar(new R().noche())).singleElement()
				.satisfies(b -> assertThat(b.nivel()).isEqualTo(Nivel.PROFESIONAL_EN_DIAS));
	}

	@Test
	void dolorEnReposoSoloCuentaSiHayDolorEnAlgunaZona() {
		assertThat(evaluar(new R().reposo())).isEmpty();
		assertThat(evaluar(new R().reposo().dolor(Region.COLUMNA_LUMBAR, 2))).singleElement()
				.satisfies(b -> assertThat(b.nivel()).isEqualTo(Nivel.PROFESIONAL_EN_DIAS));
	}

	@Test
	void unaSolaBanderaBastaParaBloquearLaRutinaYLasUrgentesVanPrimero() {
		List<BanderaRoja> r = evaluar(new R().noche().toracico());

		assertThat(BanderasRojas.bloqueaLaRutina(r)).isTrue();
		assertThat(r).extracting(BanderaRoja::nivel)
				.containsExactly(Nivel.URGENCIAS_HOY, Nivel.PROFESIONAL_EN_DIAS);
		assertThat(BanderasRojas.bloqueaLaRutina(evaluar(new R().noche()))).isTrue();
	}

	@Test
	void todasLasBanderasLlevanMotivoGuiaYUnLenguajeDeFitness() {
		List<BanderaRoja> todas = evaluar(new R().esfinteres().toracico().mareo().fuerza().hormigueo()
				.fiebre().trauma().peso().noche().reposo().dolor(Region.HOMBRO, 5));

		assertThat(todas).isNotEmpty().allSatisfy(b -> {
			assertThat(b.motivo()).isNotBlank();
			assertThat(b.guia()).isNotBlank();
			assertThat(PROHIBIDAS.matcher(b.motivo()).find()).isFalse();
		});
	}

	@Test
	void validaElDolorEntreCeroYDiez() {
		assertThatThrownBy(() -> new R().dolor(Region.HOMBRO, 11).build())
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("0 y 10");
		assertThatThrownBy(() -> new R().dolor(Region.HOMBRO, -1).build())
				.isInstanceOf(IllegalArgumentException.class);
	}

}
