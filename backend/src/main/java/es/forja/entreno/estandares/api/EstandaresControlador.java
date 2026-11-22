package es.forja.entreno.estandares.api;

import java.time.Duration;
import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.forja.entreno.estandares.repositorio.EstandaresRepositorio;
import es.forja.entreno.estandares.repositorio.EstandaresRepositorio.TablaEstandar;

/**
 * Percentiles de sentadilla, banca y peso muerto por sexo y franja de peso (RF-30), de OpenPowerlifting. Son datos
 * públicos: la app los guarda y calcula el percentil en el móvil, así que el peso y las marcas no viajan para esto.
 */
@RestController
@RequestMapping("/api/v1/estandares")
public class EstandaresControlador {

	private static final Duration CACHE = Duration.ofDays(7);

	private final EstandaresRepositorio estandares;

	public EstandaresControlador(EstandaresRepositorio estandares) {
		this.estandares = estandares;
	}

	@GetMapping
	public ResponseEntity<List<TablaEstandar>> todas() {
		return ResponseEntity.ok().cacheControl(CacheControl.maxAge(CACHE).cachePrivate()).body(estandares.todas());
	}
}
