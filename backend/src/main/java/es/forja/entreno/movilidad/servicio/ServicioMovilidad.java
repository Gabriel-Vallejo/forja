package es.forja.entreno.movilidad.servicio;

import java.time.Clock;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.forja.comun.api.ErrorNegocio;
import es.forja.consentimiento.modelo.ConsentimientoRevocado;
import es.forja.consentimiento.modelo.TipoConsentimiento;
import es.forja.consentimiento.servicio.ServicioConsentimientos;
import es.forja.entreno.movilidad.modelo.Movilidad;
import es.forja.entreno.movilidad.modelo.TestMovilidad;
import es.forja.entreno.movilidad.modelo.Tramo;
import es.forja.entreno.movilidad.modelo.TramosMovilidad;
import es.forja.entreno.movilidad.repositorio.MovilidadRepositorio;

/**
 * Tests de movilidad del usuario (RF-08). Es una medida del cuerpo: como el ADR 0004 trata por exceso todo lo dudoso
 * como dato de salud, exige el consentimiento de {@code medidas_salud}, y revocarlo borra los tests (RF-46).
 */
@Service
public class ServicioMovilidad {

	private static final TipoConsentimiento CONSENTIMIENTO = TipoConsentimiento.MEDIDAS_SALUD;

	private final MovilidadRepositorio repositorio;
	private final ServicioConsentimientos consentimientos;
	private final Clock reloj;

	public ServicioMovilidad(MovilidadRepositorio repositorio, ServicioConsentimientos consentimientos, Clock reloj) {
		this.repositorio = repositorio;
		this.consentimientos = consentimientos;
		this.reloj = reloj;
	}

	public Movilidad obtener(UUID usuario) {
		return Movilidad.de(repositorio.deUsuario(consentimientos.exigir(usuario, CONSENTIMIENTO)));
	}

	@Transactional
	public Movilidad guardar(UUID usuario, TramosMovilidad cuerpo) {
		long usuarioId = consentimientos.exigir(usuario, CONSENTIMIENTO);
		Map<TestMovilidad, Tramo> tramos;
		try {
			tramos = cuerpo.aMapa();
		} catch (IllegalArgumentException e) {
			throw ErrorNegocio.datosNoValidos(e.getMessage());
		}
		repositorio.guardar(usuarioId, tramos, reloj.instant());
		return Movilidad.de(repositorio.deUsuario(usuarioId));
	}

	/** Los tramos que usa la rutina: los del usuario si tiene el consentimiento y, si no, ninguno. */
	public Map<TestMovilidad, Tramo> paraLaRutina(UUID usuario) {
		Map<TestMovilidad, Tramo> tramos = new EnumMap<>(TestMovilidad.class);
		if (!consentimientos.tiene(usuario, CONSENTIMIENTO)) {
			return tramos;
		}
		repositorio.deUsuario(consentimientos.exigir(usuario, CONSENTIMIENTO))
				.forEach((test, r) -> Tramo.desdeCodigo(r.tramo()).ifPresent(t -> tramos.put(test, t)));
		return tramos;
	}

	/** Revocar el consentimiento de medidas de salud borra los tests, en la misma transacción. */
	@EventListener
	public void alRevocarUnConsentimiento(ConsentimientoRevocado revocado) {
		if (revocado.tipo() == CONSENTIMIENTO) {
			repositorio.borrarTodos(revocado.usuarioId());
		}
	}
}
