package es.forja.comun.api;

import java.lang.reflect.RecordComponent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.media.Schema;

/**
 * Lo que springdoc no deduce solo. Las respuestas se serializan con todos sus campos (los vacíos como
 * {@code null}), así que en los esquemas que solo salen en respuestas cada campo es obligatorio y los marcados con
 * {@link Nullable} admiten además {@code null}. Las peticiones se quedan como están: ahí manda Bean Validation.
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionOpenApi {

	private static final String PAQUETE = "es.forja";
	private static final String PREFIJO_REF = "#/components/schemas/";
	private static final String NULO = "null";

	@Bean
	OpenAPI especificacionForja() {
		return new OpenAPI().info(new Info().title("Forja API").version("v1")
				.description("API de Forja: cuenta, limitaciones, plan, sesiones y privacidad."));
	}

	@Bean
	@SuppressWarnings("rawtypes")
	OpenApiCustomizer camposDeRespuestaObligatorios() {
		return api -> {
			Map<String, Schema> esquemas = api.getComponents() == null ? null : api.getComponents().getSchemas();
			if (esquemas == null) {
				return;
			}
			Set<String> peticiones = alcanzables(referencias(api, true), esquemas);
			Map<String, Class<?>> registros = registros();
			for (String nombre : alcanzables(referencias(api, false), esquemas)) {
				Class<?> registro = registros.get(nombre);
				if (!peticiones.contains(nombre) && registro != null) {
					completar(esquemas.get(nombre), registro);
				}
			}
		};
	}

	/** Marca obligatorio cada campo del record y, si es {@link Nullable}, le añade el tipo {@code null}. */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void completar(Schema esquema, Class<?> registro) {
		Map<String, Schema> propiedades = esquema.getProperties();
		if (propiedades == null) {
			return;
		}
		List<String> obligatorios = new ArrayList<>();
		for (RecordComponent campo : componentes(registro)) {
			Schema propiedad = propiedades.get(campo.getName());
			if (propiedad == null) {
				continue;
			}
			obligatorios.add(campo.getName());
			if (campo.getAnnotatedType().isAnnotationPresent(Nullable.class)) {
				propiedades.put(campo.getName(), anulable(propiedad));
			}
		}
		esquema.setRequired(obligatorios.stream().sorted().toList());
	}

	/** Los campos tal como los serializa Jackson: los de un {@link JsonUnwrapped} suben al padre. */
	private static List<RecordComponent> componentes(Class<?> registro) {
		List<RecordComponent> campos = new ArrayList<>();
		for (RecordComponent campo : registro.getRecordComponents()) {
			if (campo.isAnnotationPresent(JsonUnwrapped.class) && campo.getType().isRecord()) {
				campos.addAll(componentes(campo.getType()));
			} else {
				campos.add(campo);
			}
		}
		return campos;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Schema anulable(Schema propiedad) {
		if (propiedad.get$ref() != null) {
			return new JsonSchema().anyOf(List.of(new JsonSchema().$ref(propiedad.get$ref()),
					new JsonSchema().types(Set.of(NULO))));
		}
		propiedad.addType(NULO);
		return propiedad;
	}

	/** Los esquemas que nombran las respuestas ({@code peticion = false}) o los cuerpos y parámetros. */
	private static Set<String> referencias(OpenAPI api, boolean peticion) {
		Set<String> nombres = new HashSet<>();
		if (api.getPaths() == null) {
			return nombres;
		}
		api.getPaths().values().forEach(ruta -> ruta.readOperations().forEach(op -> {
			if (peticion) {
				if (op.getRequestBody() != null && op.getRequestBody().getContent() != null) {
					op.getRequestBody().getContent().values().forEach(m -> recoger(m.getSchema(), nombres));
				}
				if (op.getParameters() != null) {
					op.getParameters().forEach(p -> recoger(p.getSchema(), nombres));
				}
			} else if (op.getResponses() != null) {
				op.getResponses().values().stream().filter(r -> r.getContent() != null)
						.forEach(r -> r.getContent().values().forEach(m -> recoger(m.getSchema(), nombres)));
			}
		}));
		return nombres;
	}

	/** Cierre transitivo: un esquema usado dentro de otro también cuenta. */
	@SuppressWarnings("rawtypes")
	private static Set<String> alcanzables(Set<String> inicio, Map<String, Schema> esquemas) {
		Set<String> vistos = new HashSet<>();
		Deque<String> pendientes = new ArrayDeque<>(inicio);
		while (!pendientes.isEmpty()) {
			String nombre = pendientes.pop();
			Schema esquema = esquemas.get(nombre);
			if (vistos.add(nombre) && esquema != null) {
				Set<String> hijos = new HashSet<>();
				recoger(esquema, hijos);
				pendientes.addAll(hijos);
			}
		}
		return vistos;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void recoger(@Nullable Schema esquema, Set<String> nombres) {
		if (esquema == null) {
			return;
		}
		if (esquema.get$ref() != null) {
			nombres.add(esquema.get$ref().replace(PREFIJO_REF, ""));
		}
		recoger(esquema.getItems(), nombres);
		if (esquema.getAdditionalProperties() instanceof Schema adicional) {
			recoger(adicional, nombres);
		}
		if (esquema.getProperties() != null) {
			esquema.getProperties().values().forEach(p -> recoger((Schema) p, nombres));
		}
		for (List<Schema> grupo : List.of(sinNulo(esquema.getAllOf()), sinNulo(esquema.getAnyOf()),
				sinNulo(esquema.getOneOf()))) {
			grupo.forEach(s -> recoger(s, nombres));
		}
	}

	@SuppressWarnings("rawtypes")
	private static List<Schema> sinNulo(@Nullable List<Schema> lista) {
		return lista == null ? List.of() : lista;
	}

	/**
	 * Los records de la aplicación por nombre simple, que es como springdoc nombra los esquemas. Si dos records se
	 * llaman igual, el nombre queda sin clase y su esquema no se toca: no sabríamos cuál de los dos es.
	 */
	private static Map<String, Class<?>> registros() {
		ClassPathScanningCandidateComponentProvider escaner = new ClassPathScanningCandidateComponentProvider(false) {
			@Override
			protected boolean isCandidateComponent(AnnotatedBeanDefinition definicion) {
				return definicion.getMetadata().isIndependent();
			}
		};
		escaner.addIncludeFilter(new AssignableTypeFilter(Record.class));
		Map<String, Class<?>> registros = new HashMap<>();
		Set<String> repetidos = new HashSet<>();
		for (BeanDefinition definicion : escaner.findCandidateComponents(PAQUETE)) {
			Class<?> clase = ClassUtils.resolveClassName(definicion.getBeanClassName(),
					ConfiguracionOpenApi.class.getClassLoader());
			if (registros.putIfAbsent(clase.getSimpleName(), clase) != null) {
				repetidos.add(clase.getSimpleName());
			}
		}
		repetidos.forEach(registros::remove);
		return registros;
	}
}
