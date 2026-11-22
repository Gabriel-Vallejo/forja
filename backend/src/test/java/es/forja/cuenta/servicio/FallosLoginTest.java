package es.forja.cuenta.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

/** La espera va por cuenta y origen; con IPv6 el origen es el /64. */
class FallosLoginTest {

	private static final Instant AHORA = Instant.parse("2026-11-22T10:00:00Z");

	@Test
	void conIpv6RotarDentroDelMismo64NoReiniciaLosFallos() {
		FallosLogin fallos = new FallosLogin();
		for (int i = 0; i < 10; i++) {
			fallos.fallo(1, "2001:db8:aa:bb::" + Integer.toHexString(i + 1), AHORA);
		}

		assertThat(fallos.enEspera(1, "2001:db8:aa:bb:ffff::9", AHORA)).isTrue();
		assertThat(fallos.enEspera(1, "2001:db8:aa:cc::1", AHORA)).isFalse();
	}

	@Test
	void conIpv4CadaIpVaAparteYOtraCuentaNoSeEntera() {
		FallosLogin fallos = new FallosLogin();
		for (int i = 0; i < 10; i++) {
			fallos.fallo(1, "203.0.113.7", AHORA);
		}

		assertThat(fallos.enEspera(1, "203.0.113.7", AHORA)).isTrue();
		assertThat(fallos.enEspera(1, "203.0.113.8", AHORA)).isFalse();
		assertThat(fallos.enEspera(2, "203.0.113.7", AHORA)).isFalse();
	}

	@Test
	void conElMapaLlenoSoloSeDescartanLosOrigenesSinEsperaYLaActivaSigue() {
		FallosLogin fallos = new FallosLogin(3);
		for (int i = 0; i < 10; i++) {
			fallos.fallo(1, "203.0.113.7", AHORA);
		}
		for (int i = 0; i < 4; i++) {
			fallos.fallo(100 + i, "198.51.100." + i, AHORA);
		}

		assertThat(fallos.enEspera(1, "203.0.113.7", AHORA)).isTrue();
		assertThat(fallos.tamano()).isLessThanOrEqualTo(3);
	}

	@Test
	void olvidarBorraLosFallosDeLaCuentaDesdeCualquierOrigen() {
		FallosLogin fallos = new FallosLogin();
		fallos.fallo(1, "203.0.113.7", AHORA);
		fallos.fallo(1, "198.51.100.1", AHORA);
		fallos.fallo(2, "203.0.113.7", AHORA);

		fallos.olvidar(1);

		assertThat(fallos.fallos(1, "203.0.113.7")).isZero();
		assertThat(fallos.fallos(1, "198.51.100.1")).isZero();
		assertThat(fallos.fallos(2, "203.0.113.7")).isEqualTo(1);
	}
}
