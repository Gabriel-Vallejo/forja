package es.forja.cuenta.servicio;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import es.forja.cuenta.modelo.Dispositivo;
import es.forja.cuenta.modelo.Usuario;
import es.forja.cuenta.repositorio.DispositivoRepositorio;
import es.forja.cuenta.repositorio.TokenRefrescoRepositorio;
import es.forja.cuenta.repositorio.UsuarioRepositorio;

/** Los dispositivos de una cuenta: verlos y cerrar la sesión de cualquiera (ADR 0009 §4). */
@Service
public class ServicioDispositivos {

	/** Dispositivo tal como lo ve su dueño; {@code actual} marca el que hace la petición. */
	public record Vista(Dispositivo dispositivo, boolean actual) {
	}

	private final UsuarioRepositorio usuarios;
	private final DispositivoRepositorio dispositivos;
	private final TokenRefrescoRepositorio refrescos;
	private final TransactionTemplate transaccion;
	private final Clock reloj;

	public ServicioDispositivos(UsuarioRepositorio usuarios, DispositivoRepositorio dispositivos,
			TokenRefrescoRepositorio refrescos, TransactionTemplate transaccion, Clock reloj) {
		this.usuarios = usuarios;
		this.dispositivos = dispositivos;
		this.refrescos = refrescos;
		this.transaccion = transaccion;
		this.reloj = reloj;
	}

	private Usuario usuario(UUID uuid) {
		return usuarios.porUuid(uuid).orElseThrow(() -> ErrorCuenta.noAutenticado("La sesión ya no es válida."));
	}

	public List<Vista> listar(UUID usuarioUuid, UUID dispositivoActual) {
		return dispositivos.vivosDeUsuario(usuario(usuarioUuid).id()).stream()
				.map(d -> new Vista(d, d.uuid().equals(dispositivoActual))).toList();
	}

	/** Todos los dispositivos de la cuenta, también los revocados: es lo que se exporta. */
	public List<Dispositivo> todos(UUID usuarioUuid) {
		return dispositivos.deUsuario(usuario(usuarioUuid).id());
	}

	/** Revoca uno de los dispositivos del usuario; si no existe o es de otra cuenta, 404. */
	public void revocar(UUID usuarioUuid, UUID dispositivoUuid) {
		Dispositivo dispositivo = dispositivos.vivoDeUsuario(dispositivoUuid, usuario(usuarioUuid).id())
				.orElseThrow(() -> ErrorCuenta.noEncontrado("No existe ese dispositivo."));
		Instant ahora = reloj.instant();
		transaccion.executeWithoutResult(estado -> {
			refrescos.revocarDeDispositivo(dispositivo.id(), ahora);
			dispositivos.revocar(dispositivo.id(), ahora);
		});
	}
}
