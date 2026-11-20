package es.forja.restricciones.filtro;

import static es.forja.restricciones.filtro.Candidatos.axial;
import static es.forja.restricciones.filtro.Candidatos.base;
import static es.forja.restricciones.filtro.Candidatos.conEstado;
import static es.forja.restricciones.filtro.Candidatos.conParametro;
import static es.forja.restricciones.filtro.Candidatos.conRegiones;
import static es.forja.restricciones.filtro.Candidatos.deProfesional;
import static es.forja.restricciones.filtro.Candidatos.noCurado;
import static es.forja.restricciones.filtro.Candidatos.reducido;
import static es.forja.restricciones.filtro.Candidatos.sobrePatron;
import static es.forja.restricciones.filtro.Candidatos.sobreRegion;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import es.forja.restricciones.filtro.Decision.Excluido;
import es.forja.restricciones.filtro.Decision.Permitido;
import es.forja.restricciones.filtro.Decision.Sustituido;
import es.forja.restricciones.modelo.Estado;
import es.forja.restricciones.modelo.Patron;
import es.forja.restricciones.modelo.Region;
import es.forja.restricciones.modelo.TipoRestriccion;

class FiltroRestriccionesTest {

	private final FiltroRestricciones filtro = new FiltroRestricciones();

	private static final EjercicioCandidato SENTADILLA_BARRA = axial(base("a_sentadilla_barra", Patron.SENTADILLA,
			"sentadilla", "barra"));
	private static final EjercicioCandidato PRENSA = reducido(base("b_prensa", Patron.SENTADILLA, "sentadilla",
			"maquina"));
	private static final EjercicioCandidato GOBLET = base("c_goblet", Patron.SENTADILLA, "sentadilla", "kettlebell");
	private static final EjercicioCandidato PRESS_MILITAR = conRegiones(
			base("d_press_militar", Patron.EMPUJE_VERTICAL, "press_vertical", "barra"), Region.HOMBRO);
	private static final EjercicioCandidato PRESS_BANCA = base("e_press_banca", Patron.EMPUJE_HORIZONTAL,
			"press_horizontal", "barra");

	private static final List<EjercicioCandidato> CATALOGO = List.of(SENTADILLA_BARRA, PRENSA, GOBLET,
			PRESS_MILITAR, PRESS_BANCA);

	private Map<String, Decision> aplicar(List<EjercicioCandidato> catalogo,
			es.forja.restricciones.modelo.Restriccion... restricciones) {
		return filtro.aplicar(catalogo, List.of(restricciones));
	}

	@Test
	void sinRestriccionesTodoQuedaPermitidoSinAnotaciones() {
		Map<String, Decision> r = aplicar(CATALOGO);

		assertThat(r).hasSize(CATALOGO.size());
		assertThat(r.values()).allSatisfy(d -> assertThat(d).isEqualTo(new Permitido(List.of())));
	}

	@Test
	void evitarUnPatronExcluyeTodosLosEjerciciosDeEsePatron() {
		Map<String, Decision> r = aplicar(CATALOGO, sobrePatron(Patron.SENTADILLA, TipoRestriccion.EVITAR));

		assertThat(r.get("a_sentadilla_barra")).isInstanceOf(Excluido.class);
		assertThat(r.get("b_prensa")).isInstanceOf(Excluido.class);
		assertThat(r.get("c_goblet")).isInstanceOf(Excluido.class);
		assertThat(r.get("d_press_militar")).isInstanceOf(Permitido.class);
		assertThat(((Excluido) r.get("b_prensa")).motivo()).contains("Evitar").contains("sentadilla")
				.contains("ejercicio b_prensa");
	}

	@Test
	void evitarUnaRegionExcluyeLosQueLaCargan() {
		Map<String, Decision> r = aplicar(CATALOGO, sobreRegion(Region.HOMBRO, TipoRestriccion.EVITAR));

		assertThat(r.get("d_press_militar")).isInstanceOf(Excluido.class);
		assertThat(((Excluido) r.get("d_press_militar")).motivo()).contains("hombro");
		assertThat(r.get("e_press_banca")).isInstanceOf(Permitido.class);
	}

	@Test
	void conRegionYPatronLaRestriccionExigeLasDosCosas() {
		var r = new es.forja.restricciones.modelo.Restriccion(Region.HOMBRO, es.forja.restricciones.modelo.Lado.AMBOS,
				Patron.EMPUJE_HORIZONTAL, TipoRestriccion.EVITAR, null, Estado.ACTUAL,
				es.forja.restricciones.modelo.Origen.USUARIO, null);

		Map<String, Decision> d = aplicar(CATALOGO, r);

		// El press militar carga hombro pero no es empuje horizontal; el de banca es empuje horizontal sin hombro.
		assertThat(d.get("d_press_militar")).isInstanceOf(Permitido.class);
		assertThat(d.get("e_press_banca")).isInstanceOf(Permitido.class);
	}

	@Test
	void elHistoricoSinLimitacionNoCambiaNada() {
		Map<String, Decision> r = aplicar(CATALOGO,
				conEstado(sobrePatron(Patron.SENTADILLA, TipoRestriccion.EVITAR), Estado.HISTORICO_SIN_LIMITACION));

		assertThat(r.values()).allSatisfy(d -> assertThat(d).isInstanceOf(Permitido.class));
	}

	@Test
	void sinCargaAxialSustituyeLaSentadillaConBarraPorLaPrensa() {
		Map<String, Decision> r = aplicar(CATALOGO, sobrePatron(Patron.SENTADILLA, TipoRestriccion.SIN_CARGA_AXIAL));

		assertThat(r.get("a_sentadilla_barra")).isInstanceOfSatisfying(Sustituido.class, s -> {
			assertThat(s.porIdExterno()).isEqualTo("b_prensa");
			assertThat(s.motivo()).isEqualTo(
					"Sin carga axial en sentadilla: ejercicio b_prensa en vez de ejercicio a_sentadilla_barra");
		});
		assertThat(r.get("b_prensa")).isInstanceOf(Permitido.class);
		assertThat(r.get("c_goblet")).isInstanceOf(Permitido.class);
	}

	@Test
	void limitarRangoSustituyePorLaVarianteReducidaDeLaMismaFamilia() {
		var barra = base("a_barra", Patron.SENTADILLA, "sentadilla", "barra");
		var cajon = reducido(base("z_cajon", Patron.SENTADILLA, "sentadilla", "barra"));

		Map<String, Decision> r = aplicar(List.of(barra, cajon),
				sobrePatron(Patron.SENTADILLA, TipoRestriccion.LIMITAR_RANGO));

		assertThat(r.get("a_barra")).isInstanceOfSatisfying(Sustituido.class,
				s -> assertThat(s.porIdExterno()).isEqualTo("z_cajon"));
		assertThat(r.get("z_cajon")).isInstanceOf(Permitido.class);
	}

	@Test
	void limitarRangoSinVarianteReducidaPasaAlSiguienteEscalonYSiNoHayQuedaExcluido() {
		var barra = base("a_barra", Patron.SENTADILLA, "sentadilla", "barra");
		var goblet = base("b_goblet", Patron.SENTADILLA, "sentadilla", "kettlebell");

		Map<String, Decision> r = aplicar(List.of(barra, goblet),
				sobrePatron(Patron.SENTADILLA, TipoRestriccion.LIMITAR_RANGO));

		// Ninguna cumple el rango reducido: no se inventa un sustituto que incumpla la restricción.
		assertThat(r.get("a_barra")).isInstanceOfSatisfying(Excluido.class,
				e -> assertThat(e.motivo()).contains("sin variante compatible").contains("Limitar rango"));
		assertThat(r.get("b_goblet")).isInstanceOf(Excluido.class);
	}

	@Test
	void escalonDosMismaFamiliaYOtroImplemento() {
		var barra = axial(base("a_barra", Patron.SENTADILLA, "sentadilla", "barra"));
		var gobletAxial = axial(base("b_goblet_axial", Patron.SENTADILLA, "sentadilla", "kettlebell"));
		var hack = base("c_hack", Patron.SENTADILLA, "sentadilla", "maquina");

		Map<String, Decision> r = aplicar(List.of(barra, gobletAxial, hack),
				sobrePatron(Patron.SENTADILLA, TipoRestriccion.SIN_CARGA_AXIAL));

		// El hack no reduce el rango, pero es la misma familia con otro implemento.
		assertThat(r.get("a_barra")).isInstanceOfSatisfying(Sustituido.class,
				s -> assertThat(s.porIdExterno()).isEqualTo("c_hack"));
	}

	@Test
	void escalonTresOtroCuradoDelMismoPatronAunqueSeaDeOtraFamilia() {
		var barra = axial(base("a_barra", Patron.SENTADILLA, "sentadilla", "barra"));
		var zancadaLibre = base("b_otra", Patron.SENTADILLA, "otra_familia", "mancuerna");

		Map<String, Decision> r = aplicar(List.of(barra, zancadaLibre),
				sobrePatron(Patron.SENTADILLA, TipoRestriccion.SIN_CARGA_AXIAL));

		assertThat(r.get("a_barra")).isInstanceOfSatisfying(Sustituido.class,
				s -> assertThat(s.porIdExterno()).isEqualTo("b_otra"));
	}

	@Test
	void unSustitutoNoCuradoNuncaEntra() {
		var barra = axial(base("a_barra", Patron.SENTADILLA, "sentadilla", "barra"));
		var derivado = noCurado(base("b_derivado", Patron.SENTADILLA, "sentadilla", "maquina"));

		Map<String, Decision> r = aplicar(List.of(barra, derivado),
				sobrePatron(Patron.SENTADILLA, TipoRestriccion.SIN_CARGA_AXIAL));

		assertThat(r.get("a_barra")).isInstanceOf(Excluido.class);
		assertThat(r.get("b_derivado")).isInstanceOf(Permitido.class);
	}

	@Test
	void unEjercicioNoCuradoQueIncumpleSeSustituyePorUnCurado() {
		var derivadoAxial = noCurado(axial(base("a_derivado", Patron.SENTADILLA, null, "barra")));
		var curado = base("b_curado", Patron.SENTADILLA, "sentadilla", "maquina");

		Map<String, Decision> r = aplicar(List.of(derivadoAxial, curado),
				sobrePatron(Patron.SENTADILLA, TipoRestriccion.SIN_CARGA_AXIAL));

		assertThat(r.get("a_derivado")).isInstanceOfSatisfying(Sustituido.class,
				s -> assertThat(s.porIdExterno()).isEqualTo("b_curado"));
	}

	@Test
	void elSustitutoCumpleTodasLasRestriccionesNoSoloLaQueMotivaElCambio() {
		var barra = axial(base("a_barra", Patron.SENTADILLA, "sentadilla", "barra"));
		var sinAxialPeroHombro = conRegiones(base("b_prensa", Patron.SENTADILLA, "sentadilla", "maquina"),
				Region.HOMBRO);
		var limpio = base("c_goblet", Patron.SENTADILLA, "sentadilla", "kettlebell");

		Map<String, Decision> r = aplicar(List.of(barra, sinAxialPeroHombro, limpio),
				sobrePatron(Patron.SENTADILLA, TipoRestriccion.SIN_CARGA_AXIAL),
				sobreRegion(Region.HOMBRO, TipoRestriccion.EVITAR));

		assertThat(r.get("b_prensa")).isInstanceOf(Excluido.class);
		assertThat(r.get("a_barra")).isInstanceOfSatisfying(Sustituido.class,
				s -> assertThat(s.porIdExterno()).isEqualTo("c_goblet"));
	}

	@Test
	void conVariosCandidatosIgualesGanaElDeMenorIdExterno() {
		var barra = axial(base("a_barra", Patron.SENTADILLA, "sentadilla", "barra"));
		var maquinaB = base("m_b", Patron.SENTADILLA, "sentadilla", "maquina");
		var maquinaA = base("m_a", Patron.SENTADILLA, "sentadilla", "maquina");

		Map<String, Decision> r = aplicar(List.of(maquinaB, barra, maquinaA),
				sobrePatron(Patron.SENTADILLA, TipoRestriccion.SIN_CARGA_AXIAL));

		assertThat(r.get("a_barra")).isInstanceOfSatisfying(Sustituido.class,
				s -> assertThat(s.porIdExterno()).isEqualTo("m_a"));
		assertThat(r.keySet()).containsExactly("a_barra", "m_a", "m_b");
	}

	@Test
	void sinImpactoSinAgarrePesadoYSinPorEncimaDeLaCabezaSustituyenElQueLosViola() {
		var salto = new EjercicioCandidato("a_salto", "Salto", Patron.SENTADILLA, Set.of(), false, true,
				Agarre.NINGUNO, false, "salto", RangoMovimiento.COMPLETO, "peso_corporal", true);
		var saltoSuave = base("b_suave", Patron.SENTADILLA, "salto", "peso_corporal");
		var colgado = new EjercicioCandidato("c_colgado", "Colgado", Patron.TRACCION_VERTICAL, Set.of(), false,
				false, Agarre.PESADO, false, "dominada", RangoMovimiento.COMPLETO, "peso_corporal", true);
		var colgadoLigero = base("d_ligero", Patron.TRACCION_VERTICAL, "dominada", "banda");
		var encima = new EjercicioCandidato("e_encima", "Encima", Patron.EMPUJE_VERTICAL, Set.of(), false, false,
				Agarre.NINGUNO, true, "press_vertical", RangoMovimiento.COMPLETO, "barra", true);
		var inclinado = base("f_inclinado", Patron.EMPUJE_VERTICAL, "press_vertical", "mancuerna");
		List<EjercicioCandidato> catalogo = List.of(salto, saltoSuave, colgado, colgadoLigero, encima, inclinado);

		Map<String, Decision> r = aplicar(catalogo,
				sobrePatron(Patron.SENTADILLA, TipoRestriccion.SIN_IMPACTO),
				sobrePatron(Patron.TRACCION_VERTICAL, TipoRestriccion.SIN_AGARRE_PESADO),
				sobrePatron(Patron.EMPUJE_VERTICAL, TipoRestriccion.SIN_POR_ENCIMA_CABEZA));

		assertThat(((Sustituido) r.get("a_salto")).porIdExterno()).isEqualTo("b_suave");
		assertThat(((Sustituido) r.get("c_colgado")).porIdExterno()).isEqualTo("d_ligero");
		assertThat(((Sustituido) r.get("e_encima")).porIdExterno()).isEqualTo("f_inclinado");
	}

	@Test
	void limitarCargaYTempoControladoNoExcluyenSoloAnotan() {
		Map<String, Decision> r = aplicar(CATALOGO,
				conParametro(sobrePatron(Patron.SENTADILLA, TipoRestriccion.LIMITAR_CARGA), "rpe_6"),
				sobreRegion(Region.HOMBRO, TipoRestriccion.TEMPO_CONTROLADO));

		assertThat(r.get("a_sentadilla_barra")).isInstanceOfSatisfying(Permitido.class,
				p -> assertThat(p.anotaciones()).containsExactly("limitar carga en sentadilla: rpe_6"));
		assertThat(r.get("d_press_militar")).isInstanceOfSatisfying(Permitido.class,
				p -> assertThat(p.anotaciones()).containsExactly("tempo controlado en hombro"));
		assertThat(r.get("e_press_banca")).isEqualTo(new Permitido(List.of()));
	}

	@Test
	void unaRestriccionDeProfesionalExcluyeEnVezDeSustituir() {
		Map<String, Decision> r = aplicar(CATALOGO,
				deProfesional(sobrePatron(Patron.SENTADILLA, TipoRestriccion.SIN_CARGA_AXIAL)));

		assertThat(r.get("a_sentadilla_barra")).isInstanceOfSatisfying(Excluido.class,
				e -> assertThat(e.motivo()).contains("Sin carga axial").contains("profesional"));
		assertThat(r.get("b_prensa")).isInstanceOf(Permitido.class);
	}

	@Test
	void limitarRangoDeProfesionalExcluyeLosDeRangoCompleto() {
		Map<String, Decision> r = aplicar(CATALOGO,
				deProfesional(sobrePatron(Patron.SENTADILLA, TipoRestriccion.LIMITAR_RANGO)));

		assertThat(r.get("a_sentadilla_barra")).isInstanceOf(Excluido.class);
		assertThat(r.get("c_goblet")).isInstanceOf(Excluido.class);
		assertThat(r.get("b_prensa")).isInstanceOf(Permitido.class);
	}

	@Test
	void unaCargaLimitadaPorProfesionalSigueSiendoAnotacionParaElSolver() {
		Map<String, Decision> r = aplicar(CATALOGO,
				deProfesional(sobrePatron(Patron.SENTADILLA, TipoRestriccion.LIMITAR_CARGA)));

		assertThat(r.get("a_sentadilla_barra")).isInstanceOfSatisfying(Permitido.class,
				p -> assertThat(p.anotaciones()).singleElement().asString().contains("profesional"));
	}

	@Test
	void elSustitutoLlevaLasAnotacionesQueLeAplican() {
		Map<String, Decision> r = aplicar(CATALOGO,
				sobrePatron(Patron.SENTADILLA, TipoRestriccion.SIN_CARGA_AXIAL),
				sobrePatron(Patron.SENTADILLA, TipoRestriccion.TEMPO_CONTROLADO));

		assertThat(r.get("a_sentadilla_barra")).isInstanceOfSatisfying(Sustituido.class,
				s -> assertThat(s.anotaciones()).containsExactly("tempo controlado en sentadilla"));
	}

	@Test
	void unCatalogoVacioDaUnResultadoVacio() {
		assertThat(filtro.aplicar(List.of(), List.of())).isEmpty();
	}

	@Test
	void rechazaIdsExternosRepetidosEnElCatalogo() {
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> filtro.aplicar(List.of(PRENSA, PRENSA), List.of()))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("b_prensa");
	}

}
