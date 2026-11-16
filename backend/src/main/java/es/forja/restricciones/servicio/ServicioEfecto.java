package es.forja.restricciones.servicio;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.forja.entreno.catalogo.repositorio.EjercicioRepositorio;
import es.forja.restricciones.filtro.AdaptadorCatalogo;
import es.forja.restricciones.filtro.Decision;
import es.forja.restricciones.filtro.EjercicioCandidato;
import es.forja.restricciones.filtro.FiltroRestricciones;
import es.forja.restricciones.modelo.Restriccion;

/** Aplica el filtro sobre el catálogo con las restricciones del usuario, para enseñarle qué cambia. */
@Service
public class ServicioEfecto {

	private final ServicioRestricciones restricciones;
	private final EjercicioRepositorio ejercicios;
	private final FiltroRestricciones filtro = new FiltroRestricciones();

	// shortcut: el catálogo solo cambia al arrancar (Flyway), así que se carga una vez; si se edita en caliente, invalidar aquí.
	private volatile List<EjercicioCandidato> catalogo;

	public ServicioEfecto(ServicioRestricciones restricciones, EjercicioRepositorio ejercicios) {
		this.restricciones = restricciones;
		this.ejercicios = ejercicios;
	}

	/** Solo los ejercicios que cambian: excluidos, sustituidos o permitidos con anotaciones. */
	@Transactional(readOnly = true)
	public List<EfectoEjercicio> calcular(UUID usuario) {
		long usuarioId = restricciones.exigir(usuario);
		List<Restriccion> activas = restricciones.listarDeUsuarioId(usuarioId).stream()
				.map(RestriccionGuardada::restriccion).toList();
		List<EjercicioCandidato> candidatos = catalogo();
		Map<String, EjercicioCandidato> porId = candidatos.stream()
				.collect(Collectors.toMap(EjercicioCandidato::idExterno, e -> e));
		return filtro.aplicar(candidatos, activas).entrySet().stream()
				.filter(e -> cambia(e.getValue()))
				.map(e -> new EfectoEjercicio(porId.get(e.getKey()), e.getValue(), sustituto(e.getValue(), porId)))
				.toList();
	}

	private static boolean cambia(Decision decision) {
		return !(decision instanceof Decision.Permitido p) || !p.anotaciones().isEmpty();
	}

	private static EjercicioCandidato sustituto(Decision decision, Map<String, EjercicioCandidato> porId) {
		return decision instanceof Decision.Sustituido s ? porId.get(s.porIdExterno()) : null;
	}

	/** El catálogo entero visto por el filtro; también lo usa el generador de la rutina. */
	@Transactional(readOnly = true)
	public List<EjercicioCandidato> catalogo() {
		List<EjercicioCandidato> cargado = catalogo;
		if (cargado == null) {
			// El adaptador necesita la sesión abierta: estamos dentro de la transacción de solo lectura.
			cargado = ejercicios.findAll().stream().map(AdaptadorCatalogo::desde).toList();
			catalogo = cargado;
		}
		return cargado;
	}
}
