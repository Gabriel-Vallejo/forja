package es.forja.comun.cifrado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class CifradorTest {

	private static final SecureRandom AZAR = new SecureRandom();
	private static final String TABLA = "restriccion";
	private static final String COLUMNA = "datos";
	private static final byte[] CLARO = "dato sintético de prueba".getBytes(StandardCharsets.UTF_8);

	private static byte[] clave() {
		byte[] clave = new byte[32];
		AZAR.nextBytes(clave);
		return clave;
	}

	private final byte[] claveActual = clave();
	private final Cifrador cifrador = new Cifrador("k2", claveActual, Map.of());
	private final UUID fila = UUID.randomUUID();

	@Test
	void descifraLoQueCifra() {
		byte[] valor = cifrador.cifrar(TABLA, COLUMNA, fila, CLARO);

		assertThat(cifrador.descifrar(TABLA, COLUMNA, fila, valor)).isEqualTo(CLARO);
	}

	@Test
	void elValorLlevaVersionKidYNoContieneElTextoEnClaro() {
		byte[] valor = cifrador.cifrar(TABLA, COLUMNA, fila, CLARO);

		assertThat(valor[0]).isEqualTo((byte) 1);
		assertThat(valor[1]).isEqualTo((byte) 2);
		assertThat(new String(valor, 2, 2, StandardCharsets.UTF_8)).isEqualTo("k2");
		assertThat(new String(valor, StandardCharsets.ISO_8859_1)).doesNotContain("sint");
	}

	@Test
	void dosCifradosDelMismoTextoSonDistintos() {
		byte[] uno = cifrador.cifrar(TABLA, COLUMNA, fila, CLARO);
		byte[] otro = cifrador.cifrar(TABLA, COLUMNA, fila, CLARO);

		assertThat(uno).isNotEqualTo(otro);
	}

	@Test
	void unByteCambiadoNoSeDescifra() {
		byte[] valor = cifrador.cifrar(TABLA, COLUMNA, fila, CLARO);
		for (int posicion = 0; posicion < valor.length; posicion++) {
			byte[] roto = valor.clone();
			roto[posicion] ^= 0x01;
			assertThatThrownBy(() -> cifrador.descifrar(TABLA, COLUMNA, fila, roto))
					.isInstanceOf(ErrorDeCifrado.class);
		}
	}

	@Test
	void unValorTruncadoOVacioNoSeDescifra() {
		byte[] valor = cifrador.cifrar(TABLA, COLUMNA, fila, CLARO);

		assertThatThrownBy(() -> cifrador.descifrar(TABLA, COLUMNA, fila, new byte[0]))
				.isInstanceOf(ErrorDeCifrado.class);
		assertThatThrownBy(() -> cifrador.descifrar(TABLA, COLUMNA, fila, Arrays.copyOf(valor, 5)))
				.isInstanceOf(ErrorDeCifrado.class);
	}

	@Test
	void copiadoAOtraFilaColumnaOTablaNoSeDescifra() {
		byte[] valor = cifrador.cifrar(TABLA, COLUMNA, fila, CLARO);

		assertThatThrownBy(() -> cifrador.descifrar(TABLA, COLUMNA, UUID.randomUUID(), valor))
				.isInstanceOf(ErrorDeCifrado.class);
		assertThatThrownBy(() -> cifrador.descifrar(TABLA, "otra", fila, valor)).isInstanceOf(ErrorDeCifrado.class);
		assertThatThrownBy(() -> cifrador.descifrar("otra", COLUMNA, fila, valor)).isInstanceOf(ErrorDeCifrado.class);
	}

	@Test
	void descifraConUnaClaveAnteriorYCifraSiempreConLaActual() {
		byte[] antigua = clave();
		Cifrador viejo = new Cifrador("k1", antigua, Map.of());
		byte[] valorViejo = viejo.cifrar(TABLA, COLUMNA, fila, CLARO);
		Cifrador rotado = new Cifrador("k2", claveActual, Map.of("k1", antigua));

		assertThat(rotado.descifrar(TABLA, COLUMNA, fila, valorViejo)).isEqualTo(CLARO);
		byte[] nuevo = rotado.cifrar(TABLA, COLUMNA, fila, CLARO);
		assertThat(new String(nuevo, 2, 2, StandardCharsets.UTF_8)).isEqualTo("k2");
		assertThat(cifrador.descifrar(TABLA, COLUMNA, fila, nuevo)).isEqualTo(CLARO);
	}

	@Test
	void unKidDesconocidoNoSeDescifra() {
		byte[] valorViejo = new Cifrador("k1", clave(), Map.of()).cifrar(TABLA, COLUMNA, fila, CLARO);

		assertThatThrownBy(() -> cifrador.descifrar(TABLA, COLUMNA, fila, valorViejo))
				.isInstanceOf(ErrorDeCifrado.class);
	}

	@Test
	void unaVersionDesconocidaNoSeDescifra() {
		byte[] valor = cifrador.cifrar(TABLA, COLUMNA, fila, CLARO);
		valor[0] = 2;

		assertThatThrownBy(() -> cifrador.descifrar(TABLA, COLUMNA, fila, valor)).isInstanceOf(ErrorDeCifrado.class);
	}

	@Test
	void elMensajeDelErrorNoLlevaNiElValorNiLaClave() {
		byte[] valor = cifrador.cifrar(TABLA, COLUMNA, fila, CLARO);
		valor[valor.length - 1] ^= 0x01;
		String claveEnBase64 = java.util.Base64.getEncoder().encodeToString(claveActual);

		assertThatThrownBy(() -> cifrador.descifrar(TABLA, COLUMNA, fila, valor))
				.satisfies(e -> assertThat(e.getMessage()).doesNotContain(claveEnBase64).doesNotContain("k2")
						.doesNotContain(TABLA).doesNotContain(fila.toString()));
	}

	@Test
	void unaClaveQueNoMide32BytesNoSeAcepta() {
		assertThatThrownBy(() -> new Cifrador("k", new byte[16], Map.of()))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new Cifrador("k", claveActual, Map.of("k0", new byte[31])))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void elKidDebeSerUtilYNoRepetirse() {
		assertThatThrownBy(() -> new Cifrador(" ", claveActual, Map.of())).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new Cifrador("k", claveActual, Map.of("k", clave())))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
