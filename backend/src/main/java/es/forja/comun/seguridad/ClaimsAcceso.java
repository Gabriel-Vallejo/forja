package es.forja.comun.seguridad;

import java.util.UUID;

/** Lo que dice un token de acceso válido: quién es ({@code sub}) y desde qué dispositivo ({@code sid}). */
public record ClaimsAcceso(UUID usuario, UUID dispositivo) {
}
