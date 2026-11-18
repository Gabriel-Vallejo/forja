package es.forja.entreno.perfil.servicio;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.forja.comun.api.ErrorNegocio;
import es.forja.cuenta.repositorio.UsuarioRepositorio;
import es.forja.entreno.perfil.modelo.DatosPerfil;
import es.forja.entreno.perfil.repositorio.PerfilDatosRepositorio;

/** Sexo, experiencia y objetivo del usuario. Son preferencias: no piden consentimiento. */
@Service
public class ServicioPerfilDatos {

	private final PerfilDatosRepositorio repositorio;
	private final UsuarioRepositorio usuarios;
	private final Clock reloj;

	public ServicioPerfilDatos(PerfilDatosRepositorio repositorio, UsuarioRepositorio usuarios, Clock reloj) {
		this.repositorio = repositorio;
		this.usuarios = usuarios;
		this.reloj = reloj;
	}

	public DatosPerfil obtener(UUID usuario) {
		return repositorio.buscar(idDe(usuario)).orElse(DatosPerfil.POR_DEFECTO);
	}

	@Transactional
	public DatosPerfil guardar(UUID usuario, DatosPerfil datos) {
		long usuarioId = idDe(usuario);
		repositorio.guardar(usuarioId, datos, reloj.instant());
		return repositorio.buscar(usuarioId).orElseThrow();
	}

	private long idDe(UUID usuario) {
		return usuarios.porUuid(usuario).orElseThrow(() -> ErrorNegocio.noAutenticado("La sesión ya no es válida."))
				.id();
	}
}
