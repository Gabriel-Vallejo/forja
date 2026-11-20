package es.forja.consentimiento.modelo;

import java.time.Instant;

/** Un consentimiento tal como quedó registrado: vivo ({@code revocadoEn} nulo) o retirado. */
public record RegistroConsentimiento(TipoConsentimiento tipo, String versionTexto, Instant dadoEn,
		Instant revocadoEn) {
}
