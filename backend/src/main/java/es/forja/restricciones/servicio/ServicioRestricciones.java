package es.forja.restricciones.servicio;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.forja.comun.api.ErrorNegocio;
import es.forja.comun.cifrado.Cifrador;
import es.forja.consentimiento.modelo.ConsentimientoRevocado;
import es.forja.consentimiento.modelo.TipoConsentimiento;
import es.forja.consentimiento.servicio.ServicioConsentimientos;
import es.forja.restricciones.modelo.Restriccion;
import es.forja.restricciones.modelo.RestriccionCodificada;
import es.forja.restricciones.repositorio.RestriccionRepositorio;
import es.forja.restricciones.repositorio.RestriccionRepositorio.FilaCifrada;
import tools.jackson.databind.json.JsonMapper;

/**
 * Limitaciones funcionales del usuario (ADR 0007). Todo exige el consentimiento de {@code limitaciones}, y en
 * la base solo hay la restricción entera cifrada (ADR 0004, enmienda 8).
 */
@Service
public class ServicioRestricciones {

	public static final int MAXIMO_POR_USUARIO = 30;
	public static final int MAXIMO_TEXTO_ORIGINAL = 500;
	public static final int MAXIMO_PARAMETRO = 40;

	private final RestriccionRepositorio restricciones;
	private final ServicioConsentimientos consentimientos;
	private final Cifrador cifrador;
	private final JsonMapper json;
	private final Clock reloj;

	public ServicioRestricciones(RestriccionRepositorio restricciones, ServicioConsentimientos consentimientos,
			Cifrador cifrador, JsonMapper json, Clock reloj) {
		this.restricciones = restricciones;
		this.consentimientos = consentimientos;
		this.cifrador = cifrador;
		this.json = json;
		this.reloj = reloj;
	}

	public List<RestriccionGuardada> listar(UUID usuario) {
		return listarDeUsuarioId(exigir(usuario));
	}

	/** Las del usuario con el consentimiento ya comprobado; la usa también el cálculo del efecto. */
	public List<RestriccionGuardada> listarDeUsuarioId(long usuarioId) {
		return restricciones.deUsuario(usuarioId).stream().map(this::descifrar).toList();
	}

	@Transactional
	public RestriccionGuardada crear(UUID usuario, RestriccionCodificada datos) {
		long usuarioId = exigir(usuario);
		Restriccion restriccion = validar(datos);
		// shortcut: contar y luego insertar no es atómico; dos altas simultáneas pueden pasar de 30 por una, cuando haya más tráfico se bloquea la fila del usuario.
		if (restricciones.contar(usuarioId) >= MAXIMO_POR_USUARIO) {
			throw ErrorNegocio.conflicto("Has llegado al máximo de " + MAXIMO_POR_USUARIO + " limitaciones.");
		}
		UUID uuid = UUID.randomUUID();
		restricciones.insertar(uuid, usuarioId, cifrar(uuid, restriccion), reloj.instant());
		return new RestriccionGuardada(uuid, restriccion);
	}

	@Transactional
	public RestriccionGuardada actualizar(UUID usuario, UUID uuid, RestriccionCodificada datos) {
		long usuarioId = exigir(usuario);
		Restriccion restriccion = validar(datos);
		if (restricciones.actualizar(usuarioId, uuid, cifrar(uuid, restriccion), reloj.instant()) == 0) {
			throw noEncontrada();
		}
		return new RestriccionGuardada(uuid, restriccion);
	}

	@Transactional
	public void borrar(UUID usuario, UUID uuid) {
		if (restricciones.borrar(exigir(usuario), uuid) == 0) {
			throw noEncontrada();
		}
	}

	/** Revocar el consentimiento de limitaciones borra todas las restricciones, en la misma transacción. */
	@EventListener
	public void alRevocarUnConsentimiento(ConsentimientoRevocado revocado) {
		if (revocado.tipo() == TipoConsentimiento.LIMITACIONES) {
			restricciones.borrarTodasDeUsuario(revocado.usuarioId());
		}
	}

	public long exigir(UUID usuario) {
		return consentimientos.exigir(usuario, TipoConsentimiento.LIMITACIONES);
	}

	private static ErrorNegocio noEncontrada() {
		return ErrorNegocio.noEncontrado("No existe esa limitación.");
	}

	/** Longitudes y códigos; el constructor de {@link Restriccion} pone el resto de reglas. */
	private static Restriccion validar(RestriccionCodificada datos) {
		if (datos.textoOriginal() != null && datos.textoOriginal().length() > MAXIMO_TEXTO_ORIGINAL) {
			throw ErrorNegocio.datosNoValidos(
					"El texto original admite como máximo " + MAXIMO_TEXTO_ORIGINAL + " caracteres.");
		}
		if (datos.parametro() != null && datos.parametro().length() > MAXIMO_PARAMETRO) {
			throw ErrorNegocio.datosNoValidos("El parámetro admite como máximo " + MAXIMO_PARAMETRO + " caracteres.");
		}
		try {
			return datos.aRestriccion();
		} catch (IllegalArgumentException e) {
			throw ErrorNegocio.datosNoValidos(e.getMessage());
		}
	}

	private byte[] cifrar(UUID uuid, Restriccion restriccion) {
		byte[] claro = json.writeValueAsString(RestriccionCodificada.de(restriccion)).getBytes(StandardCharsets.UTF_8);
		return cifrador.cifrar(RestriccionRepositorio.TABLA, RestriccionRepositorio.COLUMNA_DATOS, uuid, claro);
	}

	private RestriccionGuardada descifrar(FilaCifrada fila) {
		byte[] claro = cifrador.descifrar(RestriccionRepositorio.TABLA, RestriccionRepositorio.COLUMNA_DATOS,
				fila.uuid(), fila.datos());
		RestriccionCodificada datos = json.readValue(new String(claro, StandardCharsets.UTF_8),
				RestriccionCodificada.class);
		return new RestriccionGuardada(fila.uuid(), datos.aRestriccion());
	}
}
