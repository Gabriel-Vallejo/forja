package es.forja.entreno.rutina.solver;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import es.forja.entreno.rutina.modelo.CatalogoRutina;
import es.forja.entreno.rutina.modelo.EjercicioRutina;
import es.forja.entreno.rutina.modelo.EntradaRutina;
import es.forja.restricciones.filtro.Agarre;
import es.forja.restricciones.filtro.EjercicioCandidato;
import es.forja.restricciones.filtro.RangoMovimiento;
import es.forja.restricciones.modelo.Estado;
import es.forja.restricciones.modelo.Lado;
import es.forja.restricciones.modelo.Origen;
import es.forja.restricciones.modelo.Patron;
import es.forja.restricciones.modelo.Region;
import es.forja.restricciones.modelo.Restriccion;
import es.forja.restricciones.modelo.TipoRestriccion;

/**
 * Catálogos y entradas pequeños y sintéticos con semilla fija, para las propiedades del generador. Cada caso trae
 * un {@code evitar} sobre un patrón al azar para comprobar que ese patrón no aparece.
 */
final class CasosAleatorios {

	static final List<Patron> PATRONES = List.of(Patron.SENTADILLA, Patron.BISAGRA, Patron.EMPUJE_HORIZONTAL,
			Patron.TRACCION_VERTICAL, Patron.AISLAMIENTO_BRAZO);
	static final List<String> MUSCULOS = List.of("pecho", "dorsales", "cuadriceps", "isquiotibiales", "biceps",
			"triceps");
	static final List<String> MATERIALES = List.of("barra", "mancuerna", "maquina");
	private static final List<Region> REGIONES = List.of(Region.HOMBRO, Region.RODILLA, Region.COLUMNA_LUMBAR);
	private static final List<String> FAMILIAS = List.of("f1", "f2", "f3");
	private static final List<TipoRestriccion> LIMITAR = List.of(TipoRestriccion.LIMITAR_RANGO,
			TipoRestriccion.SIN_CARGA_AXIAL, TipoRestriccion.SIN_POR_ENCIMA_CABEZA, TipoRestriccion.LIMITAR_CARGA);
	private static final int MIN_EJERCICIOS = 4;
	private static final int MAX_EJERCICIOS = 14;
	private static final int MAX_EXCLUSIONES = 2;
	private static final int MAX_LIMITAR = 2;
	private static final int PASO_MINUTOS = 15;

	record Caso(CatalogoRutina catalogo, EntradaRutina entrada, Patron patronEvitado) {

		Map<String, EjercicioRutina> porId() {
			Map<String, EjercicioRutina> mapa = new LinkedHashMap<>();
			catalogo.ejercicios().forEach(e -> mapa.put(e.idExterno(), e));
			return mapa;
		}
	}

	private CasosAleatorios() {
	}

	static Caso caso(long semilla) {
		Random azar = new Random(semilla);
		List<EjercicioRutina> ejercicios = catalogo(azar);
		Patron evitado = elegir(azar, PATRONES);
		List<Restriccion> restricciones = new ArrayList<>(limitaciones(azar));
		restricciones.add(new Restriccion(null, Lado.AMBOS, evitado, TipoRestriccion.EVITAR, null, Estado.ACTUAL,
				Origen.USUARIO, null));
		EntradaRutina entrada = new EntradaRutina(2 + azar.nextInt(5), 30 + PASO_MINUTOS * azar.nextInt(7),
				material(azar), exclusiones(azar, ejercicios), prioritarios(azar), 1 + azar.nextInt(2),
				List.copyOf(restricciones));
		Map<String, String> nombres = new LinkedHashMap<>();
		MUSCULOS.forEach(m -> nombres.put(m, "Músculo " + m));
		return new Caso(new CatalogoRutina(ejercicios, nombres), entrada, evitado);
	}

	private static <T> T elegir(Random azar, List<T> opciones) {
		return opciones.get(azar.nextInt(opciones.size()));
	}

	private static List<EjercicioRutina> catalogo(Random azar) {
		int tamano = MIN_EJERCICIOS + azar.nextInt(MAX_EJERCICIOS - MIN_EJERCICIOS + 1);
		List<EjercicioRutina> lista = new ArrayList<>();
		for (int i = 0; i < tamano; i++) {
			lista.add(ejercicio(azar, i));
		}
		return List.copyOf(lista);
	}

	private static EjercicioRutina ejercicio(Random azar, int indice) {
		Set<Region> regiones = EnumSet.noneOf(Region.class);
		REGIONES.forEach(r -> {
			if (azar.nextBoolean()) {
				regiones.add(r);
			}
		});
		String material = elegir(azar, MATERIALES);
		EjercicioCandidato candidato = new EjercicioCandidato("e%02d".formatted(indice), "Ejercicio " + indice,
				elegir(azar, PATRONES), regiones, azar.nextBoolean(), false, Agarre.NINGUNO, azar.nextInt(4) == 0,
				azar.nextInt(5) == 0 ? null : elegir(azar, FAMILIAS), elegir(azar, List.of(RangoMovimiento.values())),
				material, azar.nextInt(6) > 0);
		return new EjercicioRutina(candidato, azar.nextInt(6) > 0, azar.nextBoolean(), azar.nextInt(3) == 0,
				material, musculos(azar));
	}

	private static Map<String, Integer> musculos(Random azar) {
		Map<String, Integer> musculos = new LinkedHashMap<>();
		musculos.put(elegir(azar, MUSCULOS), EjercicioRutina.PESO_PRIMARIO);
		int secundarios = azar.nextInt(3);
		for (int i = 0; i < secundarios; i++) {
			musculos.putIfAbsent(elegir(azar, MUSCULOS), EjercicioRutina.PESO_SECUNDARIO);
		}
		return musculos;
	}

	private static Set<String> material(Random azar) {
		Set<String> material = new HashSet<>();
		MATERIALES.forEach(m -> {
			if (azar.nextInt(4) > 0) {
				material.add(m);
			}
		});
		return Set.copyOf(material);
	}

	private static Set<String> exclusiones(Random azar, List<EjercicioRutina> ejercicios) {
		Set<String> excluidos = new HashSet<>();
		int cuantos = azar.nextInt(MAX_EXCLUSIONES + 1);
		for (int i = 0; i < cuantos; i++) {
			excluidos.add(elegir(azar, ejercicios).idExterno());
		}
		return Set.copyOf(excluidos);
	}

	private static List<String> prioritarios(Random azar) {
		List<String> prioritarios = new ArrayList<>();
		int cuantos = azar.nextInt(EntradaRutina.MAXIMO_PRIORITARIOS + 1);
		for (int i = 0; i < cuantos; i++) {
			String m = elegir(azar, MUSCULOS);
			if (!prioritarios.contains(m)) {
				prioritarios.add(m);
			}
		}
		return List.copyOf(prioritarios);
	}

	private static List<Restriccion> limitaciones(Random azar) {
		List<Restriccion> lista = new ArrayList<>();
		int cuantas = azar.nextInt(MAX_LIMITAR + 1);
		for (int i = 0; i < cuantas; i++) {
			boolean porRegion = azar.nextBoolean();
			lista.add(new Restriccion(porRegion ? elegir(azar, REGIONES) : null, Lado.AMBOS,
					porRegion ? null : elegir(azar, PATRONES), elegir(azar, LIMITAR), null, Estado.ACTUAL,
					Origen.USUARIO, null));
		}
		return lista;
	}
}
