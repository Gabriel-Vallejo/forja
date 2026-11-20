package es.forja.restricciones.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.forja.comun.seguridad.UsuarioAutenticado;
import es.forja.restricciones.filtro.Decision;
import es.forja.restricciones.modelo.RestriccionCodificada;
import es.forja.restricciones.servicio.EfectoEjercicio;
import es.forja.restricciones.servicio.RestriccionGuardada;
import es.forja.restricciones.servicio.ServicioEfecto;
import es.forja.restricciones.servicio.ServicioRestricciones;

/**
 * Limitaciones del usuario autenticado (ADR 0007), con el consentimiento de {@code limitaciones}. Los enums van
 * por código ({@code sin_carga_axial}, {@code rodilla}). Dato de salud: nada de esto se escribe en el log.
 */
@RestController
@RequestMapping("/api/v1/restricciones")
public class RestriccionesControlador {

	public record RespuestaRestriccion(UUID uuid, String region, String lado, String patron, String tipo,
			String parametro, String estado, String origen, String textoOriginal) {

		public static RespuestaRestriccion de(RestriccionGuardada guardada) {
			RestriccionCodificada c = RestriccionCodificada.de(guardada.restriccion());
			return new RespuestaRestriccion(guardada.uuid(), c.region(), c.lado(), c.patron(), c.tipo(),
					c.parametro(), c.estado(), c.origen(), c.textoOriginal());
		}
	}

	public record Sustituto(String idExterno, String nombre) {
	}

	/** {@code decision}: {@code excluido}, {@code sustituido} o {@code con_anotaciones}. */
	public record RespuestaEfecto(String idExterno, String nombre, String decision, String motivo,
			Sustituto sustituto, List<String> anotaciones) {

		static RespuestaEfecto de(EfectoEjercicio efecto) {
			String id = efecto.ejercicio().idExterno();
			String nombre = efecto.ejercicio().nombre();
			return switch (efecto.decision()) {
				case Decision.Excluido e -> new RespuestaEfecto(id, nombre, "excluido", e.motivo(), null, List.of());
				case Decision.Sustituido s -> new RespuestaEfecto(id, nombre, "sustituido", s.motivo(),
						new Sustituto(efecto.sustituto().idExterno(), efecto.sustituto().nombre()), s.anotaciones());
				case Decision.Permitido p -> new RespuestaEfecto(id, nombre, "con_anotaciones", null, null,
						p.anotaciones());
			};
		}
	}

	private final ServicioRestricciones restricciones;
	private final ServicioEfecto efecto;

	public RestriccionesControlador(ServicioRestricciones restricciones, ServicioEfecto efecto) {
		this.restricciones = restricciones;
		this.efecto = efecto;
	}

	@GetMapping
	public List<RespuestaRestriccion> listar(@AuthenticationPrincipal UsuarioAutenticado actual) {
		return restricciones.listar(actual.usuario()).stream().map(RespuestaRestriccion::de).toList();
	}

	@PostMapping
	public ResponseEntity<RespuestaRestriccion> crear(@AuthenticationPrincipal UsuarioAutenticado actual,
			@RequestBody RestriccionCodificada cuerpo) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(RespuestaRestriccion.de(restricciones.crear(actual.usuario(), cuerpo)));
	}

	@PutMapping("/{uuid}")
	public RespuestaRestriccion actualizar(@AuthenticationPrincipal UsuarioAutenticado actual,
			@PathVariable UUID uuid, @RequestBody RestriccionCodificada cuerpo) {
		return RespuestaRestriccion.de(restricciones.actualizar(actual.usuario(), uuid, cuerpo));
	}

	@DeleteMapping("/{uuid}")
	public ResponseEntity<Void> borrar(@AuthenticationPrincipal UsuarioAutenticado actual, @PathVariable UUID uuid) {
		restricciones.borrar(actual.usuario(), uuid);
		return ResponseEntity.noContent().build();
	}

	/** Qué ejercicios del catálogo cambian con las restricciones del usuario: solo los que cambian. */
	@GetMapping("/efecto")
	public List<RespuestaEfecto> efecto(@AuthenticationPrincipal UsuarioAutenticado actual) {
		return efecto.calcular(actual.usuario()).stream().map(RespuestaEfecto::de).toList();
	}
}
