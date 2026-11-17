package es.forja.entreno.perfil.servicio;

import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.forja.comun.api.ErrorNegocio;
import es.forja.cuenta.repositorio.UsuarioRepositorio;
import es.forja.entreno.perfil.modelo.AjustesEntreno;
import es.forja.entreno.perfil.modelo.PerfilEntreno;
import es.forja.entreno.perfil.repositorio.PerfilEntrenoRepositorio;
import es.forja.entreno.perfil.repositorio.PerfilEntrenoRepositorio.Opcion;
import es.forja.entreno.rutina.repositorio.CatalogoRutinaRepositorio;

/** Ajustes del plan del usuario (días, minutos y material) y el material que la app ofrece. */
@Service
public class ServicioPerfilEntreno {

	private final PerfilEntrenoRepositorio perfiles;
	private final CatalogoRutinaRepositorio catalogo;
	private final UsuarioRepositorio usuarios;
	private final Clock reloj;

	public ServicioPerfilEntreno(PerfilEntrenoRepositorio perfiles, CatalogoRutinaRepositorio catalogo,
			UsuarioRepositorio usuarios, Clock reloj) {
		this.perfiles = perfiles;
		this.catalogo = catalogo;
		this.usuarios = usuarios;
		this.reloj = reloj;
	}

	public PerfilEntreno obtener(UUID usuario) {
		return perfiles.buscar(idDe(usuario)).map(a -> aPerfil(a, false))
				.orElseGet(() -> aPerfil(AjustesEntreno.POR_DEFECTO, true));
	}

	@Transactional
	public PerfilEntreno guardar(UUID usuario, AjustesEntreno ajustes) {
		Set<String> conocidos = catalogo.codigosMaterial();
		if (!conocidos.containsAll(ajustes.material())) {
			throw ErrorNegocio.datosNoValidos("Hay material que no existe.");
		}
		perfiles.guardar(idDe(usuario), ajustes, reloj.instant());
		return obtener(usuario);
	}

	public List<Opcion> materiales() {
		return perfiles.materialesOfrecidos();
	}

	private static PerfilEntreno aPerfil(AjustesEntreno a, boolean porDefecto) {
		return new PerfilEntreno(a.diasPorSemana(), a.minutosPorSesion(), a.material(), porDefecto);
	}

	private long idDe(UUID usuario) {
		return usuarios.porUuid(usuario).orElseThrow(() -> ErrorNegocio.noAutenticado("La sesión ya no es válida."))
				.id();
	}
}
