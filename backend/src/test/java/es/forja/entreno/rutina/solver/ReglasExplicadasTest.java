package es.forja.entreno.rutina.solver;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import es.forja.entreno.movilidad.modelo.TestMovilidad;
import es.forja.entreno.movilidad.modelo.Tramo;
import es.forja.entreno.rutina.modelo.ConfiguracionRutina;
import es.forja.entreno.rutina.modelo.EjercicioRutina;
import es.forja.entreno.rutina.modelo.PlanRutina.VolumenMusculo;
import es.forja.entreno.rutina.solver.ReglasExplicadas.Contexto;
import es.forja.entreno.rutina.solver.ReglasExplicadas.Razon;
import es.forja.entreno.rutina.solver.ReglasExplicadas.Regla;
import es.forja.restricciones.filtro.Agarre;
import es.forja.restricciones.filtro.EjercicioCandidato;
import es.forja.restricciones.filtro.RangoMovimiento;
import es.forja.restricciones.modelo.Patron;

class ReglasExplicadasTest {

	/** Vocabulario sanitario que la finalidad de fitness prohíbe (ADR 0007 y CLAUDE.md). */
	private static final Pattern PROHIBIDAS = Pattern.compile(
			"(?i)(rehabilit|alivi|corrig|previen|lesi[oó]n|desequilibr|atrofi|seguro|terap|cura)");
	/** Una cita «Apellido Año» dentro de la fuente. */
	private static final Pattern CITA = Pattern.compile("([A-ZÁÉÍÓÚ][a-záéíóúñ]+) (\\d{4})");
	/** Los tests corren desde {@code backend/}. */
	private static final Path REFERENCIAS = Path.of("..", "docs", "referencias.md");

	private static final ConfiguracionRutina CONFIG = ConfiguracionDePrueba.estandar();

	private static EjercicioRutina ejercicio(String id, Patron patron, boolean compuesto, boolean estirada,
			Map<String, Integer> musculos) {
		EjercicioCandidato c = new EjercicioCandidato(id, "Ejercicio " + id, patron, EnumSet.noneOf(
				es.forja.restricciones.modelo.Region.class), false, false, Agarre.NINGUNO, false, null,
				RangoMovimiento.COMPLETO, "maquina", true);
		return new EjercicioRutina(c, true, compuesto, estirada, "Máquina", musculos);
	}

	private static Map<String, Long> terminos(long estirada, long compuesto, long volumen, long movilidad,
			long prioritarios) {
		Map<String, Long> t = new LinkedHashMap<>();
		t.put(Terminos.COBERTURA_PRIORITARIOS, prioritarios);
		t.put(Terminos.POSICION_ESTIRADA, estirada);
		t.put(Terminos.FATIGA, 0L);
		t.put(Terminos.COMPUESTO, compuesto);
		t.put(Terminos.MOVILIDAD, movilidad);
		t.put(Terminos.PALANCAS, 0L);
		t.put(Terminos.VARIEDAD, 0L);
		t.put(Terminos.VOLUMEN, volumen);
		return t;
	}

	private static final List<VolumenMusculo> VOLUMEN = List.of(
			new VolumenMusculo("isquiotibiales", "Isquiotibiales", 12, 10, 14, 20, Textos.FUENTE_VOLUMEN),
			new VolumenMusculo("cuadriceps", "Cuádriceps", 14, 10, 14, 20, Textos.FUENTE_VOLUMEN));

	private static Contexto contexto(EjercicioRutina e, Map<String, Long> terminos, int series, int dias,
			int seriesPatronDia, List<String> sustituye, Map<TestMovilidad, Tramo> tramos) {
		return new Contexto(e, terminos, VOLUMEN, series, dias, seriesPatronDia, sustituye, tramos);
	}

	private static List<String> codigos(List<Razon> razones) {
		return razones.stream().map(Razon::codigo).toList();
	}

	@Test
	void cadaReglaTieneCodigoPlantillaFuenteYNivel() {
		assertThat(ReglasExplicadas.REGLAS).isNotEmpty().allSatisfy(r -> {
			assertThat(r.codigo()).matches("[a-z_]+");
			assertThat(r.plantilla()).isNotBlank();
			assertThat(r.fuente()).isNotBlank();
			assertThat(r.nivel()).isNotNull();
		});
		assertThat(ReglasExplicadas.REGLAS.stream().map(Regla::codigo)).doesNotHaveDuplicates().contains(
				ReglasExplicadas.POSICION_ESTIRADA, ReglasExplicadas.COMPUESTO, ReglasExplicadas.VOLUMEN,
				ReglasExplicadas.FRECUENCIA, ReglasExplicadas.MATERIAL, ReglasExplicadas.MOVILIDAD,
				ReglasExplicadas.LIMITACION, ReglasExplicadas.TOPE_PATRON);
	}

	@Test
	void lasReglasDeSaludSonHeuristicaOPropuesta() {
		assertThat(ReglasExplicadas.regla(ReglasExplicadas.MOVILIDAD).nivel()).isEqualTo(ReglasExplicadas.Nivel.H);
		assertThat(ReglasExplicadas.regla(ReglasExplicadas.MOVILIDAD).fuente()).contains("no ciencia");
		assertThat(ReglasExplicadas.regla(ReglasExplicadas.LIMITACION).nivel()).isEqualTo(ReglasExplicadas.Nivel.P);
		assertThat(ReglasExplicadas.regla(ReglasExplicadas.TOPE_PATRON).nivel()).isEqualTo(ReglasExplicadas.Nivel.H);
		assertThat(ReglasExplicadas.regla(ReglasExplicadas.FRECUENCIA).nivel()).isEqualTo(ReglasExplicadas.Nivel.H);
	}

	@Test
	void todaCitaDeUnaFuenteEstaEnReferencias() throws IOException {
		String referencias = Files.readString(REFERENCIAS, StandardCharsets.UTF_8);
		List<String> fuentes = new java.util.ArrayList<>(ReglasExplicadas.REGLAS.stream().map(Regla::fuente).toList());
		ReglasExplicadas.FUENTES_ESTIRADA.values().forEach(f -> fuentes.add(f.fuente()));
		for (String fuente : fuentes) {
			Matcher m = CITA.matcher(fuente);
			while (m.find()) {
				assertThat(referencias).as(fuente).containsPattern("\\*\\*" + m.group(1) + " [^*]*\\(" + m.group(2));
			}
		}
	}

	@Test
	void ningunTextoUsaVocabularioSanitario() {
		ReglasExplicadas.REGLAS.forEach(r -> assertThat(PROHIBIDAS.matcher(r.plantilla() + r.fuente()).find())
				.as(r.codigo()).isFalse());
		ReglasExplicadas.FUENTES_ESTIRADA.values().forEach(
				f -> assertThat(PROHIBIDAS.matcher(f.detalle() + f.fuente()).find()).as(f.fuente()).isFalse());
		// Y las razones ya rellenas, con todas las reglas a la vez y los tres tests de movilidad.
		EjercicioRutina goblet = ejercicio("Goblet_Squat", Patron.SENTADILLA, true, false,
				Map.of("cuadriceps", 2, "gluteos", 1));
		Map<TestMovilidad, Tramo> tramos = Map.of(TestMovilidad.TOBILLO, Tramo.LIMITADO, TestMovilidad.CADERA,
				Tramo.LIMITADO, TestMovilidad.HOMBRO, Tramo.LIMITADO);
		List<Razon> razones = ReglasExplicadas.todas(CONFIG,
				contexto(goblet, terminos(6, 16, 120, 40, 10), 4, 2, 6, List.of("Sentadilla con barra"), tramos));
		assertThat(razones).hasSizeGreaterThan(ReglasExplicadas.MAXIMO).allSatisfy(
				r -> assertThat(PROHIBIDAS.matcher(r.texto()).find()).as(r.texto()).isFalse());
		ReglasMovilidad.idsCitados().forEach(id -> ReglasMovilidad.favorecen(id, tramos)
				.forEach(f -> assertThat(PROHIBIDAS.matcher(f.porQue()).find()).isFalse()));
	}

	@Test
	void laPosicionEstiradaCitaSuFuenteYNivelPorEjercicio() {
		EjercicioRutina curl = ejercicio("Seated_Leg_Curl", Patron.AISLAMIENTO_PIERNA, false, true,
				Map.of("isquiotibiales", 2));

		Razon razon = ReglasExplicadas.porQue(CONFIG, contexto(curl, terminos(6, 0, 0, 0, 0), 3, 1, 3, List.of(),
				Map.of())).get(0);

		assertThat(razon.codigo()).isEqualTo(ReglasExplicadas.POSICION_ESTIRADA);
		assertThat(razon.fuente()).isEqualTo("Maeo 2021");
		assertThat(razon.nivel()).isEqualTo("B");
		assertThat(razon.texto()).contains("estirado");
	}

	@Test
	void laEstiradaSinFuentePropiaUsaLaRevisionYEsIndirecta() {
		EjercicioRutina otro = ejercicio("Otro", Patron.AISLAMIENTO_PIERNA, false, true, Map.of("cuadriceps", 2));

		Razon razon = ReglasExplicadas.porQue(CONFIG, contexto(otro, terminos(6, 0, 0, 0, 0), 3, 1, 3, List.of(),
				Map.of())).get(0);

		assertThat(razon.fuente()).isEqualTo("Pedrosa 2026");
		assertThat(razon.nivel()).isEqualTo("C");
	}

	@Test
	void ordenaPorLoQueMasPesoEnElObjetivoYCortaEnCuatro() {
		EjercicioRutina rumano = ejercicio("Romanian_Deadlift", Patron.BISAGRA, true, true,
				Map.of("isquiotibiales", 2, "gluteos", 2));

		List<Razon> razones = ReglasExplicadas.porQue(CONFIG,
				contexto(rumano, terminos(8, 16, 150, 0, 0), 4, 2, 6, List.of(), Map.of()));

		assertThat(codigos(razones)).containsExactly(ReglasExplicadas.VOLUMEN, ReglasExplicadas.COMPUESTO,
				ReglasExplicadas.POSICION_ESTIRADA, ReglasExplicadas.FRECUENCIA);
		assertThat(razones.get(0).texto()).contains("isquiotibiales").contains("12").contains("14");
	}

	@Test
	void sinTerminosQuedanElMaterialYLoDelDia() {
		EjercicioRutina press = ejercicio("Press", Patron.EMPUJE_HORIZONTAL, true, false, Map.of("pecho", 2));

		List<Razon> razones = ReglasExplicadas.porQue(CONFIG,
				contexto(press, Map.of(), 3, 1, 6, List.of(), Map.of()));

		assertThat(codigos(razones)).containsExactly(ReglasExplicadas.TOPE_PATRON, ReglasExplicadas.MATERIAL);
		assertThat(razones.get(0).texto()).contains("empuje horizontal").contains("6");
		assertThat(razones.get(1).texto()).contains("máquina");
	}

	@Test
	void elAislamientoNoTieneTopeDeSeriesPorDia() {
		EjercicioRutina curl = ejercicio("Curl", Patron.AISLAMIENTO_BRAZO, false, false, Map.of("biceps", 2));

		assertThat(codigos(ReglasExplicadas.porQue(CONFIG, contexto(curl, Map.of(), 3, 1, 9, List.of(),
				Map.of())))).doesNotContain(ReglasExplicadas.TOPE_PATRON);
	}

	@Test
	void laLimitacionVaPrimeroYNoEsTerapeutica() {
		EjercicioRutina goblet = ejercicio("Goblet_Squat", Patron.SENTADILLA, true, false, Map.of("cuadriceps", 2));

		Razon razon = ReglasExplicadas.porQue(CONFIG, contexto(goblet, terminos(0, 16, 150, 0, 0), 4, 1, 4,
				List.of("Sentadilla con barra"), Map.of())).get(0);

		assertThat(razon.codigo()).isEqualTo(ReglasExplicadas.LIMITACION);
		assertThat(razon.texto()).contains("sentadilla con barra").contains("compatible con la limitación que indicaste");
		assertThat(razon.nivel()).isEqualTo("P");
	}

	@Test
	void sustituirAVariosEjerciciosEsUnaSolaRazonYDejaSitioALasDemas() {
		EjercicioRutina prensa = ejercicio("Prensa", Patron.SENTADILLA, true, false, Map.of("cuadriceps", 2));

		List<Razon> razones = ReglasExplicadas.porQue(CONFIG, contexto(prensa, terminos(0, 16, 150, 0, 0), 4, 1, 4,
				List.of("Sentadilla con barra", "Sentadilla frontal con barra", "Sentadilla goblet", "Sentadilla hack"),
				Map.of()));

		assertThat(codigos(razones).stream().filter(ReglasExplicadas.LIMITACION::equals)).hasSize(1);
		assertThat(razones.get(0).texto()).contains("sentadilla con barra").contains("3 más");
		assertThat(razones).hasSizeGreaterThan(1);
	}

	@Test
	void laMovilidadSoloSaleSiElTramoFavoreceElEjercicio() {
		EjercicioRutina goblet = ejercicio("Goblet_Squat", Patron.SENTADILLA, true, false, Map.of("cuadriceps", 2));
		Map<String, Long> t = terminos(0, 16, 150, 40, 0);

		List<Razon> conTramo = ReglasExplicadas.porQue(CONFIG,
				contexto(goblet, t, 4, 1, 4, List.of(), Map.of(TestMovilidad.TOBILLO, Tramo.LIMITADO)));
		List<Razon> bueno = ReglasExplicadas.porQue(CONFIG,
				contexto(goblet, t, 4, 1, 4, List.of(), Map.of(TestMovilidad.TOBILLO, Tramo.BUENO)));

		Razon movilidad = conTramo.stream().filter(r -> r.codigo().equals(ReglasExplicadas.MOVILIDAD)).findFirst()
				.orElseThrow();
		assertThat(movilidad.texto()).contains("tobillo").contains("limitado");
		assertThat(movilidad.nivel()).isEqualTo("H");
		assertThat(codigos(bueno)).doesNotContain(ReglasExplicadas.MOVILIDAD);
	}

	@Test
	void unMusculoSinMetaNoDaRazonDeVolumen() {
		EjercicioRutina core = ejercicio("Plancha", Patron.CORE, false, false, Map.of("abdominales", 2));

		assertThat(codigos(ReglasExplicadas.porQue(CONFIG, contexto(core, terminos(0, 0, 30, 0, 0), 3, 1, 3,
				List.of(), Map.of())))).doesNotContain(ReglasExplicadas.VOLUMEN);
	}

	@Test
	void elPrioritarioSeExplica() {
		EjercicioRutina press = ejercicio("Press", Patron.EMPUJE_HORIZONTAL, true, false, Map.of("pecho", 2));

		assertThat(codigos(ReglasExplicadas.porQue(CONFIG, contexto(press, terminos(0, 0, 0, 0, 30), 3, 1, 3,
				List.of(), Map.of())))).first().isEqualTo(ReglasExplicadas.PRIORITARIO);
	}
}
