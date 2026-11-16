package es.forja.consentimiento.modelo;

import java.time.Instant;

/** Consentimiento vivo: qué tipo, qué versión del texto se enseñó y cuándo se dio. */
public record Consentimiento(TipoConsentimiento tipo, String versionTexto, Instant dadoEn) {
}
