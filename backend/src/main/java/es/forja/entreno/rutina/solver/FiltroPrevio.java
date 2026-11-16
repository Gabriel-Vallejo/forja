package es.forja.entreno.rutina.solver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import es.forja.entreno.rutina.modelo.EjercicioRutina;
import es.forja.entreno.rutina.modelo.EntradaRutina;
import es.forja.entreno.rutina.modelo.PlanRutina.Descarte;
import es.forja.restricciones.filtro.Decision;
import es.forja.restricciones.filtro.FiltroRestricciones;

/**
 * Filtro previo del modelo (ADR 0002 §1): material, exclusiones del usuario y {@link FiltroRestricciones}, en ese
 * orden, para que un sustituto también tenga material y no esté excluido. Solo entran los curados aptos para fuerza.
 */
final class FiltroPrevio {

	static final String MATERIAL = "material";
	static final String EXCLUSION = "exclusion";
	static final String RESTRICCION = "restriccion";
	static final String SUSTITUCION = "sustitucion";

	private static final FiltroRestricciones FILTRO = new FiltroRestricciones();

	/**
	 * @param candidatos ordenados por {@code idExterno}
	 * @param anotaciones avisos del filtro por {@code idExterno} del candidato
	 */
	record Resultado(List<EjercicioRutina> candidatos, Map<String, List<String>> anotaciones,
			List<Descarte> descartes) {
	}

	private FiltroPrevio() {
	}

	static Resultado aplicar(List<EjercicioRutina> catalogo, EntradaRutina entrada) {
		List<Descarte> descartes = new ArrayList<>();
		List<EjercicioRutina> validos = new ArrayList<>();
		catalogo.stream().filter(e -> e.candidato().curado() && e.aptoFuerza())
				.sorted(Comparator.comparing(EjercicioRutina::idExterno)).forEach(e -> {
					if (!entrada.material().contains(e.candidato().implemento())) {
						descartes.add(descarte(e, MATERIAL, "Necesita "
								+ e.nombreMaterial().toLowerCase(Locale.ROOT) + " y no lo tienes a mano."));
					} else if (entrada.exclusiones().contains(e.idExterno())) {
						descartes.add(descarte(e, EXCLUSION, "Lo has quitado tú."));
					} else {
						validos.add(e);
					}
				});
		return aplicarRestricciones(validos, entrada, descartes);
	}

	private static Resultado aplicarRestricciones(List<EjercicioRutina> validos, EntradaRutina entrada,
			List<Descarte> descartes) {
		Map<String, EjercicioRutina> porId = validos.stream()
				.collect(Collectors.toMap(EjercicioRutina::idExterno, Function.identity()));
		Map<String, Decision> decisiones = FILTRO.aplicar(
				validos.stream().map(EjercicioRutina::candidato).toList(), entrada.restricciones());
		Map<String, List<String>> anotaciones = new TreeMap<>();
		decisiones.forEach((id, decision) -> {
			EjercicioRutina e = porId.get(id);
			switch (decision) {
				case Decision.Permitido p -> anotaciones.putIfAbsent(id, p.anotaciones());
				case Decision.Excluido x -> descartes.add(descarte(e, RESTRICCION, x.motivo()));
				case Decision.Sustituido s -> {
					anotaciones.putIfAbsent(s.porIdExterno(), s.anotaciones());
					descartes.add(descarte(e, SUSTITUCION, s.motivo()));
				}
			}
		});
		List<EjercicioRutina> candidatos = anotaciones.keySet().stream().map(porId::get).toList();
		return new Resultado(candidatos, anotaciones, List.copyOf(descartes));
	}

	private static Descarte descarte(EjercicioRutina e, String tipo, String motivo) {
		return new Descarte(e.idExterno(), e.nombre(), tipo, motivo);
	}
}
