package es.forja.entreno.rutina.solver;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import es.forja.entreno.rutina.modelo.EjercicioRutina;
import es.forja.entreno.rutina.modelo.EntradaRutina;
import es.forja.entreno.rutina.modelo.PlanRutina.Holgura;
import es.forja.restricciones.modelo.Patron;

/**
 * Explica los patrones principales que faltan en el plan: si no queda ninguna variante compuesta (y por qué: material,
 * lo que el usuario quitó o sus restricciones) o si había variantes pero no caben en sus días y minutos.
 */
final class TrazaBasicos {

	private static final double UNO = 1.0;

	private TrazaBasicos() {
	}

	/**
	 * @param catalogo el catálogo entero, para saber qué variantes había antes del filtro
	 * @param elegidos los ejercicios del plan
	 */
	static List<Holgura> holguras(List<String> patrones, EntradaRutina entrada, List<EjercicioRutina> catalogo,
			FiltroPrevio.Resultado filtro, List<EjercicioRutina> elegidos) {
		return patrones.stream().filter(codigo -> elegidos.stream().noneMatch(e -> esBasico(e, codigo)))
				.map(codigo -> new Holgura(codigo, Textos.BASICO, UNO, mensaje(codigo, entrada, catalogo, filtro)))
				.toList();
	}

	private static boolean esBasico(EjercicioRutina e, String codigo) {
		return e.compuesto() && e.patron() == Patron.desdeCodigo(codigo);
	}

	private static String mensaje(String codigo, EntradaRutina entrada, List<EjercicioRutina> catalogo,
			FiltroPrevio.Resultado filtro) {
		String nombre = Patron.desdeCodigo(codigo).nombre();
		if (filtro.candidatos().stream().anyMatch(e -> esBasico(e, codigo))) {
			return Textos.basicoNoCabe(nombre, entrada.dias(), entrada.minutos());
		}
		Set<String> variantes = catalogo.stream()
				.filter(e -> e.candidato().curado() && e.aptoFuerza() && esBasico(e, codigo))
				.map(EjercicioRutina::idExterno).collect(Collectors.toSet());
		// Una sustitución también es cosa de las restricciones: el filtro la cambió por otra variante.
		Set<String> tipos = filtro.descartes().stream().filter(d -> variantes.contains(d.idExterno()))
				.map(d -> FiltroPrevio.SUSTITUCION.equals(d.tipo()) ? FiltroPrevio.RESTRICCION : d.tipo())
				.collect(Collectors.toSet());
		return Textos.basicoSinVariantes(nombre, tipos);
	}
}
