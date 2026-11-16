package es.forja.cuenta.servicio;

/** Contraseñas que ya han salido en filtraciones; la fuente de la lista queda por elegir (ADR 0009). */
public interface ListaContrasenasFiltradas {

	boolean contiene(String contrasena);
}
