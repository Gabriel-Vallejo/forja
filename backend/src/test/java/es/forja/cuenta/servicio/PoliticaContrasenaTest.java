package es.forja.cuenta.servicio;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PoliticaContrasenaTest {

	private final PoliticaContrasena politica = new PoliticaContrasena(c -> c.equals("contrasena-filtrada"));

	@Test
	void aceptaUnaContrasenaLargaQueNoEstaFiltrada() {
		assertThatCode(() -> politica.exigir("una-frase-larga-y-propia")).doesNotThrowAnyException();
	}

	@Test
	void rechazaUnaContrasenaDeMenosDeDoceCaracteres() {
		assertThatThrownBy(() -> politica.exigir("corta-11-ch")).isInstanceOf(ErrorCuenta.class)
				.hasMessageContaining("al menos 12");
	}

	@Test
	void rechazaUnaContrasenaDeMasDeCientoVeintiochoCaracteres() {
		assertThatThrownBy(() -> politica.exigir("x".repeat(129))).isInstanceOf(ErrorCuenta.class)
				.hasMessageContaining("128");
	}

	@Test
	void rechazaUnaContrasenaDeLaListaFiltrada() {
		assertThatThrownBy(() -> politica.exigir("contrasena-filtrada")).isInstanceOf(ErrorCuenta.class)
				.hasMessageContaining("filtraciones");
	}

	@Test
	void rechazaUnaContrasenaNula() {
		assertThatThrownBy(() -> politica.exigir(null)).isInstanceOf(ErrorCuenta.class);
	}
}
