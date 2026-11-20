package es.forja.exportacion.modelo;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import es.forja.cuenta.servicio.ServicioCuenta.DatosCuenta;
import es.forja.entreno.movilidad.modelo.Movilidad;
import es.forja.entreno.perfil.modelo.DatosPerfil;
import es.forja.entreno.perfil.modelo.MedidasPerfil;
import es.forja.entreno.perfil.modelo.PerfilEntreno;
import es.forja.entreno.plan.modelo.PreferenciasGuardadas;
import es.forja.entreno.plan.modelo.VersionPlan;
import es.forja.restricciones.api.RestriccionesControlador.RespuestaRestriccion;
import es.forja.restricciones.bloqueo.ServicioBloqueo.BloqueoActivo;
import es.forja.sesiones.modelo.SesionVista;

/**
 * Todo lo que Forja guarda del usuario (RGPD, arts. 15 y 20), descifrado y solo con identificadores públicos. Sin los
 * hashes de la contraseña ni de los tokens. Los datos de salud salen si siguen guardados: retirar el consentimiento ya
 * los borró, así que entonces {@code medidas} y {@code movilidad} son nulos y las listas van vacías.
 *
 * @param formato versión de esta estructura; sube si cambia de forma incompatible
 * @param perfilDatos y {@code perfilEntreno}: con {@code porDefecto} o los valores por defecto si nunca los guardó
 * @param planes todas las versiones, de la más antigua a la actual
 * @param sesiones con todas sus series, también las corregidas, las correcciones y las anulaciones
 */
public record DatosExportados(int formato, Instant generadoEn, DatosCuenta cuenta,
		List<DispositivoExportado> dispositivos, List<ConsentimientoExportado> consentimientos, DatosPerfil perfilDatos,
		MedidasPerfil medidas, PerfilEntreno perfilEntreno, Movilidad movilidad, List<RespuestaRestriccion> limitaciones,
		List<BloqueoActivo> bloqueos, PreferenciasGuardadas preferenciasPlan, List<VersionPlan> planes,
		List<SesionVista> sesiones) {

	public DatosExportados {
		dispositivos = List.copyOf(dispositivos);
		consentimientos = List.copyOf(consentimientos);
		limitaciones = List.copyOf(limitaciones);
		bloqueos = List.copyOf(bloqueos);
		planes = List.copyOf(planes);
		sesiones = List.copyOf(sesiones);
	}

	/** @param revocadoEn nulo si la sesión del dispositivo sigue abierta */
	public record DispositivoExportado(UUID uuid, String nombre, String plataforma, Instant ultimaActividad,
			Instant revocadoEn) {
	}

	/** @param revocadoEn nulo si el consentimiento sigue dado */
	public record ConsentimientoExportado(String tipo, String versionTexto, Instant dadoEn, Instant revocadoEn) {
	}
}
