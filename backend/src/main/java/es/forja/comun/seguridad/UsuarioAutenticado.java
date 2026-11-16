package es.forja.comun.seguridad;

import java.util.UUID;

/** Principal que deja el filtro JWT en el contexto de seguridad. */
public record UsuarioAutenticado(UUID usuario, UUID dispositivo) {
}
