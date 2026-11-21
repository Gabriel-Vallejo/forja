package es.forja.sesiones.servicio;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import es.forja.comun.api.ErrorNegocio;
import es.forja.cuenta.repositorio.UsuarioRepositorio;
import es.forja.entreno.perfil.servicio.ServicioPerfilEntreno;
import es.forja.sesiones.modelo.ResumenProgreso;
import es.forja.sesiones.modelo.ResumenProgreso.SesionHecha;
import es.forja.sesiones.repositorio.SesionRepositorio;

/** Racha y cifras del perfil (Hoy, Perfil y la pantalla de racha), en los días de la zona horaria del móvil. */
@Service
public class ServicioProgreso {

	private final SesionRepositorio sesiones;
	private final ServicioPerfilEntreno perfiles;
	private final UsuarioRepositorio usuarios;
	private final Clock reloj;

	public ServicioProgreso(SesionRepositorio sesiones, ServicioPerfilEntreno perfiles, UsuarioRepositorio usuarios,
			Clock reloj) {
		this.sesiones = sesiones;
		this.perfiles = perfiles;
		this.usuarios = usuarios;
		this.reloj = reloj;
	}

	public ResumenProgreso resumen(UUID usuario, String zona) {
		ZoneId zonaId = zona(zona);
		long usuarioId = usuarios.porUuid(usuario)
				.orElseThrow(() -> ErrorNegocio.noAutenticado("La sesión ya no es válida.")).id();
		List<SesionHecha> hechas = sesiones.sesionesHechas(usuarioId).stream()
				.map(s -> new SesionHecha(LocalDate.ofInstant(s.realizadaEn(), zonaId), s.kg())).toList();
		return ResumenProgreso.de(hechas, perfiles.obtener(usuario).diasPorSemana(), LocalDate.now(reloj.withZone(zonaId)));
	}

	private static ZoneId zona(String zona) {
		if (zona == null || zona.isBlank()) {
			return ZoneId.of("UTC");
		}
		try {
			return ZoneId.of(zona);
		} catch (RuntimeException e) {
			throw ErrorNegocio.datosNoValidos("Esa zona horaria no existe.");
		}
	}
}
