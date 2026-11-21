package es.forja.sesiones.modelo;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Racha semanal y cifras del perfil a partir de las sesiones hechas. Una semana (de lunes a domingo) cuenta si se
 * entrena al menos {@code objetivoSemanal} días distintos; descansar el resto no rompe nada, y la semana en curso
 * solo suma cuando ya se ha cumplido (no corta la racha mientras dura).
 *
 * <p>shortcut: el objetivo es el de hoy para todo el historial; si alguien cambia de 3 a 4 días, sus semanas viejas
 * se miden con 4. Guardar el objetivo por semana si molesta.
 *
 * @param kgMovidos suma de peso × repeticiones de las series de trabajo (con peso corporal cuenta 0)
 * @param porcentajeCumplido días hechos sobre los previstos (como mucho el objetivo por semana) en las semanas ya
 *            acabadas desde la primera sesión
 */
public record ResumenProgreso(int sesiones, BigDecimal kgMovidos, int diasEntrenados, int objetivoSemanal,
		int hechosEstaSemana, int semanasDeRacha, int mejorRacha, int porcentajeCumplido,
		List<SemanaProgreso> ultimasSemanas) {

	public static final int SEMANAS_EN_EL_MAPA = 12;
	private static final int DIAS_SEMANA = 7;
	private static final int CIEN = 100;

	/** Una sesión con alguna serie de trabajo, en el día de la zona horaria del usuario. */
	public record SesionHecha(LocalDate dia, BigDecimal kg) {
	}

	/** Una semana del mapa: si se entrenó cada día, de lunes a domingo. */
	public record SemanaProgreso(LocalDate lunes, List<Boolean> dias) {
	}

	public static ResumenProgreso de(List<SesionHecha> hechas, int objetivoSemanal, LocalDate hoy) {
		LocalDate lunesActual = lunesDe(hoy);
		Set<LocalDate> dias = hechas.stream().map(SesionHecha::dia).collect(Collectors.toSet());
		Map<LocalDate, Long> diasPorSemana = dias.stream()
				.collect(Collectors.groupingBy(ResumenProgreso::lunesDe, TreeMap::new, Collectors.counting()));
		BigDecimal kg = hechas.stream().map(SesionHecha::kg).reduce(BigDecimal.ZERO, BigDecimal::add);
		int estaSemana = diasPorSemana.getOrDefault(lunesActual, 0L).intValue();
		return new ResumenProgreso(hechas.size(), kg, dias.size(), objetivoSemanal, estaSemana,
				racha(diasPorSemana, objetivoSemanal, lunesActual), mejorRacha(diasPorSemana, objetivoSemanal, lunesActual),
				porcentaje(diasPorSemana, objetivoSemanal, lunesActual), mapa(dias, lunesActual));
	}

	private static LocalDate lunesDe(LocalDate dia) {
		return dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
	}

	private static boolean cumplida(Map<LocalDate, Long> diasPorSemana, LocalDate lunes, int objetivo) {
		return diasPorSemana.getOrDefault(lunes, 0L) >= objetivo;
	}

	/** Semanas cumplidas seguidas hasta la anterior, más la actual si ya se cumplió. */
	private static int racha(Map<LocalDate, Long> diasPorSemana, int objetivo, LocalDate lunesActual) {
		int racha = cumplida(diasPorSemana, lunesActual, objetivo) ? 1 : 0;
		LocalDate lunes = lunesActual.minusWeeks(1);
		while (cumplida(diasPorSemana, lunes, objetivo)) {
			racha++;
			lunes = lunes.minusWeeks(1);
		}
		return racha;
	}

	private static int mejorRacha(Map<LocalDate, Long> diasPorSemana, int objetivo, LocalDate lunesActual) {
		if (diasPorSemana.isEmpty()) {
			return 0;
		}
		int mejor = 0;
		int seguidas = 0;
		for (LocalDate lunes = diasPorSemana.keySet().iterator().next(); !lunes.isAfter(lunesActual);
				lunes = lunes.plusWeeks(1)) {
			seguidas = cumplida(diasPorSemana, lunes, objetivo) ? seguidas + 1 : 0;
			mejor = Math.max(mejor, seguidas);
		}
		return mejor;
	}

	private static int porcentaje(Map<LocalDate, Long> diasPorSemana, int objetivo, LocalDate lunesActual) {
		if (diasPorSemana.isEmpty() || objetivo <= 0) {
			return 0;
		}
		LocalDate primera = diasPorSemana.keySet().iterator().next();
		long semanas = ChronoUnit.WEEKS.between(primera, lunesActual);
		if (semanas <= 0) {
			return 0;
		}
		long hechos = diasPorSemana.entrySet().stream().filter(e -> e.getKey().isBefore(lunesActual))
				.mapToLong(e -> Math.min(e.getValue(), objetivo)).sum();
		return (int) (hechos * CIEN / (semanas * objetivo));
	}

	private static List<SemanaProgreso> mapa(Set<LocalDate> dias, LocalDate lunesActual) {
		List<SemanaProgreso> semanas = new ArrayList<>();
		for (int s = SEMANAS_EN_EL_MAPA - 1; s >= 0; s--) {
			LocalDate lunes = lunesActual.minusWeeks(s);
			List<Boolean> entrenado = new ArrayList<>();
			for (int d = 0; d < DIAS_SEMANA; d++) {
				entrenado.add(dias.contains(lunes.plusDays(d)));
			}
			semanas.add(new SemanaProgreso(lunes, List.copyOf(entrenado)));
		}
		return List.copyOf(semanas);
	}
}
