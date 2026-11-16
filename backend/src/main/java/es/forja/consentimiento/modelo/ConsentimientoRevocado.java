package es.forja.consentimiento.modelo;

/**
 * Se publica, dentro de la transacción de la revocación, para que cada dominio borre los datos que
 * dependían del consentimiento (ADR 0004, §3) sin que consentimiento conozca sus tablas.
 */
public record ConsentimientoRevocado(long usuarioId, TipoConsentimiento tipo) {
}
