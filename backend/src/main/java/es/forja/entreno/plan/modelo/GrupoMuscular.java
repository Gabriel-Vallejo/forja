package es.forja.entreno.plan.modelo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

/** Los 14 grupos del componente {@code Cuerpo} de la app; cada músculo del catálogo cae en uno. */
public enum GrupoMuscular {
	PECHO("pecho"),
	HOMBROS("hombros"),
	BRAZOS("brazos"),
	TRICEPS("triceps"),
	ANTEBRAZOS("antebrazos"),
	ABDOMEN("abdomen"),
	CUADRICEPS("cuadriceps"),
	ADUCTORES("aductores"),
	GEMELOS("gemelos"),
	TRAPECIO("trapecio"),
	DORSAL("dorsal"),
	LUMBAR("lumbar"),
	GLUTEOS("gluteos"),
	ISQUIOS("isquios");

	/** Músculo del catálogo ({@code musculo.codigo}) → grupo del cuerpo. */
	private static final Map<String, GrupoMuscular> DEL_CATALOGO = Map.ofEntries(
			Map.entry("pecho", PECHO),
			Map.entry("hombros", HOMBROS),
			Map.entry("biceps", BRAZOS),
			Map.entry("triceps", TRICEPS),
			Map.entry("antebrazos", ANTEBRAZOS),
			Map.entry("abdominales", ABDOMEN),
			Map.entry("cuadriceps", CUADRICEPS),
			Map.entry("aductores", ADUCTORES),
			Map.entry("gemelos", GEMELOS),
			Map.entry("trapecio", TRAPECIO),
			Map.entry("cuello", TRAPECIO),
			Map.entry("dorsales", DORSAL),
			Map.entry("espalda_media", DORSAL),
			Map.entry("lumbares", LUMBAR),
			Map.entry("gluteos", GLUTEOS),
			Map.entry("abductores", GLUTEOS),
			Map.entry("isquiotibiales", ISQUIOS));

	private final String codigo;

	GrupoMuscular(String codigo) {
		this.codigo = codigo;
	}

	@JsonValue
	public String codigo() {
		return codigo;
	}

	/** Falla si el catálogo trae un músculo sin grupo: hay que añadirlo a la tabla de arriba. */
	public static GrupoMuscular delCatalogo(String musculo) {
		GrupoMuscular grupo = DEL_CATALOGO.get(musculo);
		if (grupo == null) {
			throw new IllegalStateException("El músculo del catálogo «" + musculo + "» no tiene grupo en el cuerpo.");
		}
		return grupo;
	}

	/**
	 * Los grupos de un ejercicio sin repetir (varios músculos pueden caer en el mismo): si un grupo sale como primario y
	 * como secundario, se queda como primario. Primarios antes que secundarios; dentro de cada rol, por orden de
	 * aparición.
	 */
	public static List<MusculoEjercicio> agrupar(List<MusculoCatalogo> musculos) {
		Map<GrupoMuscular, RolMuscular> roles = new LinkedHashMap<>();
		for (RolMuscular rol : RolMuscular.values()) {
			musculos.stream().filter(m -> m.rol() == rol).map(m -> delCatalogo(m.codigo()))
					.forEach(g -> roles.putIfAbsent(g, rol));
		}
		return roles.entrySet().stream().map(e -> new MusculoEjercicio(e.getKey(), e.getValue())).toList();
	}

	/** Un músculo tal cual viene de {@code ejercicio_musculo}. */
	public record MusculoCatalogo(String codigo, RolMuscular rol) {
	}

	/** Un grupo del cuerpo que trabaja el ejercicio, y cómo. */
	public record MusculoEjercicio(GrupoMuscular grupo, RolMuscular rol) {
	}
}
