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
	void sinRutaUsaLaListaIncluidaDeContrasenasComunesDe12OMas() {
		for (ArchivoContrasenasFiltradas lista : java.util.List.of(new ArchivoContrasenasFiltradas(""),
				new ArchivoContrasenasFiltradas(null))) {
			assertThat(lista.contiene("123qweasdzxc")).isTrue();
			assertThat(lista.contiene("1Q2W3E4R5T6Y")).isTrue();
			assertThat(lista.contiene("una-contrasena-larga-1")).isFalse();
		}
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
