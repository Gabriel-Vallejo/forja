package es.forja.restricciones.filtro;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import es.forja.restricciones.filtro.Decision.Excluido;
import es.forja.restricciones.filtro.Decision.Permitido;
import es.forja.restricciones.filtro.Decision.Sustituido;
import es.forja.restricciones.modelo.Estado;
import es.forja.restricciones.modelo.Lado;
import es.forja.restricciones.modelo.Origen;
import es.forja.restricciones.modelo.Patron;
import es.forja.restricciones.modelo.Region;
import es.forja.restricciones.modelo.Restriccion;
import es.forja.restricciones.modelo.TipoRestriccion;

/**
 * Propiedades del filtro previo (ADR 0002, enmienda de 2026-11-16).
 *
 * Cada propiedad se comprueba sobre {@value #CASOS} catálogos y listas de restricciones aleatorios con semilla
 * fija: si una falla, la semilla del caso la reproduce. Sin librería de propiedades: jqwik pide expresamente
 * que no lo usen agentes de IA, y aquí los tests los escriben agentes.
 */
class FiltroPropiedadesTest {

	private static final int CASOS = 300;
	private static final int MAX_EJERCICIOS = 25;
	private static final int MAX_RESTRICCIONES = 4;
	private static final List<Patron> PATRONES = List.of(Patron.SENTADILLA, Patron.EMPUJE_VERTICAL,
			Patron.TRACCION_VERTICAL);
	private static final List<Region> REGIONES = List.of(Region.HOMBRO, Region.RODILLA, Region.COLUMNA_LUMBAR);
	private static final List<String> FAMILIAS = List.of("f1", "f2", "f3");
	private static final List<String> IMPLEMENTOS = List.of("barra", "maquina", "mancuerna");

	private final FiltroRestricciones filtro = new FiltroRestricciones();

	static Stream<Long> semillas() {
		return LongStream.range(0, CASOS).boxed();
	}

	private record Caso(List<EjercicioCandidato> catalogo, List<Restriccion> restricciones, Patron patron) {

		Map<String, EjercicioCandidato> porId() {
			return catalogo.stream().collect(Collectors.toMap(EjercicioCandidato::idExterno, e -> e));
		}
	}

	private static Caso caso(long semilla) {
		Random azar = new Random(semilla);
		return new Caso(catalogo(azar), restricciones(azar), elegir(azar, PATRONES));
	}

	private static <T> T elegir(Random azar, List<T> opciones) {
		return opciones.get(azar.nextInt(opciones.size()));
	}

	private static <T> T elegirONulo(Random azar, List<T> opciones, double probabilidadNulo) {
		return azar.nextDouble() < probabilidadNulo ? null : elegir(azar, opciones);
	}

	private static List<EjercicioCandidato> catalogo(Random azar) {
		int tamano = azar.nextInt(MAX_EJERCICIOS + 1);
		List<EjercicioCandidato> catalogo = new ArrayList<>();
		for (int i = 0; i < tamano; i++) {
			catalogo.add(ejercicio(azar, i));
		}
		return List.copyOf(catalogo);
	}

	private static EjercicioCandidato ejercicio(Random azar, int indice) {
		Set<Region> regiones = EnumSet.noneOf(Region.class);
		REGIONES.forEach(r -> {
			if (azar.nextBoolean()) {
				regiones.add(r);
			}
		});
		return new EjercicioCandidato("e%02d".formatted(indice), "Ejercicio " + indice, elegir(azar, PATRONES),
				Set.copyOf(regiones), azar.nextBoolean(), azar.nextBoolean(),
				elegir(azar, List.of(Agarre.values())), azar.nextBoolean(), elegirONulo(azar, FAMILIAS, 0.2),
				elegir(azar, List.of(RangoMovimiento.values())), elegir(azar, IMPLEMENTOS), azar.nextInt(3) > 0);
	}

	private static List<Restriccion> restricciones(Random azar) {
		int tamano = azar.nextInt(MAX_RESTRICCIONES + 1);
		List<Restriccion> lista = new ArrayList<>();
		while (lista.size() < tamano) {
			Region region = elegirONulo(azar, REGIONES, 0.5);
			Patron patron = elegirONulo(azar, PATRONES, 0.3);
			if (region == null && patron == null) {
				continue;
			}
			lista.add(new Restriccion(region, Lado.AMBOS, patron, elegir(azar, List.of(TipoRestriccion.values())),
					null, elegir(azar, List.of(Estado.values())), elegir(azar, List.of(Origen.values())), null));
		}
		return List.copyOf(lista);
	}

	@ParameterizedTest(name = "semilla {0}")
	@MethodSource("semillas")
	void conEvitarEnUnPatronNingunPermitidoNiSustituidoAcabaEnEsePatron(long semilla) {
		Caso caso = caso(semilla);
		List<Restriccion> restricciones = new ArrayList<>(caso.restricciones());
		restricciones.add(new Restriccion(null, Lado.AMBOS, caso.patron(), TipoRestriccion.EVITAR, null,
				Estado.ACTUAL, Origen.USUARIO, null));
		Map<String, EjercicioCandidato> porId = caso.porId();

		filtro.aplicar(caso.catalogo(), restricciones).forEach((id, decision) -> {
			switch (decision) {
				case Permitido p -> assertThat(porId.get(id).patron()).isNotEqualTo(caso.patron());
				case Sustituido s -> assertThat(porId.get(s.porIdExterno()).patron()).isNotEqualTo(caso.patron());
				case Excluido e -> { }
			}
		});
	}

	@ParameterizedTest(name = "semilla {0}")
	@MethodSource("semillas")
	void todoSustituidoApuntaAUnCuradoQueCumpleTodasLasRestricciones(long semilla) {
		Caso caso = caso(semilla);
		Map<String, EjercicioCandidato> porId = caso.porId();

		filtro.aplicar(caso.catalogo(), caso.restricciones()).values().stream()
				.filter(Sustituido.class::isInstance).map(Sustituido.class::cast).forEach(s -> {
					EjercicioCandidato sustituto = porId.get(s.porIdExterno());
					assertThat(sustituto.curado()).isTrue();
					assertThat(FiltroRestricciones.cumple(sustituto, caso.restricciones())).isTrue();
				});
	}

	@ParameterizedTest(name = "semilla {0}")
	@MethodSource("semillas")
	void todaDecisionNoPermitidaLlevaMotivo(long semilla) {
		Caso caso = caso(semilla);

		filtro.aplicar(caso.catalogo(), caso.restricciones()).values().forEach(d -> {
			switch (d) {
				case Excluido e -> assertThat(e.motivo()).isNotBlank();
				case Sustituido s -> assertThat(s.motivo()).isNotBlank();
				case Permitido p -> { }
			}
		});
	}

	@ParameterizedTest(name = "semilla {0}")
	@MethodSource("semillas")
	void todoPermitidoCumpleLasRestricciones(long semilla) {
		Caso caso = caso(semilla);
		Map<String, EjercicioCandidato> porId = caso.porId();

		filtro.aplicar(caso.catalogo(), caso.restricciones()).forEach((id, d) -> {
			if (d instanceof Permitido) {
				assertThat(FiltroRestricciones.cumple(porId.get(id), caso.restricciones())).isTrue();
			}
		});
	}

	@ParameterizedTest(name = "semilla {0}")
	@MethodSource("semillas")
	void aplicarElFiltroDosVecesDaElMismoResultado(long semilla) {
		Caso caso = caso(semilla);

		assertThat(filtro.aplicar(caso.catalogo(), caso.restricciones()))
				.isEqualTo(filtro.aplicar(caso.catalogo(), caso.restricciones()));
		assertThat(filtro.aplicar(caso.catalogo(), caso.restricciones())).hasSize(caso.catalogo().size());
	}

}
