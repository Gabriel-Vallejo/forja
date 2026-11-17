package es.forja.entreno.perfil.modelo;

import java.util.List;

/** Ajustes del plan del usuario; {@code porDefecto} es true si aún no ha guardado los suyos. */
public record PerfilEntreno(int diasPorSemana, int minutosPorSesion, List<String> material, boolean porDefecto) {
}
