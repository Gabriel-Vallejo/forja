package es.forja.comun.cifrado;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PropiedadesCifrado.class)
public class ConfiguracionCifrado {

	private static final String SEPARADOR_PAR = ",";
	private static final String SEPARADOR_KID = ":";
	private static final int PARTES_DE_UN_PAR = 2;

	@Bean
	Cifrador cifrador(PropiedadesCifrado propiedades) {
		try {
			return new Cifrador(propiedades.kidActual(), decodificar(propiedades.claveActual()),
					anteriores(propiedades.clavesAnteriores()));
		} catch (IllegalArgumentException e) {
			// Mensaje propio: nunca se repite aquí el contenido de la configuración.
			throw new IllegalStateException("La configuración del cifrado (forja.cifrado.*) no es válida.");
		}
	}

	private static Map<String, byte[]> anteriores(String texto) {
		Map<String, byte[]> claves = new LinkedHashMap<>();
		if (texto == null || texto.isBlank()) {
			return claves;
		}
		for (String par : texto.split(SEPARADOR_PAR)) {
			String[] partes = par.strip().split(SEPARADOR_KID, PARTES_DE_UN_PAR);
			if (partes.length != PARTES_DE_UN_PAR) {
				throw new IllegalArgumentException("Par kid:clave mal formado");
			}
			claves.put(partes[0], decodificar(partes[1]));
		}
		return claves;
	}

	private static byte[] decodificar(String base64) {
		return Base64.getDecoder().decode(base64.strip());
	}
}
