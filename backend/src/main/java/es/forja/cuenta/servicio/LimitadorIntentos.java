package es.forja.cuenta.servicio;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;

/**
 * Límite de intentos por IP y por correo en login, registro y recuperación (ADR 0009 §6).
 *
 * <p>shortcut: los contadores viven en memoria de esta instancia (se pierden al reiniciar y no se comparten
 * entre réplicas) y, si el mapa pasa de {@link #MAXIMO_DE_CLAVES}, se vacía entero; con varias instancias
 * habría que mover Bucket4j a Redis.
 */
@Component
public class LimitadorIntentos {

	public static final int INTENTOS_POR_IP = 30;
	public static final Duration VENTANA_POR_IP = Duration.ofMinutes(1);
	public static final int INTENTOS_POR_CORREO = 20;
	public static final Duration VENTANA_POR_CORREO = Duration.ofMinutes(15);
	static final int MAXIMO_DE_CLAVES = 100_000;

	private final ConcurrentHashMap<String, Bucket> cubos = new ConcurrentHashMap<>();

	/** Gasta un intento de esta acción para la IP y para el correo; lanza 429 si alguno se agotó. */
	public void exigir(String accion, String ip, String correo) {
		gastar("ip|" + accion + "|" + ip, INTENTOS_POR_IP, VENTANA_POR_IP);
		gastar("correo|" + accion + "|" + correo, INTENTOS_POR_CORREO, VENTANA_POR_CORREO);
	}

	/** Gasta un intento solo por clave (sin IP), para operaciones ya autenticadas. */
	public void exigirPorClave(String accion, String clave) {
		exigirPorClave(accion, clave, INTENTOS_POR_CORREO, VENTANA_POR_CORREO);
	}

	/** Igual, con un límite propio de la acción. */
	public void exigirPorClave(String accion, String clave, int capacidad, Duration ventana) {
		gastar("clave|" + accion + "|" + clave, capacidad, ventana);
	}

	private void gastar(String clave, int capacidad, Duration ventana) {
		if (cubos.size() > MAXIMO_DE_CLAVES) {
			cubos.clear();
		}
		Bucket cubo = cubos.computeIfAbsent(clave, k -> Bucket.builder()
				.addLimit(Bandwidth.builder().capacity(capacidad).refillIntervally(capacidad, ventana).build())
				.build());
		ConsumptionProbe prueba = cubo.tryConsumeAndReturnRemaining(1);
		if (!prueba.isConsumed()) {
			throw ErrorCuenta.demasiadasPeticiones(Duration.ofNanos(prueba.getNanosToWaitForRefill()));
		}
	}
}
