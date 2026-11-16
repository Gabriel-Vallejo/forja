package es.forja.cuenta.servicio;

import org.springframework.stereotype.Component;

/** Reglas de contraseña del registro y del restablecimiento (ADR 0009 §1). */
@Component
public class PoliticaContrasena {

	public static final int LONGITUD_MINIMA = 12;
	public static final int LONGITUD_MAXIMA = 128;

	private final ListaContrasenasFiltradas filtradas;

	public PoliticaContrasena(ListaContrasenasFiltradas filtradas) {
		this.filtradas = filtradas;
	}

	public void exigir(String contrasena) {
		if (contrasena == null || contrasena.length() < LONGITUD_MINIMA) {
			throw ErrorCuenta.datosNoValidos("La contraseña debe tener al menos " + LONGITUD_MINIMA + " caracteres.");
		}
		if (contrasena.length() > LONGITUD_MAXIMA) {
			throw ErrorCuenta.datosNoValidos("La contraseña no puede pasar de " + LONGITUD_MAXIMA + " caracteres.");
		}
		if (filtradas.contiene(contrasena)) {
			throw ErrorCuenta.datosNoValidos("Esa contraseña ha salido en filtraciones. Elige otra.");
		}
	}
}
