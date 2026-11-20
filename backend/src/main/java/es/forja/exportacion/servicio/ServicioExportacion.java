package es.forja.exportacion.servicio;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.forja.consentimiento.modelo.TipoConsentimiento;
import es.forja.consentimiento.servicio.ServicioConsentimientos;
import es.forja.cuenta.servicio.LimitadorIntentos;
import es.forja.cuenta.servicio.ServicioCuenta;
import es.forja.cuenta.servicio.ServicioDispositivos;
import es.forja.entreno.movilidad.servicio.ServicioMovilidad;
import es.forja.entreno.perfil.servicio.ServicioMedidas;
import es.forja.entreno.perfil.servicio.ServicioPerfilDatos;
import es.forja.entreno.perfil.servicio.ServicioPerfilEntreno;
import es.forja.entreno.plan.servicio.ServicioAjustes;
import es.forja.entreno.plan.servicio.ServicioPlan;
import es.forja.exportacion.modelo.DatosExportados;
import es.forja.exportacion.modelo.DatosExportados.ConsentimientoExportado;
import es.forja.exportacion.modelo.DatosExportados.DispositivoExportado;
import es.forja.restricciones.api.RestriccionesControlador.RespuestaRestriccion;
import es.forja.restricciones.bloqueo.ServicioBloqueo;
import es.forja.restricciones.servicio.ServicioRestricciones;
import es.forja.sesiones.servicio.ServicioSesiones;

/**
 * «Descargar mis datos» (RGPD, arts. 15 y 20): junta lo que sirve cada dominio con sus propios servicios de lectura,
 * que ya descifran. No toca tablas ni escribe nada en el log: el contenido son datos de salud.
 */
@Service
public class ServicioExportacion {

	public static final int FORMATO = 1;
	public static final int EXPORTACIONES_POR_VENTANA = 5;
	public static final Duration VENTANA = Duration.ofHours(1);
	private static final String ACCION = "exportar";

	private final LimitadorIntentos limitador;
	private final ServicioCuenta cuenta;
	private final ServicioDispositivos dispositivos;
	private final ServicioConsentimientos consentimientos;
	private final ServicioPerfilDatos perfilDatos;
	private final ServicioMedidas medidas;
	private final ServicioPerfilEntreno perfilEntreno;
	private final ServicioMovilidad movilidad;
	private final ServicioRestricciones restricciones;
	private final ServicioBloqueo bloqueos;
	private final ServicioAjustes ajustes;
	private final ServicioPlan planes;
	private final ServicioSesiones sesiones;
	private final Clock reloj;

	public ServicioExportacion(LimitadorIntentos limitador, ServicioCuenta cuenta, ServicioDispositivos dispositivos,
			ServicioConsentimientos consentimientos, ServicioPerfilDatos perfilDatos, ServicioMedidas medidas,
			ServicioPerfilEntreno perfilEntreno, ServicioMovilidad movilidad, ServicioRestricciones restricciones,
			ServicioBloqueo bloqueos, ServicioAjustes ajustes, ServicioPlan planes, ServicioSesiones sesiones,
			Clock reloj) {
		this.limitador = limitador;
		this.cuenta = cuenta;
		this.dispositivos = dispositivos;
		this.consentimientos = consentimientos;
		this.perfilDatos = perfilDatos;
		this.medidas = medidas;
		this.perfilEntreno = perfilEntreno;
		this.movilidad = movilidad;
		this.restricciones = restricciones;
		this.bloqueos = bloqueos;
		this.ajustes = ajustes;
		this.planes = planes;
		this.sesiones = sesiones;
		this.reloj = reloj;
	}

	/** Todo en una transacción de solo lectura, para que las secciones cuadren entre sí. 429 pasado el límite. */
	@Transactional(readOnly = true)
	public DatosExportados exportar(UUID usuario) {
		limitador.exigirPorClave(ACCION, usuario.toString(), EXPORTACIONES_POR_VENTANA, VENTANA);
		boolean conMedidas = consentimientos.tiene(usuario, TipoConsentimiento.MEDIDAS_SALUD);
		boolean conLimitaciones = consentimientos.tiene(usuario, TipoConsentimiento.LIMITACIONES);
		return new DatosExportados(FORMATO, reloj.instant(), cuenta.datos(usuario), dispositivosDe(usuario),
				consentimientosDe(usuario), perfilDatos.obtener(usuario), conMedidas ? medidas.obtener(usuario) : null,
				perfilEntreno.obtener(usuario), conMedidas ? movilidad.obtener(usuario) : null,
				conLimitaciones ? restricciones.listar(usuario).stream().map(RespuestaRestriccion::de).toList()
						: List.of(),
				bloqueos.activos(usuario), ajustes.preferencias(usuario), planes.versiones(usuario),
				sesiones.historial(usuario));
	}

	private List<DispositivoExportado> dispositivosDe(UUID usuario) {
		return dispositivos.todos(usuario).stream().map(d -> new DispositivoExportado(d.uuid(), d.nombre(),
				d.plataforma().codigo(), d.ultimaActividad(), d.revocadoEn())).toList();
	}

	private List<ConsentimientoExportado> consentimientosDe(UUID usuario) {
		return consentimientos.historial(usuario).stream().map(c -> new ConsentimientoExportado(c.tipo().codigo(),
				c.versionTexto(), c.dadoEn(), c.revocadoEn())).toList();
	}
}
