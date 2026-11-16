package es.forja.entreno.plan.modelo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** e1RM de una sesión; nulo si ninguna serie de trabajo cuenta (demasiadas repeticiones o sin carga). */
public record PuntoE1rm(UUID sesion, Instant realizadaEn, BigDecimal e1rm) {
}
