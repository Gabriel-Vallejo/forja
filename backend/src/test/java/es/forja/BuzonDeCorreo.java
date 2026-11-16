package es.forja;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import es.forja.cuenta.servicio.EnvioCorreo;

/** Buzón en memoria para leer el token que habría ido en el enlace del correo. */
public final class BuzonDeCorreo implements EnvioCorreo {

	public record Mensaje(String correo, String tipo, String token) {
	}

	private final List<Mensaje> enviados = Collections.synchronizedList(new ArrayList<>());

	@Override
	public void enviarVerificacion(String correo, String token) {
		enviados.add(new Mensaje(correo, "verificacion", token));
	}

	@Override
	public void enviarRecuperacion(String correo, String token) {
		enviados.add(new Mensaje(correo, "recuperacion", token));
	}

	public Optional<String> ultimoToken(String correo, String tipo) {
		synchronized (enviados) {
			return enviados.reversed().stream().filter(m -> m.correo().equals(correo) && m.tipo().equals(tipo))
					.map(Mensaje::token).findFirst();
		}
	}

	public int total() {
		return enviados.size();
	}

	public void vaciar() {
		enviados.clear();
	}

	@TestConfiguration(proxyBeanMethods = false)
	public static class Configuracion {

		@Bean
		@Primary
		BuzonDeCorreo buzonDeCorreo() {
			return new BuzonDeCorreo();
		}
	}
}
