package es.forja.cuenta.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

/** El barrido no corre en cada petición, respeta lo que aún frena y pone techo a la memoria. */
class BarridoTest {

	private final AtomicLong reloj = new AtomicLong(1_000_000_000_000L);
	private final Barrido barrido = new Barrido(reloj::get);

	private static Map<String, Boolean> mapa(int activas, int sobrantes) {
		Map<String, Boolean> m = new HashMap<>();
		for (int i = 0; i < activas; i++) {
			m.put("activa" + i, true);
		}
		for (int i = 0; i < sobrantes; i++) {
			m.put("sobra" + i, false);
		}
		return m;
	}

	@Test
	void porEncimaDelMaximoQuitaSoloLoQueNoFrenaYNoVuelveABarrerHastaPasados10s() {
		Map<String, Boolean> m = mapa(3, 5);

		barrido.siToca(m, 4, activa -> !activa);
		assertThat(m).hasSize(3).containsOnlyKeys("activa0", "activa1", "activa2");

		m.putAll(mapa(0, 5));
		barrido.siToca(m, 4, activa -> !activa);
		assertThat(m).hasSize(8);

		reloj.addAndGet(Barrido.CADA.toNanos());
		barrido.siToca(m, 4, activa -> !activa);
		assertThat(m).hasSize(3);
	}

	@Test
	void siTodoFrenaYPasaDelDobleRecortaHastaElMaximo() {
		Map<String, Boolean> m = mapa(9, 0);

		barrido.siToca(m, 4, activa -> !activa);

		assertThat(m).hasSize(4);
	}

	@Test
	void porDebajoDelMaximoNoHaceNada() {
		Map<String, Boolean> m = mapa(2, 2);

		barrido.siToca(m, 4, activa -> !activa);

		assertThat(m).hasSize(4);
	}
}
