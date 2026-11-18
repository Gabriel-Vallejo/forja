package es.forja.entreno.perfil.servicio;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.UUID;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.forja.comun.api.ErrorNegocio;
import es.forja.comun.cifrado.Cifrador;
import es.forja.consentimiento.modelo.ConsentimientoRevocado;
import es.forja.consentimiento.modelo.TipoConsentimiento;
import es.forja.consentimiento.servicio.ServicioConsentimientos;
import es.forja.entreno.perfil.modelo.MedidasCuerpo;
import es.forja.entreno.perfil.modelo.MedidasPerfil;
import es.forja.entreno.perfil.modelo.MedidasPerfil.GrasaEstimada;
import es.forja.entreno.perfil.repositorio.MedidasRepositorio;
import es.forja.entreno.perfil.repositorio.PerfilDatosRepositorio;
import tools.jackson.databind.json.JsonMapper;

/**
 * Altura, peso y circunferencias. Son datos de salud (ADR 0004): exigen el consentimiento de {@code medidas_salud},
 * van cifradas en una sola columna y revocarlo las borra (RF-46). El % de grasa se calcula al leer.
 */
@Service
public class ServicioMedidas {

	private static final TipoConsentimiento CONSENTIMIENTO = TipoConsentimiento.MEDIDAS_SALUD;
	private static final MedidasCuerpo SIN_MEDIDAS = new MedidasCuerpo(null, null, null, null, null);

	private final MedidasRepositorio medidas;
	private final PerfilDatosRepositorio datos;
	private final ServicioConsentimientos consentimientos;
	private final Cifrador cifrador;
	private final JsonMapper json;
	private final Clock reloj;

	public ServicioMedidas(MedidasRepositorio medidas, PerfilDatosRepositorio datos,
			ServicioConsentimientos consentimientos, Cifrador cifrador, JsonMapper json, Clock reloj) {
		this.medidas = medidas;
		this.datos = datos;
		this.consentimientos = consentimientos;
		this.cifrador = cifrador;
		this.json = json;
		this.reloj = reloj;
	}

	public MedidasPerfil obtener(UUID usuario) {
		long usuarioId = consentimientos.exigir(usuario, CONSENTIMIENTO);
		return aPerfil(usuarioId, medidas.buscar(usuarioId).map(b -> descifrar(usuario, b)).orElse(SIN_MEDIDAS));
	}

	@Transactional
	public MedidasPerfil guardar(UUID usuario, MedidasCuerpo cuerpo) {
		long usuarioId = consentimientos.exigir(usuario, CONSENTIMIENTO);
		if (cuerpo.cuelloCm() != null && cuerpo.cinturaCm() != null && cuerpo.cinturaCm() <= cuerpo.cuelloCm()) {
			throw ErrorNegocio.datosNoValidos("La cintura tiene que ser mayor que el cuello.");
		}
		byte[] claro = json.writeValueAsString(cuerpo).getBytes(StandardCharsets.UTF_8);
		medidas.guardar(usuarioId,
				cifrador.cifrar(MedidasRepositorio.TABLA, MedidasRepositorio.COLUMNA_DATOS, usuario, claro),
				reloj.instant());
		return aPerfil(usuarioId, cuerpo);
	}

	/** Revocar el consentimiento de medidas de salud borra las medidas, en la misma transacción. */
	@EventListener
	public void alRevocarUnConsentimiento(ConsentimientoRevocado revocado) {
		if (revocado.tipo() == CONSENTIMIENTO) {
			medidas.borrar(revocado.usuarioId());
		}
	}

	private MedidasPerfil aPerfil(long usuarioId, MedidasCuerpo m) {
		GrasaEstimada grasa = GrasaNavy
				.porcentaje(datos.sexo(usuarioId), m.alturaCm(), m.cuelloCm(), m.cinturaCm(), m.caderaCm())
				.stream().mapToObj(p -> new GrasaEstimada(p, GrasaNavy.MARGEN)).findFirst().orElse(null);
		return MedidasPerfil.de(m, grasa);
	}

	private MedidasCuerpo descifrar(UUID usuario, byte[] cifrado) {
		byte[] claro = cifrador.descifrar(MedidasRepositorio.TABLA, MedidasRepositorio.COLUMNA_DATOS, usuario,
				cifrado);
		return json.readValue(new String(claro, StandardCharsets.UTF_8), MedidasCuerpo.class);
	}
}
