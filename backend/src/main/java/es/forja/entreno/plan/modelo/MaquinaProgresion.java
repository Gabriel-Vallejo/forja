package es.forja.entreno.plan.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import es.forja.entreno.plan.modelo.Transicion.Evidencia;

/**
 * Progresión de un ejercicio (RF-26): función pura y determinista sobre sus sesiones vigentes de trabajo. No
 * depende del orden en que se le pasan las sesiones ni guarda estado: recorre el historial de la más antigua a la
 * más reciente (desempatando por uuid) y aplica {@link Transicion} en cada sesión.
 *
 * <p>Decisiones: la ventana de estancamiento se mide hasta la última sesión (no hasta «ahora»), así que leer más
 * tarde no cambia el resultado. Una sesión cuenta como deload si viene tras el aviso y su carga no pasa de la del
 * deload; si se entrena más pesado, el aviso sigue en pie. Al terminar un deload la ventana empieza de nuevo.
 */
public final class MaquinaProgresion {

	private static final int DIAS_POR_SEMANA = 7;
	private static final BigDecimal CIEN = BigDecimal.valueOf(100);

	private MaquinaProgresion() {
	}

	public static ResultadoProgresion evaluar(List<SesionEjercicio> entrada, ReglaEjercicio regla,
			ParametrosProgresion parametros) {
		List<SesionEjercicio> sesiones = entrada.stream()
				.sorted(Comparator.comparing(SesionEjercicio::realizadaEn).thenComparing(SesionEjercicio::sesion))
				.toList();
		List<PuntoE1rm> historial = sesiones.stream().map(s -> new PuntoE1rm(s.sesion(), s.realizadaEn(),
				E1rm.deSesion(s.series(), parametros.e1rmRepeticionesMaximas()).orElse(null))).toList();
		Recorrido recorrido = new Recorrido(regla, parametros);
		for (int i = 0; i < sesiones.size(); i++) {
			recorrido.aplicar(sesiones.subList(0, i + 1));
		}
		return recorrido.resultado(historial);
	}

	/** El estado que se va arrastrando de sesión en sesión; vive solo dentro de una llamada a {@code evaluar}. */
	private static final class Recorrido {

		private final ReglaEjercicio regla;
		private final ParametrosProgresion parametros;
		private Estado estado = Estado.ACUMULANDO;
		private boolean haHabidoSesiones;
		private BigDecimal cargaObjetivo;
		private BigDecimal cargaAntesDelDeload;
		private Instant reinicio;
		private int desplazamiento;
		private boolean enElTope;
		private boolean trasDeload;

		Recorrido(ReglaEjercicio regla, ParametrosProgresion parametros) {
			this.regla = regla;
			this.parametros = parametros;
			this.cargaObjetivo = regla.cargaInicial();
		}

		private int repeticionesMax() {
			return regla.repeticionesMax() + desplazamiento;
		}

		void aplicar(List<SesionEjercicio> hasta) {
			SesionEjercicio actual = hasta.getLast();
			haHabidoSesiones = true;
			enElTope = false;
			boolean cumplida = ReglaProgresion.cumplida(actual.series(), regla.series(), repeticionesMax(),
					regla.rirObjetivo());
			boolean deload = estado == Estado.DELOAD
					&& actual.cargaMaxima().compareTo(cargaDeload(cargaAntesDelDeload)) <= 0;
			boolean estancado = !deload && hayEstancamiento(hasta);
			Estado nuevo = Transicion.resolver(
					Transicion.siguiente(estado, new Evidencia(cumplida, estancado, deload)));
			trasDeload = deload;
			if (deload) {
				reinicio = actual.realizadaEn();
				cargaObjetivo = cargaAntesDelDeload;
				cargaAntesDelDeload = null;
			} else {
				cargaAntesDelDeload = nuevo == Estado.DELOAD ? actual.cargaMaxima() : null;
				cargaObjetivo = actual.cargaMaxima();
				if (nuevo == Estado.SUBIR_CARGA) {
					subir();
				}
			}
			estado = nuevo;
		}

		private void subir() {
			if (!regla.pesoCorporal()) {
				cargaObjetivo = cargaObjetivo.add(regla.incrementoKg());
			} else if (repeticionesMax() < parametros.pesoCorporalTopeRepeticiones()) {
				desplazamiento++;
			} else {
				enElTope = true;
			}
		}

		private BigDecimal cargaDeload(BigDecimal carga) {
			BigDecimal paso = parametros.deload().redondeoKg();
			return carga.multiply(parametros.deload().factorCarga()).divide(paso, 0, RoundingMode.HALF_UP)
					.multiply(paso);
		}

		/** Al menos N sesiones en la ventana y su mejor e1RM no supera en más de X % al mejor anterior. */
		private boolean hayEstancamiento(List<SesionEjercicio> hasta) {
			ParametrosProgresion.Deload d = parametros.deload();
			Instant inicio = hasta.getLast().realizadaEn().minus(Duration.ofDays((long) DIAS_POR_SEMANA * d.semanas()));
			if (reinicio != null && reinicio.isAfter(inicio)) {
				inicio = reinicio;
			}
			Instant corte = inicio;
			List<SesionEjercicio> ventana = hasta.stream().filter(s -> s.realizadaEn().isAfter(corte)).toList();
			if (ventana.size() < d.sesionesMinimas()) {
				return false;
			}
			Optional<BigDecimal> mejor = mejorE1rm(ventana);
			Optional<BigDecimal> anterior = mejorE1rm(hasta.stream().filter(s -> !s.realizadaEn().isAfter(corte)).toList());
			if (mejor.isEmpty() || anterior.isEmpty() || anterior.get().signum() <= 0) {
				return false;
			}
			BigDecimal limite = anterior.get().multiply(CIEN.add(d.mejoraMinimaPorcentaje())).divide(CIEN);
			return mejor.get().compareTo(limite) <= 0;
		}

		private Optional<BigDecimal> mejorE1rm(List<SesionEjercicio> sesiones) {
			return sesiones.stream().map(s -> E1rm.deSesion(s.series(), parametros.e1rmRepeticionesMaximas()))
					.flatMap(Optional::stream).max(BigDecimal::compareTo);
		}

		ResultadoProgresion resultado(List<PuntoE1rm> historial) {
			if (!haHabidoSesiones) {
				return new ResultadoProgresion(Estado.ACUMULANDO, cargaObjetivo, regla.series(),
						regla.repeticionesMin(), regla.repeticionesMax(), Motivos.sinSesiones(regla), historial);
			}
			int minimo = regla.repeticionesMin() + desplazamiento;
			if (estado == Estado.DELOAD) {
				ParametrosProgresion.Deload d = parametros.deload();
				int series = BigDecimal.valueOf(regla.series()).multiply(d.factorSeries())
						.setScale(0, RoundingMode.CEILING).intValue();
				return new ResultadoProgresion(estado, cargaDeload(cargaAntesDelDeload), series, minimo,
						repeticionesMax(), Motivos.deload(d), historial);
			}
			String motivo = Motivos.de(estado, regla, cargaObjetivo, minimo, repeticionesMax(), enElTope, trasDeload);
			return new ResultadoProgresion(estado, cargaObjetivo, regla.series(), minimo, repeticionesMax(), motivo,
					historial);
		}
	}
}
