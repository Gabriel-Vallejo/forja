package es.forja.entreno.plan.modelo;

import java.math.BigDecimal;

/** Una serie de trabajo vigente (sin calentamiento) tal como la ve la regla de la siguiente sesión. */
public record SerieTrabajo(BigDecimal pesoKg, int repeticiones, Integer rir) {
}
