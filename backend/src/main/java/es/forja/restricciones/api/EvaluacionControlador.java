package es.forja.restricciones.api;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.forja.comun.api.ErrorNegocio;
import es.forja.comun.seguridad.UsuarioAutenticado;
import es.forja.consentimiento.modelo.TipoConsentimiento;
import es.forja.consentimiento.servicio.ServicioConsentimientos;
import es.forja.restricciones.banderas.BanderaRoja;
import es.forja.restricciones.banderas.BanderasRojas;
import es.forja.restricciones.banderas.RespuestasGuiadas;
import es.forja.restricciones.bloqueo.NivelBloqueo;
import es.forja.restricciones.bloqueo.OrigenBloqueo;
import es.forja.restricciones.bloqueo.ServicioBloqueo;
import es.forja.restricciones.cribado.Cribado;
import es.forja.restricciones.cribado.PreguntaCribado;
import es.forja.restricciones.cribado.RespuestasCribado;
import es.forja.restricciones.cribado.ResultadoCribado;
import es.forja.restricciones.modelo.Region;

/**
 * Cribado previo y banderas rojas (ADR 0007): reglas deterministas que no guardan las respuestas, solo el nivel del resultado si bloquea (ADR 0007, enmienda). Exigen el consentimiento
 * de {@code limitaciones} antes de mirar siquiera el cuerpo. Dato de salud: nada de esto se escribe en el log.
 */
@RestController
@RequestMapping("/api/v1")
public class EvaluacionControlador {

	/** Las siete respuestas por código de pregunta y, aparte, cuáles de los síntomas fueron recientes. */
	public record SolicitudCribado(Map<String, Boolean> respuestas, List<String> recientes) {

		RespuestasCribado aDominio() {
			if (respuestas == null) {
				throw ErrorNegocio.datosNoValidos("Faltan las respuestas del cribado.");
			}
			Map<PreguntaCribado, Boolean> contestadas = new EnumMap<>(PreguntaCribado.class);
			respuestas.forEach((codigo, valor) -> contestadas.put(pregunta(codigo), valor));
			Set<PreguntaCribado> marcadas = (recientes == null ? List.<String>of() : recientes).stream()
					.map(SolicitudCribado::pregunta).collect(Collectors.toSet());
			return construir(() -> new RespuestasCribado(contestadas, marcadas));
		}

		private static PreguntaCribado pregunta(String codigo) {
			for (PreguntaCribado p : PreguntaCribado.values()) {
				if (p.name().toLowerCase(Locale.ROOT).equals(codigo)) {
					return p;
				}
			}
			throw ErrorNegocio.datosNoValidos("Hay una pregunta del cribado que no existe.");
		}
	}

	/** Una pregunta del cribado tal cual se enseña; {@code admiteReciente}: si se puede marcar el síntoma como reciente. */
	public record RespuestaPregunta(String codigo, String texto, boolean admiteReciente) {
	}

	/** {@code semanasReciente}: hasta cuándo cuenta un síntoma como reciente ({@link RespuestasCribado#SEMANAS_RECIENTE}). */
	public record RespuestaPreguntas(int semanasReciente, List<RespuestaPregunta> preguntas) {
	}

	public record RespuestaCribado(String nivel, List<String> motivos) {
		static RespuestaCribado de(ResultadoCribado r) {
			return new RespuestaCribado(codigo(r.nivel()), r.motivos());
		}
	}

	/** Dolor de 0 a 10 por código de región y las diez señales; ninguna puede faltar. */
	public record SolicitudBanderas(Map<String, Integer> dolorPorZona, Boolean dolorEnReposo,
			Boolean dolorNocturnoQueDespierta, Boolean hormigueoOEntumecimiento, Boolean perdidaDeFuerza,
			Boolean perdidaControlEsfinteresOAnestesiaSillaDeMontar, Boolean fiebreConDolorDeEspalda,
			Boolean traumatismoReciente, Boolean perdidaDePesoSinExplicacion, Boolean dolorToracico,
			Boolean mareoODesmayo) {

		RespuestasGuiadas aDominio() {
			if (dolorPorZona == null) {
				throw ErrorNegocio.datosNoValidos("Falta el dolor por zona.");
			}
			Map<Region, Integer> dolor = new EnumMap<>(Region.class);
			dolorPorZona.forEach((codigo, valor) -> dolor.put(region(codigo), requerido(valor, "dolorPorZona")));
			return construir(() -> new RespuestasGuiadas(dolor, requerido(dolorEnReposo, "dolorEnReposo"),
					requerido(dolorNocturnoQueDespierta, "dolorNocturnoQueDespierta"),
					requerido(hormigueoOEntumecimiento, "hormigueoOEntumecimiento"),
					requerido(perdidaDeFuerza, "perdidaDeFuerza"),
					requerido(perdidaControlEsfinteresOAnestesiaSillaDeMontar,
							"perdidaControlEsfinteresOAnestesiaSillaDeMontar"),
					requerido(fiebreConDolorDeEspalda, "fiebreConDolorDeEspalda"),
					requerido(traumatismoReciente, "traumatismoReciente"),
					requerido(perdidaDePesoSinExplicacion, "perdidaDePesoSinExplicacion"),
					requerido(dolorToracico, "dolorToracico"), requerido(mareoODesmayo, "mareoODesmayo")));
		}

		private static Region region(String codigo) {
			try {
				return Region.desdeCodigo(codigo);
			} catch (IllegalArgumentException e) {
				throw ErrorNegocio.datosNoValidos("Hay una zona del cuerpo que no existe.");
			}
		}

		private static <T> T requerido(T valor, String campo) {
			if (valor == null) {
				throw ErrorNegocio.datosNoValidos("Falta la respuesta «" + campo + "».");
			}
			return valor;
		}
	}

	public record RespuestaBandera(String nivel, String motivo, String guia) {
	}

	public record RespuestaBanderas(boolean bloquea, List<RespuestaBandera> banderas) {
	}

	/** {@code nivel} nulo cuando no bloquea. */
	public record EstadoBloqueo(boolean bloquea, @Nullable String nivel) {
	}

	private final ServicioConsentimientos consentimientos;
	private final ServicioBloqueo bloqueo;
	private final Cribado cribado = new Cribado();
	private final BanderasRojas banderas = new BanderasRojas();

	public EvaluacionControlador(ServicioConsentimientos consentimientos, ServicioBloqueo bloqueo) {
		this.consentimientos = consentimientos;
		this.bloqueo = bloqueo;
	}

	/** Texto fijo del cuestionario, para que la app no lo duplique: no mira al usuario, solo exige sesión. */
	@GetMapping("/cribado/preguntas")
	public RespuestaPreguntas preguntas() {
		return new RespuestaPreguntas(RespuestasCribado.SEMANAS_RECIENTE, java.util.Arrays.stream(PreguntaCribado.values())
				.map(p -> new RespuestaPregunta(codigo(p), p.texto(), p.admiteReciente())).toList());
	}

	@PostMapping("/cribado")
	public RespuestaCribado cribar(@AuthenticationPrincipal UsuarioAutenticado actual,
			@RequestBody(required = false) SolicitudCribado cuerpo) {
		long usuarioId = consentimientos.exigir(actual.usuario(), TipoConsentimiento.LIMITACIONES);
		ResultadoCribado resultado = cribado.evaluar(solicitud(cuerpo).aDominio());
		bloqueo.registrar(usuarioId, OrigenBloqueo.CRIBADO,
				resultado.nivel() == ResultadoCribado.Nivel.PARAR_URGENCIAS ? NivelBloqueo.PARAR_URGENCIAS : null);
		return RespuestaCribado.de(resultado);
	}

	/**
	 * Si hay un bloqueo activo y de qué nivel, sin más. No exige el consentimiento: sin él no puede haber bloqueos
	 * guardados (revocarlo los borra), así que la respuesta es {@code bloquea:false}.
	 */
	@GetMapping("/banderas/estado")
	public EstadoBloqueo estado(@AuthenticationPrincipal UsuarioAutenticado actual) {
		return bloqueo.nivelActivo(actual.usuario()).map(n -> new EstadoBloqueo(true, n.codigo()))
				.orElse(new EstadoBloqueo(false, null));
	}

	@PostMapping("/banderas")
	public RespuestaBanderas evaluarBanderas(@AuthenticationPrincipal UsuarioAutenticado actual,
			@RequestBody(required = false) SolicitudBanderas cuerpo) {
		long usuarioId = consentimientos.exigir(actual.usuario(), TipoConsentimiento.LIMITACIONES);
		List<BanderaRoja> resultado = banderas.evaluar(solicitud(cuerpo).aDominio());
		boolean bloquea = BanderasRojas.bloqueaLaRutina(resultado);
		// Las banderas salen con la más grave primero.
		bloqueo.registrar(usuarioId, OrigenBloqueo.BANDERAS, !bloquea ? null
				: resultado.get(0).nivel() == BanderaRoja.Nivel.URGENCIAS_HOY ? NivelBloqueo.URGENCIAS_HOY
						: NivelBloqueo.PROFESIONAL_EN_DIAS);
		return new RespuestaBanderas(bloquea, resultado.stream()
				.map(b -> new RespuestaBandera(codigo(b.nivel()), b.motivo(), b.guia())).toList());
	}

	private static <T> T solicitud(T cuerpo) {
		if (cuerpo == null) {
			throw ErrorNegocio.datosNoValidos("Falta el cuerpo de la petición.");
		}
		return cuerpo;
	}

	/** Los constructores del dominio validan y avisan con IllegalArgumentException; aquí pasa a 400. */
	private static <T> T construir(java.util.function.Supplier<T> constructor) {
		try {
			return constructor.get();
		} catch (IllegalArgumentException e) {
			throw ErrorNegocio.datosNoValidos(e.getMessage());
		}
	}

	private static String codigo(Enum<?> valor) {
		return valor.name().toLowerCase(Locale.ROOT);
	}
}
