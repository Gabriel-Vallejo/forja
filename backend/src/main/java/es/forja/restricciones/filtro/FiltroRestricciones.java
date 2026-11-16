package es.forja.restricciones.filtro;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import es.forja.restricciones.modelo.Restriccion;

/**
 * Filtro previo del solver (ADR 0002, enmienda de 2026-11-16, y ADR 0007).
 *
 * <p>Solo {@code evitar} y las restricciones de origen profesional excluyen. Las demás sustituyen
 * por la escalera: misma familia y menos rango; misma familia y otro implemento; otro curado del
 * mismo patrón; y si no hay nada compatible, se excluye con el motivo «sin variante compatible».
 * Es una función pura y determinista: ante un empate manda el menor {@code idExterno}.
 */
public class FiltroRestricciones {

	/** Decisión por ejercicio, indexada y ordenada por {@code idExterno}. */
	public Map<String, Decision> aplicar(List<EjercicioCandidato> catalogo, List<Restriccion> restricciones) {
		comprobarIdsUnicos(catalogo);
		List<Restriccion> activas = restricciones.stream().filter(Restriccion::restringe).toList();
		List<EjercicioCandidato> ordenado = catalogo.stream()
				.sorted(Comparator.comparing(EjercicioCandidato::idExterno)).toList();
		List<EjercicioCandidato> sustitutos = ordenado.stream()
				.filter(e -> e.curado() && Reglas.cumple(e, activas)).toList();

		Map<String, Decision> decisiones = new LinkedHashMap<>();
		for (EjercicioCandidato e : ordenado) {
			decisiones.put(e.idExterno(), decidir(e, activas, sustitutos));
		}
		return Collections.unmodifiableMap(decisiones);
	}

	/** Si el ejercicio cumple todas las restricciones activas; la usan los tests de propiedades. */
	public static boolean cumple(EjercicioCandidato ejercicio, List<Restriccion> restricciones) {
		return Reglas.cumple(ejercicio, restricciones.stream().filter(Restriccion::restringe).toList());
	}

	private static Decision decidir(EjercicioCandidato e, List<Restriccion> activas,
			List<EjercicioCandidato> sustitutos) {
		Optional<Violacion> principal = Reglas.principal(Reglas.violaciones(e, activas));
		if (principal.isEmpty()) {
			return new Decision.Permitido(Reglas.anotaciones(e, activas));
		}
		Restriccion motivo = principal.get().restriccion();
		if (principal.get().dura()) {
			return new Decision.Excluido(Motivos.excluido(motivo, e));
		}
		return escalera(e, sustitutos)
				.<Decision>map(s -> new Decision.Sustituido(s.idExterno(), Motivos.sustituido(motivo, e, s),
						Reglas.anotaciones(s, activas)))
				.orElseGet(() -> new Decision.Excluido(Motivos.sinVariante(motivo, e)));
	}

	/** Primer sustituto de la escalera; los candidatos ya vienen válidos y ordenados por idExterno. */
	private static Optional<EjercicioCandidato> escalera(EjercicioCandidato e, List<EjercicioCandidato> validos) {
		Predicate<EjercicioCandidato> mismaFamilia = v -> e.familiaSustitucion() != null
				&& e.familiaSustitucion().equals(v.familiaSustitucion());
		return primero(validos, mismaFamilia.and(v -> v.rango().esMasCortoQue(e.rango())))
				.or(() -> primero(validos, mismaFamilia.and(v -> !v.implemento().equals(e.implemento()))))
				.or(() -> primero(validos, v -> v.patron() == e.patron()));
	}

	private static Optional<EjercicioCandidato> primero(List<EjercicioCandidato> candidatos,
			Predicate<EjercicioCandidato> condicion) {
		return candidatos.stream().filter(condicion).findFirst();
	}

	private static void comprobarIdsUnicos(List<EjercicioCandidato> catalogo) {
		Set<String> vistos = new HashSet<>();
		for (EjercicioCandidato e : catalogo) {
			if (!vistos.add(e.idExterno())) {
				throw new IllegalArgumentException("idExterno repetido en el catálogo: " + e.idExterno());
			}
		}
	}

}
