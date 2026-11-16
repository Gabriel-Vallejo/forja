package es.forja.entreno.rutina.servicio;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import es.forja.comun.api.ErrorNegocio;
import es.forja.cuenta.servicio.LimitadorIntentos;
import es.forja.entreno.rutina.modelo.CatalogoRutina;
import es.forja.entreno.rutina.modelo.EjercicioRutina;
import es.forja.entreno.rutina.modelo.PlanRutina;
import es.forja.entreno.rutina.modelo.SolicitudRutina;
import es.forja.entreno.rutina.repositorio.CatalogoRutinaRepositorio;
import es.forja.entreno.rutina.repositorio.CatalogoRutinaRepositorio.FichaCurado;
import es.forja.entreno.rutina.solver.GeneradorRutina;
import es.forja.restricciones.modelo.Restriccion;
import es.forja.restricciones.servicio.ServicioEfecto;
import es.forja.restricciones.servicio.ServicioRestricciones;

/**
 * Genera la rutina del usuario autenticado. No guarda nada: el plan versionado es del ADR 0008. Las restricciones
 * solo entran si el usuario tiene el consentimiento de limitaciones (ADR 0004).
 */
@Service
public class ServicioRutina {

	static final String ACCION_GENERAR = "generar_rutina";

	private final ServicioEfecto efecto;
	private final ServicioRestricciones restricciones;
	private final CatalogoRutinaRepositorio repositorio;
	private final LimitadorIntentos limitador;
	private final GeneradorRutina generador;

	// shortcut: como el catálogo de ServicioEfecto, solo cambia al arrancar (Flyway); si se edita en caliente, invalidar aquí.
	private volatile CatalogoRutina catalogo;
	private volatile Set<String> materiales;

	public ServicioRutina(ServicioEfecto efecto, ServicioRestricciones restricciones,
			CatalogoRutinaRepositorio repositorio, LimitadorIntentos limitador, GeneradorRutina generador) {
		this.efecto = efecto;
		this.restricciones = restricciones;
		this.repositorio = repositorio;
		this.limitador = limitador;
		this.generador = generador;
	}

	/** Resolver cuesta CPU: gasta un intento del límite por usuario antes de nada. */
	public PlanRutina generar(UUID usuario, SolicitudRutina solicitud) {
		limitador.exigirPorClave(ACCION_GENERAR, usuario.toString());
		CatalogoRutina cargado = catalogo();
		validarCodigos(solicitud, cargado);
		List<Restriccion> suyas = restricciones.paraLaRutina(usuario);
		return generador.generar(cargado, solicitud.aEntrada(suyas));
	}

	/** Los curados del catálogo, con lo que mira el filtro y lo que necesita el modelo. */
	public CatalogoRutina catalogo() {
		CatalogoRutina cargado = catalogo;
		if (cargado == null) {
			Map<String, FichaCurado> fichas = repositorio.curados();
			List<EjercicioRutina> ejercicios = efecto.catalogo().stream()
					.filter(c -> fichas.containsKey(c.idExterno())).map(c -> {
						FichaCurado f = fichas.get(c.idExterno());
						return new EjercicioRutina(c, f.aptoFuerza(), f.compuesto(), f.posicionEstirada(),
								f.nombreMaterial(), f.musculos());
					}).toList();
			cargado = new CatalogoRutina(ejercicios, repositorio.nombresMusculo());
			catalogo = cargado;
		}
		return cargado;
	}

	private void validarCodigos(SolicitudRutina solicitud, CatalogoRutina cargado) {
		Set<String> conocidos = materiales;
		if (conocidos == null) {
			conocidos = repositorio.codigosMaterial();
			materiales = conocidos;
		}
		if (!conocidos.containsAll(solicitud.material())) {
			throw ErrorNegocio.datosNoValidos("Hay material que no existe.");
		}
		if (solicitud.prioritarios() != null
				&& !cargado.nombresMusculo().keySet().containsAll(solicitud.prioritarios())) {
			throw ErrorNegocio.datosNoValidos("Hay músculos prioritarios que no existen.");
		}
	}
}
