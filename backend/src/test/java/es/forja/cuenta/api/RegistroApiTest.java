package es.forja.cuenta.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import es.forja.ClavesDePrueba;

class RegistroApiTest extends ApiCuentaSoporte {

	private static final String RUTA = "/api/v1/auth/registro";

	@Test
	void unaAltaNuevaGuardaSoloElAnioDeNacimientoYUnHashArgon2id() throws Exception {
		String correo = correoNuevo();

		enviar(RUTA, cuerpoRegistro(correo.toUpperCase(), CONTRASENA, "1998-04-12"))
				.andExpect(status().isAccepted());

		var fila = jdbc.sql("SELECT anio_nacimiento, mayor_de_edad, verificado, hash_contrasena FROM usuario "
				+ "WHERE correo = :correo").param("correo", correo).query().singleRow();
		assertThat(((Number) fila.get("anio_nacimiento")).intValue()).isEqualTo(1998);
		assertThat(fila.get("mayor_de_edad")).isIn(true, 1);
		assertThat(fila.get("verificado")).isIn(false, 0);
		assertThat((String) fila.get("hash_contrasena")).startsWith("$argon2id$").doesNotContain(CONTRASENA);
		assertThat(buzon.ultimoToken(correo, "verificacion")).isPresent();
	}

	@Test
	void laBaseNoTieneNingunaColumnaConLaFechaCompleta() {
		List<String> columnas = jdbc.sql("""
				SELECT column_name FROM information_schema.columns
				WHERE table_schema = DATABASE() AND table_name = 'usuario'
				  AND (data_type IN ('date', 'datetime', 'timestamp') AND column_name LIKE '%nacimiento%')""")
				.query(String.class).list();

		assertThat(columnas).isEmpty();
	}

	@Test
	void rechazaAUnMenorDeEdadYNoGuardaNada() throws Exception {
		String correo = correoNuevo();
		String hace17Anios = LocalDate.now().minusYears(17).toString();

		enviar(RUTA, cuerpoRegistro(correo, CONTRASENA, hace17Anios)).andExpect(status().isBadRequest());

		assertThat(contar("SELECT COUNT(*) FROM usuario WHERE correo = ?", correo)).isZero();
		assertThat(buzon.total()).isZero();
	}

	@Test
	void admiteAQuienCumpleDieciochoHoy() throws Exception {
		enviar(RUTA, cuerpoRegistro(correoNuevo(), CONTRASENA, LocalDate.now().minusYears(18).toString()))
				.andExpect(status().isAccepted());
	}

	@Test
	void rechazaUnaContrasenaCortaSinRepetirlaEnLaRespuesta() throws Exception {
		String corta = "secreta-11c";

		var resultado = enviar(RUTA, cuerpoRegistro(correoNuevo(), corta, "1998-04-12"))
				.andExpect(status().isBadRequest());

		assertThat(resultado.andReturn().getResponse().getContentAsString()).doesNotContain(corta);
	}

	@Test
	void rechazaUnaContrasenaDeLaListaDeFiltradas() throws Exception {
		var resultado = enviar(RUTA, cuerpoRegistro(correoNuevo(), ClavesDePrueba.CONTRASENA_FILTRADA, "1998-04-12"))
				.andExpect(status().isBadRequest());

		assertThat(cuerpoDe(resultado).get("detail").asString()).contains("filtraciones");
	}

	@Test
	void rechazaUnCorreoInvalidoYUnaFechaFuturaYUnCuerpoRoto() throws Exception {
		enviar(RUTA, cuerpoRegistro("no-es-un-correo", CONTRASENA, "1998-04-12")).andExpect(status().isBadRequest());
		enviar(RUTA, cuerpoRegistro(correoNuevo(), CONTRASENA, LocalDate.now().plusDays(1).toString()))
				.andExpect(status().isBadRequest());
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(RUTA)
				.contentType("application/json").content("{no es json").with(p -> {
					p.setRemoteAddr(ip);
					return p;
				})).andExpect(status().isBadRequest());
	}

	@Test
	void unCorreoDuplicadoRecibeLaMismaRespuestaQueUnAltaNueva() throws Exception {
		String correo = correoNuevo();
		var alta = enviar(RUTA, cuerpoRegistro(correo, CONTRASENA, "1998-04-12")).andExpect(status().isAccepted());
		int correosTrasElAlta = buzon.total();

		var duplicado = enviar(RUTA, cuerpoRegistro(correo.toUpperCase(), "otra-contrasena-larga-2", "1990-01-01"))
				.andExpect(status().isAccepted());

		assertThat(duplicado.andReturn().getResponse().getContentAsString())
				.isEqualTo(alta.andReturn().getResponse().getContentAsString());
		assertThat(contar("SELECT COUNT(*) FROM usuario WHERE correo = ?", correo)).isEqualTo(1);
		assertThat(buzon.total()).isEqualTo(correosTrasElAlta);
		// La cuenta original sigue con su contraseña y sus datos.
		verificarCorreo(correo);
		iniciarSesion(correo, "Pixel", null);
	}

	@Test
	void verificarConElTokenDelCorreoMarcaLaCuentaYSoloSirveUnaVez() throws Exception {
		String correo = crearCuentaSinVerificar();
		String token = buzon.ultimoToken(correo, "verificacion").orElseThrow();

		enviar("/api/v1/auth/verificar", java.util.Map.of("token", token)).andExpect(status().isNoContent());
		enviar("/api/v1/auth/verificar", java.util.Map.of("token", token)).andExpect(status().isBadRequest());

		assertThat(contar("SELECT verificado FROM usuario WHERE correo = ?", correo)).isEqualTo(1);
	}

	@Test
	void unTokenDeVerificacionCaducaALas24Horas() throws Exception {
		String correo = crearCuentaSinVerificar();
		String token = buzon.ultimoToken(correo, "verificacion").orElseThrow();

		reloj.avanzar(Duration.ofHours(24).plusMinutes(1));

		enviar("/api/v1/auth/verificar", java.util.Map.of("token", token)).andExpect(status().isBadRequest());
		assertThat(contar("SELECT verificado FROM usuario WHERE correo = ?", correo)).isZero();
	}

	@Test
	void elTokenDeVerificacionNoSirveParaRestablecerLaContrasena() throws Exception {
		String correo = crearCuentaSinVerificar();
		String token = buzon.ultimoToken(correo, "verificacion").orElseThrow();

		enviar("/api/v1/auth/restablecer", java.util.Map.of("token", token, "contrasenaNueva", "otra-contrasena-larga-3"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void unTokenInventadoSeRechaza() throws Exception {
		enviar("/api/v1/auth/verificar", java.util.Map.of("token", "no-existe")).andExpect(status().isBadRequest());
	}
}
