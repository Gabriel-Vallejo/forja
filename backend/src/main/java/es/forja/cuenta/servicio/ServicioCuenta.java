package es.forja.cuenta.servicio;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import es.forja.comun.seguridad.DispositivosActivos;
import es.forja.cuenta.modelo.Usuario;
import es.forja.cuenta.repositorio.DispositivoRepositorio;
import es.forja.cuenta.repositorio.UsuarioRepositorio;

/** Borrado de la cuenta (ADR 0009 §9) y la consulta que usa el filtro JWT. */
@Service
public class ServicioCuenta implements DispositivosActivos {

	private static final String ACCION_BORRAR = "borrar";

	private final UsuarioRepositorio usuarios;
	private final DispositivoRepositorio dispositivos;
	private final PasswordEncoder codificador;
	private final LimitadorIntentos limitador;

	public ServicioCuenta(UsuarioRepositorio usuarios, DispositivoRepositorio dispositivos,
			PasswordEncoder codificador, LimitadorIntentos limitador) {
		this.usuarios = usuarios;
		this.dispositivos = dispositivos;
		this.codificador = codificador;
		this.limitador = limitador;
	}

	@Override
	public boolean estaActivo(UUID dispositivo, UUID usuario) {
		return dispositivos.estaVivo(dispositivo, usuario);
	}

	/** Pide la contraseña otra vez y borra la cuenta; las claves foráneas en cascada se llevan el resto. */
	public void borrar(UUID usuarioUuid, String contrasena) {
		limitador.exigirPorClave(ACCION_BORRAR, usuarioUuid.toString());
		Usuario usuario = usuarios.porUuid(usuarioUuid)
				.orElseThrow(() -> ErrorCuenta.noAutenticado("La sesión ya no es válida."));
		if (!codificador.matches(contrasena, usuario.hashContrasena())) {
			throw ErrorCuenta.prohibido("La contraseña no es correcta.");
		}
		usuarios.borrar(usuario.id());
	}
}
