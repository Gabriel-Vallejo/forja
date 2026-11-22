package es.forja.cuenta.servicio;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import es.forja.cuenta.modelo.ReglasCuenta;

/**
 * Fallos de contraseña por cuenta <b>y origen</b> (ADR 0009 §6, enmienda 2026-11-22). La espera tras 10 fallos solo
 * afecta a la IP que falló: con el bloqueo por cuenta, cualquiera que supiera un correo dejaba a su dueño sin poder
 * entrar hasta 1 h. El reparto entre muchas IPs lo frena el límite por correo de {@link LimitadorIntentos}.
 *
 * <p>shortcut: en memoria de esta instancia, como Bucket4j (se pierde al reiniciar y no se comparte entre réplicas);
 * con varias réplicas, a Redis o a una tabla.
 */
@Component
public class FallosLogin {

	static final int MAXIMO_DE_CLAVES = 100_000;
	/** Con IPv6 cada cliente tiene un /64 entero: se agrupa por prefijo para que rotar de IP no reinicie la cuenta. */
	private static final int BYTES_PREFIJO_IPV6 = 8;

	private record Estado(int fallos, Instant hasta) {
	}

	private final ConcurrentHashMap<String, Estado> estados = new ConcurrentHashMap<>();
	private final int maximo;
	private final Barrido barrido;

	public FallosLogin() {
		this(MAXIMO_DE_CLAVES);
	}

	FallosLogin(int maximo) {
		this(maximo, new Barrido());
	}

	FallosLogin(int maximo, Barrido barrido) {
		this.maximo = maximo;
		this.barrido = barrido;
	}

	public boolean enEspera(long usuario, String ip, Instant ahora) {
		Estado e = estados.get(clave(usuario, ip));
		return e != null && e.hasta() != null && ahora.isBefore(e.hasta());
	}

	/** Suma el fallo de forma atómica y, si toca, fija la espera según el contador resultante. */
	public void fallo(long usuario, String ip, Instant ahora) {
		// Solo se descartan los orígenes sin espera en curso: vaciarlo entero dejaría a quien llena el mapa a propósito
		// borrar las esperas activas. Las que quedan están acotadas por los límites de LimitadorIntentos.
		barrido.siToca(estados, maximo, e -> e.hasta() == null || !ahora.isBefore(e.hasta()));
		estados.compute(clave(usuario, ip), (k, e) -> {
			int fallos = (e == null ? 0 : e.fallos()) + 1;
			Duration espera = ReglasCuenta.esperaTrasFallos(fallos);
			return new Estado(fallos, espera.isZero() ? null : ahora.plus(espera));
		});
	}

	int tamano() {
		return estados.size();
	}

	public int fallos(long usuario, String ip) {
		Estado e = estados.get(clave(usuario, ip));
		return e == null ? 0 : e.fallos();
	}

	/** Un acierto desde ese origen lo pone a cero. */
	public void acierto(long usuario, String ip) {
		estados.remove(clave(usuario, ip));
	}

	/** Al cambiar la contraseña se olvidan los fallos de la cuenta desde cualquier origen. */
	public void olvidar(long usuario) {
		String prefijo = usuario + "|";
		estados.keySet().removeIf(k -> k.startsWith(prefijo));
	}

	private static String clave(long usuario, String ip) {
		return usuario + "|" + origen(ip);
	}

	static String origen(String ip) {
		if (ip == null || ip.indexOf(':') < 0) {
			return String.valueOf(ip);
		}
		try {
			byte[] bytes = InetAddress.getByName(ip).getAddress();
			if (bytes.length != 16) {
				return ip;
			}
			StringBuilder prefijo = new StringBuilder();
			for (int i = 0; i < BYTES_PREFIJO_IPV6; i++) {
				prefijo.append(String.format("%02x", bytes[i]));
			}
			return prefijo + "::/64";
		} catch (UnknownHostException e) {
			return ip;
		}
	}
}
