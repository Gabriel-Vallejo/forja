package es.forja.cuenta.api;

import java.time.Instant;
import java.util.UUID;

import es.forja.cuenta.modelo.Plataforma;
import es.forja.cuenta.servicio.ServicioDispositivos;
import es.forja.cuenta.servicio.Sesion;

/** Cuerpos de las respuestas de cuenta. */
public final class Respuestas {

	private Respuestas() {
	}

	public record Mensaje(String mensaje) {
	}

	public record DatosSesion(String tokenAcceso, String tokenRefresco, Instant expiraEn, UUID dispositivoId) {

		static DatosSesion de(Sesion sesion) {
			return new DatosSesion(sesion.tokenAcceso(), sesion.tokenRefresco(), sesion.expiraEn(),
					sesion.dispositivo());
		}

		@Override
		public String toString() {
			return "DatosSesion[dispositivoId=" + dispositivoId + "]";
		}
	}

	public record DatosDispositivo(UUID id, String nombre, Plataforma plataforma, Instant ultimaActividad,
			boolean actual) {

		static DatosDispositivo de(ServicioDispositivos.Vista vista) {
			var d = vista.dispositivo();
			return new DatosDispositivo(d.uuid(), d.nombre(), d.plataforma(), d.ultimaActividad(), vista.actual());
		}
	}
}
