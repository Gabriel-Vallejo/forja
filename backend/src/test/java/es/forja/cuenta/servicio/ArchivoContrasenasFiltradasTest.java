package es.forja.cuenta.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArchivoContrasenasFiltradasTest {

	@TempDir
	Path carpeta;

	@Test
	void sinRutaLaListaEstaVaciaYNoRechazaNada() {
		assertThat(new ArchivoContrasenasFiltradas("").contiene("password1234")).isFalse();
		assertThat(new ArchivoContrasenasFiltradas(null).contiene("password1234")).isFalse();
	}

	@Test
	void leeUnaContrasenaPorLineaSinDistinguirMayusculas() throws Exception {
		Path archivo = Files.write(carpeta.resolve("lista.txt"), List.of("Password1234", "", "  qwertyuiop12  "));

		var lista = new ArchivoContrasenasFiltradas(archivo.toString());

		assertThat(lista.contiene("password1234")).isTrue();
		assertThat(lista.contiene("QWERTYUIOP12")).isTrue();
		assertThat(lista.contiene("otra-contrasena")).isFalse();
	}

	@Test
	void unaRutaQueNoExisteImpideArrancar() {
		assertThatThrownBy(() -> new ArchivoContrasenasFiltradas(carpeta.resolve("no-existe.txt").toString()))
				.isInstanceOf(UncheckedIOException.class);
	}
}
