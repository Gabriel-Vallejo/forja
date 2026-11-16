package es.forja;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Reloj que arranca en la hora real y se puede adelantar, para probar caducidades sin esperar. */
public final class RelojDePrueba extends Clock {

	private volatile Duration desfase = Duration.ZERO;

	public void avanzar(Duration tiempo) {
		desfase = desfase.plus(tiempo);
	}

	public void reiniciar() {
		desfase = Duration.ZERO;
	}

	@Override
	public ZoneId getZone() {
		return ZoneOffset.UTC;
	}

	@Override
	public Clock withZone(ZoneId zona) {
		return this;
	}

	@Override
	public Instant instant() {
		return Instant.now().plus(desfase);
	}

	@TestConfiguration(proxyBeanMethods = false)
	public static class Configuracion {

		@Bean
		@Primary
		RelojDePrueba relojDePrueba() {
			return new RelojDePrueba();
		}
	}
}
