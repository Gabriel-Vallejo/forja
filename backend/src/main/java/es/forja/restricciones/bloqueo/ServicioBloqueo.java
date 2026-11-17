package es.forja.restricciones.bloqueo;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.forja.comun.api.ErrorNegocio;
import es.forja.comun.cifrado.Cifrador;
import es.forja.consentimiento.modelo.ConsentimientoRevocado;
import es.forja.consentimiento.modelo.TipoConsentimiento;
import es.forja.cuenta.repositorio.UsuarioRepositorio;
import tools.jackson.databind.json.JsonMapper;

/**
 * Recuerda el resultado (nunca las respuestas) de la última bandera roja y del último cribado urgente, para que
 * generar el plan no dependa de que el cliente haga caso (ADR 0007, enmienda). Sin consentimiento de limitaciones no
 * puede haber filas: los cuestionarios lo exigen y revocarlo las borra.
 */
@Service
public class ServicioBloqueo {

	public static final String CODIGO_ERROR = "bandera_roja_activa";

	private record Contenido(String nivel, String evaluadoEn) {
	}

	private final BloqueoRepositorio bloqueos;
	private final UsuarioRepositorio usuarios;
	private final Cifrador cifrador;
	private final JsonMapper json;
	private final Clock reloj;

	public ServicioBloqueo(BloqueoRepositorio bloqueos, UsuarioRepositorio usuarios, Cifrador cifrador,
			JsonMapper json, Clock reloj) {
		this.bloqueos = bloqueos;
		this.usuarios = usuarios;
		this.cifrador = cifrador;
		this.json = json;
		this.reloj = reloj;
	}

	/** Pone el bloqueo del origen con ese nivel, o lo quita si el nivel es nulo. */
	@Transactional
	public void registrar(long usuarioId, OrigenBloqueo origen, NivelBloqueo nivel) {
		bloqueos.borrar(usuarioId, origen);
		if (nivel == null) {
			return;
		}
		UUID uuid = UUID.randomUUID();
		byte[] claro = json.writeValueAsString(new Contenido(nivel.codigo(), reloj.instant().toString()))
				.getBytes(StandardCharsets.UTF_8);
		bloqueos.insertar(uuid, usuarioId, origen,
				cifrador.cifrar(BloqueoRepositorio.TABLA, BloqueoRepositorio.COLUMNA_DATOS, uuid, claro));
	}

	/** El nivel más grave activo del usuario, si lo hay. */
	public Optional<NivelBloqueo> nivelActivo(UUID usuario) {
		long usuarioId = usuarios.porUuid(usuario)
				.orElseThrow(() -> ErrorNegocio.noAutenticado("La sesión ya no es válida.")).id();
		return bloqueos.deUsuario(usuarioId).stream().map(this::nivel).max(Comparator.naturalOrder());
	}

	/** 409 con código estable si hay bloqueo; el mensaje es de fitness, sin diagnóstico. */
	public void exigirSinBloqueo(UUID usuario) {
		nivelActivo(usuario).ifPresent(nivel -> {
			String mensaje = nivel.esUrgente()
					? "No generamos tu plan por ahora: ve hoy a urgencias o llama al 112 y habla con un profesional sanitario."
					: "No generamos tu plan por ahora: antes habla con un profesional sanitario.";
			throw ErrorNegocio.conflicto(mensaje, CODIGO_ERROR);
		});
	}

	@EventListener
	public void alRevocarUnConsentimiento(ConsentimientoRevocado revocado) {
		if (revocado.tipo() == TipoConsentimiento.LIMITACIONES) {
			bloqueos.borrarTodosDeUsuario(revocado.usuarioId());
		}
	}

	private NivelBloqueo nivel(BloqueoRepositorio.FilaCifrada fila) {
		byte[] claro = cifrador.descifrar(BloqueoRepositorio.TABLA, BloqueoRepositorio.COLUMNA_DATOS, fila.uuid(),
				fila.datos());
		Contenido c = json.readValue(new String(claro, StandardCharsets.UTF_8), Contenido.class);
		return NivelBloqueo.valueOf(c.nivel().toUpperCase(java.util.Locale.ROOT));
	}
}
