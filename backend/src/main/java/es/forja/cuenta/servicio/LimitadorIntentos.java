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
 * entre réplicas). Con varias instancias, a Redis. Si el mapa pasa de {@link #MAXIMO_DE_CLAVES}, un barrido (como
 * mucho cada {@link Barrido#CADA}) descarta los cubos que ya se han vuelto a llenar; solo por encima del doble se
 * descartan también cubos a medias, que es lo único que un ataque muy grande puede forzar.
 */
@Component
public class LimitadorIntentos {

	public static final int INTENTOS_POR_IP = 30;
	public static final Duration VENTANA_POR_IP = Duration.ofMinutes(1);
	public static final int INTENTOS_POR_CORREO = 20;
	public static final Duration VENTANA_POR_CORREO = Duration.ofMinutes(15);
	static final int MAXIMO_DE_CLAVES = 100_000;

	/** El cubo y su capacidad: hace falta para saber si ya se ha vuelto a llenar al desalojar. */
	private record Cubo(Bucket cubo, long capacidad) {

		boolean lleno() {
			return cubo.getAvailableTokens() >= capacidad;
		}
	}

	private final ConcurrentHashMap<String, Cubo> cubos = new ConcurrentHashMap<>();
	private final Barrido barrido = new Barrido();

	/** Gasta un intento de esta acción para la IP y para el correo; lanza 429 si alguno se agotó. */
	public void exigir(String accion, String ip, String correo) {
		gastar("ip|" + accion + "|" + FallosLogin.origen(ip), INTENTOS_POR_IP, VENTANA_POR_IP);
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

	int tamano() {
		return cubos.size();
	}

	private void gastar(String clave, int capacidad, Duration ventana) {
		barrido.siToca(cubos, MAXIMO_DE_CLAVES, Cubo::lleno);
		Bucket cubo = cubos.computeIfAbsent(clave, k -> new Cubo(Bucket.builder()
				.addLimit(Bandwidth.builder().capacity(capacidad).refillIntervally(capacidad, ventana).build())
				.build(), capacidad)).cubo();
		ConsumptionProbe prueba = cubo.tryConsumeAndReturnRemaining(1);
		if (!prueba.isConsumed()) {
			throw ErrorCuenta.demasiadasPeticiones(Duration.ofNanos(prueba.getNanosToWaitForRefill()));
		}
	}
}
