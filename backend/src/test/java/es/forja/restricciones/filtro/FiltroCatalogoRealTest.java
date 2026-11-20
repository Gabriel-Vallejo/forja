package es.forja.restricciones.filtro;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import es.forja.PruebaConMysql;
import es.forja.entreno.catalogo.modelo.Ejercicio;
import es.forja.entreno.catalogo.repositorio.EjercicioRepositorio;
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

/** El filtro contra el catálogo real de 876 ejercicios sembrado por Flyway, en un MySQL de Testcontainers. */
@PruebaConMysql
@Transactional
class FiltroCatalogoRealTest {

	private final FiltroRestricciones filtro = new FiltroRestricciones();

	@Autowired
	private EjercicioRepositorio ejercicios;

	private List<EjercicioCandidato> catalogo() {
		return ejercicios.findAll().stream().map(AdaptadorCatalogo::desde).toList();
	}

	private static Restriccion restriccion(Patron patron, TipoRestriccion tipo) {
		return new Restriccion(null, Lado.AMBOS, patron, tipo, null, Estado.ACTUAL, Origen.USUARIO, null);
	}

	@Test
	void elAdaptadorCargaLasRegionesDelEjercicio() {
		Ejercicio sentadilla = ejercicios.findAll().stream()
				.filter(e -> "Barbell_Squat".equals(e.getIdExterno())).findFirst().orElseThrow();

		EjercicioCandidato c = AdaptadorCatalogo.desde(sentadilla);

		assertThat(c.nombre()).isEqualTo("Sentadilla con barra");
		assertThat(c.patron()).isEqualTo(Patron.SENTADILLA);
		assertThat(c.cargaAxial()).isTrue();
		assertThat(c.curado()).isTrue();
		assertThat(c.regiones()).contains(Region.RODILLA);
		assertThat(c.implemento()).isEqualTo("barra");
		assertThat(c.rango()).isEqualTo(RangoMovimiento.COMPLETO);
	}

	@Test
	void sinCargaAxialEnSentadillaSustituyeLaSentadillaConBarraPorUnaVarianteSinCargaAxial() {
		Map<String, Decision> r = filtro.aplicar(catalogo(),
				List.of(restriccion(Patron.SENTADILLA, TipoRestriccion.SIN_CARGA_AXIAL)));

		assertThat(r.get("Barbell_Squat")).isInstanceOfSatisfying(Sustituido.class, s -> {
			assertThat(s.porIdExterno()).isEqualTo("Leg_Press");
			assertThat(s.motivo()).isEqualTo(
					"Sin carga axial en sentadilla: prensa de piernas en vez de sentadilla con barra");
		});
		assertThat(r.get("Leg_Press")).isInstanceOf(Permitido.class);
	}

	@Test
	void evitarEmpujeVerticalNoDejaNingunPressPorEncimaDeLaCabeza() {
		List<EjercicioCandidato> catalogo = catalogo();

		Map<String, Decision> r = filtro.aplicar(catalogo,
				List.of(restriccion(Patron.EMPUJE_VERTICAL, TipoRestriccion.EVITAR)));

		assertThat(catalogo.stream().filter(e -> e.patron() == Patron.EMPUJE_VERTICAL)).isNotEmpty()
				.allSatisfy(e -> assertThat(r.get(e.idExterno())).isInstanceOf(Excluido.class));
		assertThat(catalogo.stream().filter(e -> e.porEncimaCabeza() && e.curado())
				.filter(e -> !(r.get(e.idExterno()) instanceof Excluido)))
				.as("press por encima de la cabeza que sigue permitido o sustituido")
				.allSatisfy(e -> assertThat(e.patron()).isNotEqualTo(Patron.EMPUJE_VERTICAL));
		assertThat(r.values().stream().filter(Sustituido.class::isInstance).map(Sustituido.class::cast))
				.allSatisfy(s -> assertThat(catalogo.stream()
						.filter(e -> e.idExterno().equals(s.porIdExterno())).findFirst().orElseThrow().patron())
						.isNotEqualTo(Patron.EMPUJE_VERTICAL));
	}

}
