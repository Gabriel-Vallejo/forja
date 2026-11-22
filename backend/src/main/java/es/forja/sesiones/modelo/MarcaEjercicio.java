package es.forja.sesiones.modelo;

import java.math.BigDecimal;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * La mejor serie de un ejercicio (por e1RM de Epley), para el mapa de rangos. Solo series de trabajo con carga y como
 * mucho 12 repeticiones: con más, Epley no es fiable.
 *
 * @param grupos grupos del cuerpo de sus músculos primarios (los del componente {@code Cuerpo} de la app)
 * @param levantamiento {@code sentadilla}, {@code banca} o {@code peso_muerto} si tiene tabla de OpenPowerlifting
 */
public record MarcaEjercicio(String idExterno, String nombre, List<String> grupos, BigDecimal pesoKg, int repeticiones,
		BigDecimal e1rmKg, @Nullable String levantamiento) {
}
