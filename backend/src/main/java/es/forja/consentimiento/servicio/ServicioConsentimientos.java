package es.forja.consentimiento.servicio;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.forja.comun.api.ErrorNegocio;
import es.forja.consentimiento.modelo.Consentimiento;
import es.forja.consentimiento.modelo.ConsentimientoRevocado;
import es.forja.consentimiento.modelo.TipoConsentimiento;
import es.forja.consentimiento.repositorio.ConsentimientoRepositorio;
import es.forja.cuenta.repositorio.UsuarioRepositorio;

/** Consentimiento explícito del art. 9.2.a del RGPD (ADR 0004): darlo, consultarlo, revocarlo y exigirlo. */
@Service
public class ServicioConsentimientos {

	private final ConsentimientoRepositorio consentimientos;
	private final UsuarioRepositorio usuarios;
	private final ApplicationEventPublisher eventos;
	private final Clock reloj;

	public ServicioConsentimientos(ConsentimientoRepositorio consentimientos, UsuarioRepositorio usuarios,
			ApplicationEventPublisher eventos, Clock reloj) {
		this.consentimientos = consentimientos;
		this.usuarios = usuarios;
		this.eventos = eventos;
		this.reloj = reloj;
	}

	public List<Consentimiento> listar(UUID usuario) {
		return consentimientos.vivos(idDe(usuario));
	}

	/**
	 * Da el consentimiento con la versión de texto que el cliente enseñó. Si ya está vivo con esa versión no
	 * hace nada; si la versión no es la vigente, 409.
	 */
	@Transactional
	public Consentimiento dar(UUID usuario, TipoConsentimiento tipo, String versionTexto) {
		if (!tipo.versionVigente().equals(versionTexto)) {
			throw ErrorNegocio.conflicto("El texto del consentimiento ha cambiado: vuelve a enseñárselo al usuario.");
		}
		long usuarioId = idDe(usuario);
		Optional<Consentimiento> vivo = consentimientos.vivo(usuarioId, tipo);
		if (vivo.isPresent() && vivo.get().versionTexto().equals(versionTexto)) {
			return vivo.get();
		}
		Instant ahora = reloj.instant();
		consentimientos.revocar(usuarioId, tipo, ahora);
		try {
			consentimientos.insertar(usuarioId, tipo, versionTexto, ahora);
		} catch (DuplicateKeyException e) {
			// Otra petición lo dio a la vez: el resultado es el mismo que si hubiéramos llegado segundos después.
			return consentimientos.vivo(usuarioId, tipo).orElseThrow(() -> e);
		}
		return new Consentimiento(tipo, versionTexto, ahora);
	}

	/**
	 * Revoca el consentimiento y, en la misma transacción, avisa a los dominios para que borren los datos que
	 * dependían de él. Revocar uno que no estaba dado no falla.
	 */
	@Transactional
	public void revocar(UUID usuario, TipoConsentimiento tipo) {
		long usuarioId = idDe(usuario);
		consentimientos.revocar(usuarioId, tipo, reloj.instant());
		eventos.publishEvent(new ConsentimientoRevocado(usuarioId, tipo));
	}

	/** Falla con 403 si el consentimiento no está vivo. Devuelve el id interno del usuario, para quien lo necesite. */
	public long exigir(UUID usuario, TipoConsentimiento tipo) {
		long usuarioId = idDe(usuario);
		if (consentimientos.vivo(usuarioId, tipo).isEmpty()) {
			throw ErrorNegocio.prohibido("Falta el consentimiento de " + tipo.codigo() + ".");
		}
		return usuarioId;
	}

	private long idDe(UUID usuario) {
		return usuarios.porUuid(usuario).orElseThrow(() -> ErrorNegocio.noAutenticado("La sesión ya no es válida."))
				.id();
	}
}
