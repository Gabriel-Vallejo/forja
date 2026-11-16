package es.forja.cuenta.api;

import java.time.LocalDate;
import java.util.UUID;

import es.forja.cuenta.modelo.Plataforma;
import es.forja.cuenta.servicio.PoliticaContrasena;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

/**
 * Cuerpos de las peticiones de cuenta. Ninguno deja correo, contraseña ni token en {@code toString()}.
 * Los mensajes de validación no incluyen nunca el valor rechazado.
 */
public final class Solicitudes {

	private static final int LARGO_MAXIMO_CORREO = 254;
	private static final int LARGO_MAXIMO_NOMBRE = 80;
	private static final int LARGO_MAXIMO_TOKEN = 200;

	private Solicitudes() {
	}

	public record Registro(
			@NotBlank(message = "El correo es obligatorio.") @Email(message = "El correo no es válido.")
			@Size(max = LARGO_MAXIMO_CORREO, message = "El correo es demasiado largo.") String correo,
			@NotNull(message = "La contraseña es obligatoria.")
			@Size(min = PoliticaContrasena.LONGITUD_MINIMA, max = PoliticaContrasena.LONGITUD_MAXIMA,
					message = "La contraseña debe tener entre 12 y 128 caracteres.") String contrasena,
			@NotNull(message = "La fecha de nacimiento es obligatoria.")
			@Past(message = "La fecha de nacimiento debe ser pasada.") LocalDate fechaNacimiento) {

		@Override
		public String toString() {
			return "Registro[]";
		}
	}

	public record Token(
			@NotBlank(message = "El token es obligatorio.")
			@Size(max = LARGO_MAXIMO_TOKEN, message = "El token no es válido.") String token) {

		@Override
		public String toString() {
			return "Token[]";
		}
	}

	public record Login(
			@NotBlank(message = "El correo es obligatorio.")
			@Size(max = LARGO_MAXIMO_CORREO, message = "El correo es demasiado largo.") String correo,
			@NotBlank(message = "La contraseña es obligatoria.")
			@Size(max = PoliticaContrasena.LONGITUD_MAXIMA, message = "La contraseña es demasiado larga.")
			String contrasena,
			UUID dispositivoId,
			@NotBlank(message = "El nombre del dispositivo es obligatorio.")
			@Size(max = LARGO_MAXIMO_NOMBRE, message = "El nombre del dispositivo es demasiado largo.")
			String nombreDispositivo,
			@NotNull(message = "La plataforma es obligatoria.") Plataforma plataforma) {

		@Override
		public String toString() {
			return "Login[]";
		}
	}

	public record Refresco(
			@NotBlank(message = "El token de refresco es obligatorio.")
			@Size(max = LARGO_MAXIMO_TOKEN, message = "El token de refresco no es válido.") String tokenRefresco) {

		@Override
		public String toString() {
			return "Refresco[]";
		}
	}

	public record Recuperar(
			@NotBlank(message = "El correo es obligatorio.")
			@Size(max = LARGO_MAXIMO_CORREO, message = "El correo es demasiado largo.") String correo) {

		@Override
		public String toString() {
			return "Recuperar[]";
		}
	}

	public record Restablecer(
			@NotBlank(message = "El token es obligatorio.")
			@Size(max = LARGO_MAXIMO_TOKEN, message = "El token no es válido.") String token,
			@NotNull(message = "La contraseña nueva es obligatoria.")
			@Size(max = PoliticaContrasena.LONGITUD_MAXIMA, message = "La contraseña es demasiado larga.")
			String contrasenaNueva) {

		@Override
		public String toString() {
			return "Restablecer[]";
		}
	}

	public record BorrarCuenta(
			@NotBlank(message = "La contraseña es obligatoria.")
			@Size(max = PoliticaContrasena.LONGITUD_MAXIMA, message = "La contraseña es demasiado larga.")
			String contrasena) {

		@Override
		public String toString() {
			return "BorrarCuenta[]";
		}
	}
}
