package es.forja.sesiones.servicio;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;

import org.springframework.stereotype.Service;

import es.forja.comun.api.ErrorNegocio;
import es.forja.cuenta.repositorio.UsuarioRepositorio;
import es.forja.entreno.perfil.servicio.ServicioPerfilEntreno;
import es.forja.sesiones.modelo.ResumenProgreso;
import es.forja.sesiones.modelo.ResumenProgreso.SesionHecha;
import es.forja.sesiones.repositorio.SesionRepositorio;
import es.forja.sesiones.repositorio.SesionRepositorio.SerieConCarga;
import es.forja.sesiones.modelo.MarcaEjercicio;
import es.forja.entreno.plan.modelo.SerieTrabajo;
import es.forja.entreno.plan.modelo.GrupoMuscular;
import es.forja.entreno.plan.modelo.E1rm;

/** Racha y cifras del perfil (Hoy, Perfil y la pantalla de racha), en los días de la zona horaria del móvil. */
@Service
public class ServicioProgreso {

	/** Como en la progresión: por encima, Epley no es fiable. */
	private static final int REPETICIONES_MAXIMAS_E1RM = 12;

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

	/** Los ejercicios con tabla de OpenPowerlifting (RF-30): la sentadilla, la banca y el peso muerto con barra. */
	private static final Map<String, String> LEVANTAMIENTOS = Map.of("Barbell_Squat", "sentadilla",
			"Barbell_Full_Squat", "sentadilla", "Barbell_Bench_Press_-_Medium_Grip", "banca", "Barbell_Deadlift",
			"peso_muerto");

	/** La mejor serie de cada ejercicio hecho, por e1RM; sin sesiones, la lista vacía (y la app, «sin rango»). */
	public List<MarcaEjercicio> marcas(UUID usuario) {
		long usuarioId = usuarios.porUuid(usuario)
				.orElseThrow(() -> ErrorNegocio.noAutenticado("La sesión ya no es válida.")).id();
		Map<String, MarcaEjercicio> mejores = new LinkedHashMap<>();
		for (SerieConCarga s : sesiones.seriesConCarga(usuarioId)) {
			E1rm.deSerie(new SerieTrabajo(s.pesoKg(), s.repeticiones(), null), REPETICIONES_MAXIMAS_E1RM)
					.ifPresent(e1rm -> mejores.merge(s.idExterno(),
							new MarcaEjercicio(s.idExterno(), s.nombre(), List.of(), s.pesoKg(), s.repeticiones(), e1rm,
									LEVANTAMIENTOS.get(s.idExterno())),
							(a, b) -> b.e1rmKg().compareTo(a.e1rmKg()) > 0 ? b : a));
		}
		Map<String, List<String>> musculos = sesiones.musculosPrimarios(mejores.keySet());
		return mejores.values().stream().map(m -> new MarcaEjercicio(m.idExterno(), m.nombre(),
				musculos.getOrDefault(m.idExterno(), List.of()).stream().map(GrupoMuscular::delCatalogo)
						.map(GrupoMuscular::codigo).distinct().toList(),
				m.pesoKg(), m.repeticiones(), m.e1rmKg(), m.levantamiento())).toList();
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
