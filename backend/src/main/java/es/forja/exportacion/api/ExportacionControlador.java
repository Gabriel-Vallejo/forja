package es.forja.exportacion.api;

import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.forja.comun.seguridad.UsuarioAutenticado;
import es.forja.exportacion.modelo.DatosExportados;
import es.forja.exportacion.servicio.ServicioExportacion;

/** «Descargar mis datos»: el JSON con todo lo del usuario, como adjunto con la fecha (UTC) en el nombre. */
@RestController
@RequestMapping("/api/v1")
public class ExportacionControlador {

	private final ServicioExportacion exportacion;

	public ExportacionControlador(ServicioExportacion exportacion) {
		this.exportacion = exportacion;
	}

	@GetMapping("/cuenta/datos")
	public ResponseEntity<DatosExportados> exportar(@AuthenticationPrincipal UsuarioAutenticado actual) {
		DatosExportados datos = exportacion.exportar(actual.usuario());
		String archivo = "forja-mis-datos-" + LocalDate.ofInstant(datos.generadoEn(), ZoneOffset.UTC) + ".json";
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(archivo).build()
						.toString())
				.body(datos);
	}
}
