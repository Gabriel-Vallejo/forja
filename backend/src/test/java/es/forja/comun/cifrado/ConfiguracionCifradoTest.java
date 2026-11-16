package es.forja.comun.cifrado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ConfiguracionCifradoTest {

	private static final SecureRandom AZAR = new SecureRandom();
	private final ConfiguracionCifrado configuracion = new ConfiguracionCifrado();

	private static String claveBase64() {
		byte[] clave = new byte[32];
		AZAR.nextBytes(clave);
		return Base64.getEncoder().encodeToString(clave);
	}

	@Test
	void construyeElCifradorConLaActualYLasAnteriores() {
		String anterior = claveBase64();
		Cifrador viejo = configuracion.cifrador(new PropiedadesCifrado("k1", anterior, null));
		UUID fila = UUID.randomUUID();
		byte[] valor = viejo.cifrar("t", "c", fila, new byte[] { 1, 2, 3 });

		Cifrador nuevo = configuracion.cifrador(new PropiedadesCifrado("k2", claveBase64(), "k1:" + anterior));

		assertThat(nuevo.descifrar("t", "c", fila, valor)).containsExactly(1, 2, 3);
	}

	@Test
	void sinClaveActualOMalFormadaNoArranca() {
		assertThatThrownBy(() -> configuracion.cifrador(new PropiedadesCifrado("k", "", null)))
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> configuracion.cifrador(new PropiedadesCifrado("k", "no es base64!!", null)))
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> configuracion.cifrador(
				new PropiedadesCifrado("k", Base64.getEncoder().encodeToString(new byte[16]), null)))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void unaAnteriorMalFormadaNoArranca() {
		assertThatThrownBy(() -> configuracion.cifrador(new PropiedadesCifrado("k", claveBase64(), "sin-dos-puntos")))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void elToStringNoMuestraLaClave() {
		String clave = claveBase64();

		assertThat(new PropiedadesCifrado("k", clave, clave).toString()).doesNotContain(clave);
	}
}
