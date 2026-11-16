package es.forja.cuenta.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import es.forja.BuzonDeCorreo;
import es.forja.PruebaConMysql;
import es.forja.RelojDePrueba;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Utilidades comunes de los tests de la API de cuenta: peticiones JSON, IP distinta por test y cuentas listas. */
@PruebaConMysql
public abstract class ApiCuentaSoporte {

	protected static final String CONTRASENA = "una-contrasena-larga-1";
	protected static final String FECHA_ADULTO = "1998-04-12";
	private static final AtomicInteger CONTADOR_IP = new AtomicInteger();

	/** Sesión tal como la ve un cliente: lo que devolvió el login. */
	public record Cliente(String correo, String tokenAcceso, String tokenRefresco, String dispositivo) {
		public String cabecera() {
			return "Bearer " + tokenAcceso;
		}
	}

	@Autowired
	protected MockMvc mvc;
	@Autowired
	protected JsonMapper json;
	@Autowired
	protected JdbcClient jdbc;
	@Autowired
	protected BuzonDeCorreo buzon;
	@Autowired
	protected RelojDePrueba reloj;

	protected String ip;

	@BeforeEach
	protected void prepararIpYReloj() {
		int n = CONTADOR_IP.incrementAndGet();
		ip = "10." + (n / 65_536 % 256) + "." + (n / 256 % 256) + "." + (n % 256);
		reloj.reiniciar();
		buzon.vaciar();
	}

	protected static String correoNuevo() {
		return "u" + UUID.randomUUID().toString().replace("-", "") + "@forja.test";
	}

	protected ResultActions enviar(String ruta, Object cuerpo) throws Exception {
		return enviarDesde(ip, ruta, cuerpo);
	}

	protected ResultActions enviarDesde(String origen, String ruta, Object cuerpo) throws Exception {
		return mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo))
				.with(peticion -> {
					peticion.setRemoteAddr(origen);
					return peticion;
				}));
	}

	protected ResultActions enviarAutenticado(String ruta, Cliente cliente, Object cuerpo) throws Exception {
		return mvc.perform(post(ruta).header("Authorization", cliente.cabecera())
				.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo)));
	}

	protected ResultActions consultar(String ruta, Cliente cliente) throws Exception {
		return mvc.perform(get(ruta).header("Authorization", cliente.cabecera()));
	}

	protected ResultActions borrarRecurso(String ruta, Cliente cliente) throws Exception {
		return mvc.perform(delete(ruta).header("Authorization", cliente.cabecera()));
	}

	protected static Map<String, Object> cuerpoRegistro(String correo, String contrasena, String fecha) {
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("correo", correo);
		cuerpo.put("contrasena", contrasena);
		cuerpo.put("fechaNacimiento", fecha);
		return cuerpo;
	}

	protected static Map<String, Object> cuerpoLogin(String correo, String contrasena, String nombre, Object dispositivoId) {
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("correo", correo);
		cuerpo.put("contrasena", contrasena);
		cuerpo.put("nombreDispositivo", nombre);
		cuerpo.put("plataforma", "movil");
		if (dispositivoId != null) {
			cuerpo.put("dispositivoId", dispositivoId);
		}
		return cuerpo;
	}

	protected JsonNode cuerpoDe(ResultActions resultado) throws Exception {
		return json.readTree(resultado.andReturn().getResponse().getContentAsString());
	}

	/** Da de alta una cuenta sin verificar (con el limitador de IP propio del test) y devuelve su correo. */
	protected String crearCuentaSinVerificar() throws Exception {
		String correo = correoNuevo();
		enviar("/api/v1/auth/registro", cuerpoRegistro(correo, CONTRASENA, FECHA_ADULTO)).andExpect(status().isAccepted());
		return correo;
	}

	/** Gasta el token de verificación que salió por correo. */
	protected void verificarCorreo(String correo) throws Exception {
		String token = buzon.ultimoToken(correo, "verificacion").orElseThrow();
		enviar("/api/v1/auth/verificar", Map.of("token", token)).andExpect(status().isNoContent());
	}

	/** Alta y verificación: la cuenta queda lista para entrar. */
	protected String crearCuenta() throws Exception {
		String correo = crearCuentaSinVerificar();
		verificarCorreo(correo);
		return correo;
	}

	protected Cliente iniciarSesion(String correo, String nombre, String dispositivoId) throws Exception {
		ResultActions login = enviar("/api/v1/auth/login", cuerpoLogin(correo, CONTRASENA, nombre, dispositivoId))
				.andExpect(status().isOk());
		JsonNode cuerpo = cuerpoDe(login);
		return new Cliente(correo, cuerpo.get("tokenAcceso").asString(), cuerpo.get("tokenRefresco").asString(),
				cuerpo.get("dispositivoId").asString());
	}

	protected Cliente cuentaConSesion(String nombreDispositivo) throws Exception {
		return iniciarSesion(crearCuenta(), nombreDispositivo, null);
	}

	protected long contar(String sql, Object... parametros) {
		return jdbc.sql(sql).params(parametros).query(Long.class).single();
	}
}
