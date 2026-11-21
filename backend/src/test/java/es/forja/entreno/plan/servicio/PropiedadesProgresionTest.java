package es.forja.entreno.plan.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;

import es.forja.entreno.plan.modelo.ParametrosProgresion;
import es.forja.entreno.plan.servicio.PropiedadesProgresion.Objetivo;

/** El objetivo de repeticiones de cada ejercicio: el suyo propio si lo tiene, si no el de su tipo. */
class PropiedadesProgresionTest {

	private static final Objetivo COMPUESTOS = new Objetivo(6, 10, 2);
	private static final Objetivo AISLADOS = new Objetivo(10, 15, 1);
	private static final Objetivo FACE_PULL = new Objetivo(12, 20, 1);

	private static PropiedadesProgresion con(Map<String, Objetivo> porEjercicio) {
		return new PropiedadesProgresion(COMPUESTOS, AISLADOS, Map.of(), BigDecimal.ZERO,
				new ParametrosProgresion.Deload(3, 3, BigDecimal.ONE, new BigDecimal("0.9"), new BigDecimal("0.5"),
						new BigDecimal("0.5")),
				12, 20, 3, porEjercicio);
	}

	@Test
	void unEjercicioConRangoPropioLoUsaYElRestoElDeSuTipo() {
		PropiedadesProgresion p = con(Map.of("Face_Pull", FACE_PULL));

		assertThat(p.objetivo("Face_Pull", false)).isEqualTo(FACE_PULL);
		assertThat(p.objetivo("Face_Pull", true)).isEqualTo(FACE_PULL);
		assertThat(p.objetivo("Curl", false)).isEqualTo(AISLADOS);
		assertThat(p.objetivo("Sentadilla", true)).isEqualTo(COMPUESTOS);
	}

	@Test
	void sinMapaPorEjercicioTodosVanPorSuTipo() {
		assertThat(con(null).objetivo("Face_Pull", false)).isEqualTo(AISLADOS);
	}
}
