package es.forja.cuenta.servicio;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Lista local, una contraseña por línea, en la ruta de {@code FORJA_CONTRASENAS_FILTRADAS}. Sin la variable
 * la lista queda vacía y se avisa al arrancar; no se descarga nada.
 *
 * <p>shortcut: carga todo el archivo en memoria; vale para listas de unos millones, con una de cientos de
 * millones habría que pasar a un filtro de Bloom o a una búsqueda binaria sobre el archivo ordenado.
 */
@Component
public class ArchivoContrasenasFiltradas implements ListaContrasenasFiltradas {

	private static final Logger LOG = LoggerFactory.getLogger(ArchivoContrasenasFiltradas.class);

	private final Set<String> contrasenas;

	public ArchivoContrasenasFiltradas(@Value("${forja.contrasenas-filtradas:}") String ruta) {
		if (ruta == null || ruta.isBlank()) {
			LOG.warn("FORJA_CONTRASENAS_FILTRADAS sin configurar: no se comprueban contraseñas filtradas");
			this.contrasenas = Set.of();
			return;
		}
		this.contrasenas = cargar(Path.of(ruta.strip()));
		LOG.info("Lista de contraseñas filtradas cargada: {} entradas", contrasenas.size());
	}

	private static Set<String> cargar(Path archivo) {
		try (Stream<String> lineas = Files.lines(archivo, StandardCharsets.UTF_8)) {
			Set<String> cargadas = new HashSet<>();
			lineas.filter(l -> !l.isBlank()).forEach(l -> cargadas.add(normalizar(l)));
			return Set.copyOf(cargadas);
		} catch (IOException e) {
			throw new UncheckedIOException("No se puede leer la lista de contraseñas filtradas", e);
		}
	}

	private static String normalizar(String contrasena) {
		return contrasena.strip().toLowerCase(Locale.ROOT);
	}

	@Override
	public boolean contiene(String contrasena) {
		return contrasenas.contains(normalizar(contrasena));
	}
}
