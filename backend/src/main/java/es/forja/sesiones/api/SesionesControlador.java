package es.forja.sesiones.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.forja.comun.seguridad.UsuarioAutenticado;
import es.forja.sesiones.modelo.DolorRepetido;
import es.forja.sesiones.modelo.PaginaSesiones;
import es.forja.sesiones.modelo.SesionEntrada;
import es.forja.sesiones.modelo.SesionVista;
import es.forja.sesiones.servicio.ServicioSesiones;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Registro de sesiones del usuario autenticado (ADR 0008). Los cuerpos y el dolor nunca se escriben en el log. */
@RestController
@RequestMapping("/api/v1/sesiones")
public class SesionesControlador {

	private static final int PAGINA_MAXIMA = 100_000;

	private final ServicioSesiones sesiones;

	public SesionesControlador(ServicioSesiones sesiones) {
		this.sesiones = sesiones;
	}

	/** Idempotente: la primera vez y un reenvío idéntico responden 200 con lo mismo. */
	@PutMapping("/{uuid}")
	public SesionVista guardar(@AuthenticationPrincipal UsuarioAutenticado actual, @PathVariable UUID uuid,
			@Valid @RequestBody SesionEntrada entrada) {
		return sesiones.guardar(actual.usuario(), uuid, entrada);
	}

	/** RF-27: los ejercicios que te han dolido en 2 de sus 3 últimas sesiones; la app pregunta antes de cambiarlos. */
	@GetMapping("/dolor-repetido")
	public List<DolorRepetido> dolorRepetido(@AuthenticationPrincipal UsuarioAutenticado actual) {
		return sesiones.dolorRepetido(actual.usuario());
	}

	@GetMapping
	public PaginaSesiones listar(@AuthenticationPrincipal UsuarioAutenticado actual,
			@RequestParam(required = false) Instant desde,
			@RequestParam(defaultValue = "0") @Min(0) @Max(PAGINA_MAXIMA) int pagina,
			@RequestParam(defaultValue = "" + ServicioSesiones.TAMANO_PAGINA_POR_DEFECTO) @Min(1)
			@Max(ServicioSesiones.TAMANO_PAGINA_MAXIMO) int tamano) {
		return sesiones.listar(actual.usuario(), desde, pagina, tamano);
	}
}
