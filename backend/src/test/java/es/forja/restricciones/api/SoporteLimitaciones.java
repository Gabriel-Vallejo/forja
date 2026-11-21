package es.forja.restricciones.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import es.forja.cuenta.api.ApiCuentaSoporte;

/** Atajos comunes de los tests de consentimiento y limitaciones, sobre el soporte de la API de cuenta. */
public abstract class SoporteLimitaciones extends ApiCuentaSoporte {

	protected static final String VERSION_VIGENTE = "v1";
	protected static final String RUTA_CONSENTIMIENTOS = "/api/v1/consentimientos";
	protected static final String RUTA_RESTRICCIONES = "/api/v1/restricciones";

	protected ResultActions darConsentimiento(Cliente cliente, String tipo) throws Exception {
		return enviarAutenticado(RUTA_CONSENTIMIENTOS, cliente, Map.of("tipo", tipo, "versionTexto", versionDe(tipo)));
	}

	/** La versión vigente del texto de ese tipo (la de un tipo inventado, la general). */
	protected static String versionDe(String tipo) {
		try {
			return es.forja.consentimiento.modelo.TipoConsentimiento.desdeCodigo(tipo).versionVigente();
		} catch (IllegalArgumentException e) {
			return VERSION_VIGENTE;
		}
	}

	/** Cuenta verificada, con sesión y con el consentimiento de limitaciones dado. */
	protected Cliente clienteConLimitaciones() throws Exception {
		Cliente cliente = cuentaConSesion("Pixel");
		darConsentimiento(cliente, "limitaciones").andExpect(status().isOk());
		return cliente;
	}

	protected ResultActions actualizar(String ruta, Cliente cliente, Object cuerpo) throws Exception {
		return mvc.perform(put(ruta).header("Authorization", cliente.cabecera())
				.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo)));
	}

	/** Restricción sintética: lado ambos, estado actual, origen usuario y sin texto original. */
	protected static Map<String, Object> restriccion(String region, String patron, String tipo) {
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("region", region);
		cuerpo.put("lado", "ambos");
		cuerpo.put("patron", patron);
		cuerpo.put("tipo", tipo);
		cuerpo.put("parametro", null);
		cuerpo.put("estado", "actual");
		cuerpo.put("origen", "usuario");
		cuerpo.put("textoOriginal", null);
		return cuerpo;
	}

	protected String crearRestriccion(Cliente cliente, Map<String, Object> cuerpo) throws Exception {
		ResultActions respuesta = enviarAutenticado(RUTA_RESTRICCIONES, cliente, cuerpo)
				.andExpect(status().isCreated());
		return cuerpoDe(respuesta).get("uuid").asString();
	}
}
