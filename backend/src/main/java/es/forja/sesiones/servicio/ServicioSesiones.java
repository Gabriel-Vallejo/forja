package es.forja.sesiones.servicio;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.forja.comun.api.ErrorNegocio;
import es.forja.comun.cifrado.Cifrador;
import es.forja.consentimiento.modelo.ConsentimientoRevocado;
import es.forja.consentimiento.modelo.TipoConsentimiento;
import es.forja.consentimiento.servicio.ServicioConsentimientos;
import es.forja.cuenta.repositorio.UsuarioRepositorio;
import es.forja.entreno.plan.servicio.ServicioPlan;
import es.forja.sesiones.modelo.PaginaSesiones;
import es.forja.sesiones.modelo.SerieEntrada;
import es.forja.sesiones.modelo.SesionEntrada;
import es.forja.sesiones.modelo.SesionVista;
import es.forja.sesiones.modelo.SesionVista.SerieVista;
import es.forja.sesiones.repositorio.SesionRepositorio;
import es.forja.sesiones.repositorio.SesionRepositorio.CabeceraVista;
import es.forja.sesiones.repositorio.SesionRepositorio.FilaSerie;
import es.forja.sesiones.repositorio.SesionRepositorio.FilaSesion;
import es.forja.sesiones.repositorio.SesionRepositorio.SerieVigente;

/**
 * Registro de sesiones sin conexión (ADR 0008): el móvil reenvía la sesión entera las veces que haga falta y el
 * servidor solo añade. Nada de lo que llega se escribe en el log; el dolor, además, va cifrado y solo con el
 * consentimiento de limitaciones (ADR 0004).
 */
@Service
public class ServicioSesiones {

	/** Más separación que esto entre el reloj del móvil y el del servidor marca la sesión para revisar. */
	public static final Duration UMBRAL_RELOJ_DESFASADO = Duration.ofHours(24);
	public static final int TAMANO_PAGINA_POR_DEFECTO = 20;
	public static final int TAMANO_PAGINA_MAXIMO = 100;

	static final String TABLA_SERIE = "serie_realizada";
	static final String COLUMNA_DOLOR = "dolor";

	private final SesionRepositorio sesiones;
	private final ServicioPlan planes;
	private final ServicioConsentimientos consentimientos;
	private final UsuarioRepositorio usuarios;
	private final Cifrador cifrador;
	private final Clock reloj;

	public ServicioSesiones(SesionRepositorio sesiones, ServicioPlan planes, ServicioConsentimientos consentimientos,
			UsuarioRepositorio usuarios, Cifrador cifrador, Clock reloj) {
		this.sesiones = sesiones;
		this.planes = planes;
		this.consentimientos = consentimientos;
		this.usuarios = usuarios;
		this.cifrador = cifrador;
		this.reloj = reloj;
	}

	/**
	 * Guarda la sesión: la primera vez la crea y un reenvío idéntico no cambia nada. Solo añade series; una que ya
	 * existe con contenido distinto es un 409.
	 *
	 * <p>shortcut: dos primeras llegadas simultáneas de la misma sesión pueden chocar en la base (un 5xx que el
	 * outbox reintenta); si pasa a menudo, bloquear por uuid de sesión.
	 */
	@Transactional
	public SesionVista guardar(UUID usuario, UUID uuid, SesionEntrada entrada) {
		long usuarioId = idDe(usuario);
		exigirSeriesCoherentes(entrada.series());
		FilaSesion existente = sesiones.sesion(uuid).orElse(null);
		if (existente != null && existente.usuarioId() != usuarioId) {
			throw ErrorNegocio.noEncontrado("No existe esa sesión.");
		}
		Long planId = planDe(usuarioId, entrada);
		Map<String, Long> ejercicios = ejerciciosDe(entrada.series());
		if (existente == null) {
			crear(usuarioId, uuid, planId, entrada);
		} else {
			exigirMismaCabecera(existente, planId, entrada);
		}
		anadirSeries(usuario, usuarioId, uuid, entrada.series(), ejercicios);
		return sesiones.cabecera(usuarioId, uuid).map(c -> vistas(usuario, List.of(c)).get(0)).orElseThrow();
	}

	/** Sesiones del usuario desde una fecha, de la más antigua a la más reciente, con sus series vigentes. */
	public PaginaSesiones listar(UUID usuario, Instant desde, int pagina, int tamano) {
		long usuarioId = idDe(usuario);
		Instant inicio = desde == null ? Instant.EPOCH : desde;
		List<CabeceraVista> cabeceras = sesiones.cabeceras(usuarioId, inicio, tamano, (long) pagina * tamano);
		return new PaginaSesiones(vistas(usuario, cabeceras), pagina, tamano, sesiones.contar(usuarioId, inicio));
	}

	/** Revocar el consentimiento de limitaciones borra el dolor guardado, en la misma transacción. */
	@EventListener
	public void alRevocarUnConsentimiento(ConsentimientoRevocado revocado) {
		if (revocado.tipo() == TipoConsentimiento.LIMITACIONES) {
			sesiones.borrarDolorDeUsuario(revocado.usuarioId());
		}
	}

	private void crear(long usuarioId, UUID uuid, Long planId, SesionEntrada entrada) {
		Instant recibida = reloj.instant().truncatedTo(ChronoUnit.MICROS);
		Instant realizada = entrada.realizadaEn().truncatedTo(ChronoUnit.MICROS);
		boolean desfasado = Duration.between(realizada, recibida).abs().compareTo(UMBRAL_RELOJ_DESFASADO) > 0;
		sesiones.insertarSesion(uuid, usuarioId, planId, entrada.diaPlan(), realizada, recibida, desfasado);
	}

	private void exigirMismaCabecera(FilaSesion existente, Long planId, SesionEntrada entrada) {
		boolean igual = Objects.equals(existente.planVersionId(), planId)
				&& Objects.equals(existente.diaPlan(), entrada.diaPlan())
				&& existente.realizadaEn().equals(entrada.realizadaEn().truncatedTo(ChronoUnit.MICROS));
		if (!igual) {
			throw ErrorNegocio.conflicto("Esa sesión ya existe con otros datos.");
		}
	}

	private Long planDe(long usuarioId, SesionEntrada entrada) {
		if (entrada.planVersion() == null) {
			return null;
		}
		return planes.idDeVersion(usuarioId, entrada.planVersion())
				.orElseThrow(() -> ErrorNegocio.noProcesable("Esa versión del plan no existe."));
	}

	private Map<String, Long> ejerciciosDe(List<SerieEntrada> series) {
		Set<String> pedidos = series.stream().map(SerieEntrada::ejercicio).collect(Collectors.toSet());
		Map<String, Long> ids = sesiones.idsDeEjercicios(pedidos);
		if (!ids.keySet().containsAll(pedidos)) {
			throw ErrorNegocio.noProcesable("Hay ejercicios que no existen.");
		}
		return ids;
	}

	private static void exigirSeriesCoherentes(List<SerieEntrada> series) {
		Set<UUID> vistos = new HashSet<>();
		for (SerieEntrada s : series) {
			if (!vistos.add(s.uuid())) {
				throw ErrorNegocio.datosNoValidos("Hay series repetidas en la sesión.");
			}
			if (s.anulada() && s.corrigeA() == null) {
				throw ErrorNegocio.noProcesable("Una anulación tiene que apuntar a la serie que anula.");
			}
		}
	}

	private void anadirSeries(UUID usuario, long usuarioId, UUID sesion, List<SerieEntrada> series,
			Map<String, Long> ejercicios) {
		Map<UUID, FilaSerie> previas = sesiones.series(series.stream().map(SerieEntrada::uuid).toList());
		boolean conConsentimiento = consentimientos.tiene(usuario, TipoConsentimiento.LIMITACIONES);
		// Las series ya aceptadas en este mismo envío: una corrección solo puede apuntar hacia atrás, así no hay ciclos.
		Set<UUID> anteriores = new HashSet<>();
		for (SerieEntrada s : series) {
			FilaSerie previa = previas.get(s.uuid());
			if (previa != null) {
				exigirMismaSerie(previa, sesion, s, ejercicios.get(s.ejercicio()), conConsentimiento);
			} else {
				exigirCorreccionValida(usuarioId, s, anteriores);
				insertar(sesion, s, ejercicios.get(s.ejercicio()), conConsentimiento);
			}
			anteriores.add(s.uuid());
		}
	}

	private void exigirCorreccionValida(long usuarioId, SerieEntrada s, Collection<UUID> anteriores) {
		UUID objetivo = s.corrigeA();
		if (objetivo == null) {
			return;
		}
		boolean existe = anteriores.contains(objetivo) || sesiones.existeSerieDeUsuario(usuarioId, objetivo);
		if (!existe || objetivo.equals(s.uuid())) {
			throw ErrorNegocio.noProcesable("Una corrección tiene que apuntar a una serie tuya ya registrada.");
		}
	}

	private void insertar(UUID sesion, SerieEntrada s, long ejercicioId, boolean conConsentimiento) {
		byte[] dolor = conConsentimiento && s.dolor() != null ? cifrarDolor(s.uuid(), s.dolor()) : null;
		try {
			sesiones.insertarSerie(new FilaSerie(s.uuid(), sesion, 0, ejercicioId, s.orden(), s.pesoKg(),
					s.repeticiones(), s.rir(), s.rpe(), s.calentamiento(), dolor, s.corrigeA(), s.anulada(),
					s.realizadaEn().truncatedTo(ChronoUnit.MICROS)));
		} catch (DuplicateKeyException e) {
			// El uuid lo ganó otra petición a la vez o esa serie ya tiene una corrección distinta.
			throw ErrorNegocio.conflicto("Esa serie ya existe con otros datos o ya se corrigió.");
		}
	}

	/** Una serie que ya está guardada solo vale si es esta misma: mismo contenido y misma sesión. */
	private void exigirMismaSerie(FilaSerie previa, UUID sesion, SerieEntrada s, long ejercicioId,
			boolean conConsentimiento) {
		boolean igual = previa.sesionUuid().equals(sesion) && previa.ejercicioId() == ejercicioId
				&& previa.orden() == s.orden() && previa.pesoKg().compareTo(s.pesoKg()) == 0
				&& previa.repeticiones() == s.repeticiones() && Objects.equals(previa.rir(), s.rir())
				&& iguales(previa.rpe(), s.rpe()) && previa.calentamiento() == s.calentamiento()
				&& Objects.equals(previa.corrigeA(), s.corrigeA()) && previa.anulada() == s.anulada()
				&& previa.realizadaEn().equals(s.realizadaEn().truncatedTo(ChronoUnit.MICROS))
				&& mismoDolor(previa, s, conConsentimiento);
		if (!igual) {
			throw ErrorNegocio.conflicto("Esa serie ya existe con otros datos.");
		}
	}

	/** El dolor solo se compara si hay algo guardado y consentimiento para mirarlo; lo demás no cuenta. */
	private boolean mismoDolor(FilaSerie previa, SerieEntrada s, boolean conConsentimiento) {
		if (!conConsentimiento || previa.dolor() == null) {
			return true;
		}
		return Objects.equals(descifrarDolor(previa.uuid(), previa.dolor()), s.dolor());
	}

	private static boolean iguales(java.math.BigDecimal a, java.math.BigDecimal b) {
		return a == null ? b == null : b != null && a.compareTo(b) == 0;
	}

	private List<SesionVista> vistas(UUID usuario, List<CabeceraVista> cabeceras) {
		if (cabeceras.isEmpty()) {
			return List.of();
		}
		boolean conConsentimiento = consentimientos.tiene(usuario, TipoConsentimiento.LIMITACIONES);
		Map<UUID, List<SerieVista>> porSesion = sesiones
				.vigentes(cabeceras.stream().map(CabeceraVista::uuid).toList()).stream()
				.collect(Collectors.groupingBy(SerieVigente::sesionUuid,
						Collectors.mapping(s -> aVista(s, conConsentimiento), Collectors.toList())));
		return cabeceras.stream().map(c -> new SesionVista(c.uuid(), c.planVersion(), c.diaPlan(), c.realizadaEn(),
				c.recibidaEn(), c.relojDesfasado(), porSesion.getOrDefault(c.uuid(), List.of()))).toList();
	}

	private SerieVista aVista(SerieVigente s, boolean conConsentimiento) {
		Integer dolor = conConsentimiento && s.dolor() != null ? descifrarDolor(s.uuid(), s.dolor()) : null;
		return new SerieVista(s.uuid(), s.ejercicio(), s.orden(), s.pesoKg(), s.repeticiones(), s.rir(), s.rpe(),
				s.calentamiento(), dolor, s.corrigeA(), s.anulada(), s.realizadaEn());
	}

	private byte[] cifrarDolor(UUID serie, int dolor) {
		return cifrador.cifrar(TABLA_SERIE, COLUMNA_DOLOR, serie,
				Integer.toString(dolor).getBytes(StandardCharsets.UTF_8));
	}

	private Integer descifrarDolor(UUID serie, byte[] cifrado) {
		return Integer.valueOf(new String(cifrador.descifrar(TABLA_SERIE, COLUMNA_DOLOR, serie, cifrado),
				StandardCharsets.UTF_8));
	}

	private long idDe(UUID usuario) {
		return usuarios.porUuid(usuario).orElseThrow(() -> ErrorNegocio.noAutenticado("La sesión ya no es válida."))
				.id();
	}
}
