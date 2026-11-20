package es.forja.entreno.rutina.solver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import es.forja.PruebaConMysql;
import es.forja.entreno.rutina.modelo.EntradaRutina;
import es.forja.entreno.rutina.modelo.PlanRutina;
import es.forja.entreno.rutina.servicio.ServicioRutina;

/** Preferencias de los ajustes del plan en el solver: series fijadas (duras) y «más variedad» (blanda). */
@PruebaConMysql
class GeneradorPreferenciasTest {

	private static final GeneradorRutina GENERADOR = new GeneradorRutina(ConfiguracionDePrueba.estandar());

	@Autowired
	private ServicioRutina servicio;

	private PlanRutina generar(Map<String, Integer> series, Set<String> evitar) {
		return GENERADOR.generar(servicio.catalogo(), new EntradaRutina(3, 60, GeneradorCatalogoRealTest.MATERIAL_COMPLETO,
				Set.of(), List.of(), EntradaRutina.FRECUENCIA_POR_DEFECTO, List.of(), Map.of(), series, evitar,
				EntradaRutina.Partida.NINGUNA));
	}

	private static Map<String, Integer> seriesSemana(PlanRutina plan) {
		Map<String, Integer> series = new TreeMap<>();
		plan.dias().forEach(d -> d.ejercicios().forEach(e -> series.merge(e.idExterno(), e.series(), Integer::sum)));
		return series;
	}

	@Test
	void lasSeriesFijadasSeRespetanYSiNoCabenElPlanSaleSinEllasYLoDice() {
		PlanRutina base = generar(Map.of(), Set.of());
		String id = seriesSemana(base).keySet().iterator().next();
		int antes = seriesSemana(base).get(id);

		PlanRutina conUnaMas = generar(Map.of(id, antes + 1), Set.of());
		// 30 series de un ejercicio en 3 días pasa del tope de 5 por sesión: inviable.
		PlanRutina imposible = generar(Map.of(id, 30), Set.of());

		assertThat(seriesSemana(conUnaMas).get(id)).isEqualTo(antes + 1);
		assertThat(conUnaMas.notas()).doesNotContain(Textos.SERIES_NO_CABEN);
		assertThat(imposible.notas()).contains(Textos.SERIES_NO_CABEN);
		assertThat(seriesSemana(imposible).get(id)).isNotEqualTo(30);
	}

	@Test
	void conVariedadLosEjerciciosDelPlanAnteriorPierdenPuntosYSalenMenos() {
		PlanRutina base = generar(Map.of(), Set.of());
		Set<String> anteriores = seriesSemana(base).keySet();

		PlanRutina variado = generar(Map.of(), anteriores);

		assertThat(seriesSemana(variado).keySet().stream().filter(anteriores::contains).count())
				.isLessThan(anteriores.size());
		assertThat(variado.contribuciones()).filteredOn(c -> anteriores.contains(c.idExterno()))
				.allSatisfy(c -> assertThat(c.terminos().get(Terminos.REPETIR_PLAN)).isNegative());
	}
}
