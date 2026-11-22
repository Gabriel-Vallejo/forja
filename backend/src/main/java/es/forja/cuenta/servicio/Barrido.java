package es.forja.cuenta.servicio;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.function.Predicate;

/**
 * Desalojo de los mapas en memoria de los límites de login. Se recorre el mapa como mucho una vez cada {@link #CADA}
 * (no en cada petición: con el mapa lleno, cada login recorrería 100 000 entradas) y solo cuando pasa del máximo.
 * Primero se quitan las entradas que ya no frenan nada; si aun así pasa del doble, se quitan otras hasta volver al
 * máximo, para que la memoria tenga techo aunque eso libere a algunos atacantes de un ataque muy grande.
 */
final class Barrido {

	static final Duration CADA = Duration.ofSeconds(10);

	private final AtomicLong ultimo;
	private final LongSupplier nanos;

	Barrido() {
		this(System::nanoTime);
	}

	Barrido(LongSupplier nanos) {
		this.nanos = nanos;
		this.ultimo = new AtomicLong(nanos.getAsLong() - CADA.toNanos());
	}

	<V> void siToca(Map<String, V> mapa, int maximo, Predicate<V> sobra) {
		if (mapa.size() <= maximo) {
			return;
		}
		long ahora = nanos.getAsLong();
		long antes = ultimo.get();
		if (ahora - antes < CADA.toNanos() || !ultimo.compareAndSet(antes, ahora)) {
			return;
		}
		mapa.values().removeIf(sobra);
		if (mapa.size() > 2L * maximo) {
			Iterator<String> claves = mapa.keySet().iterator();
			while (mapa.size() > maximo && claves.hasNext()) {
				claves.next();
				claves.remove();
			}
		}
	}
}
