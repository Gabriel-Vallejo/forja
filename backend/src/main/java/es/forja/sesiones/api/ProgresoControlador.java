package es.forja.sesiones.api;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.forja.comun.seguridad.UsuarioAutenticado;
import es.forja.sesiones.modelo.MarcaEjercicio;
import es.forja.sesiones.modelo.ResumenProgreso;
import es.forja.sesiones.servicio.ServicioProgreso;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/progreso")
public class ProgresoControlador {

	private static final int LARGO_MAXIMO_ZONA = 64;

	private final ServicioProgreso progreso;

	public ProgresoControlador(ServicioProgreso progreso) {
		this.progreso = progreso;
	}

	/** {@code zona}: la del móvil (p. ej. Europe/Madrid), para que cada sesión caiga en su día; sin ella, UTC. */
	@GetMapping("/resumen")
	public ResumenProgreso resumen(@AuthenticationPrincipal UsuarioAutenticado actual,
			@RequestParam(required = false) @Size(max = LARGO_MAXIMO_ZONA) String zona) {
		return progreso.resumen(actual.usuario(), zona);
	}

	/** La mejor serie de cada ejercicio hecho: la app saca de aquí el rango global y el mapa por músculo. */
	@GetMapping("/marcas")
	public List<MarcaEjercicio> marcas(@AuthenticationPrincipal UsuarioAutenticado actual) {
		return progreso.marcas(actual.usuario());
	}
}
