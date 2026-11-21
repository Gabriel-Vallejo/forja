package es.forja.cuenta.servicio;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Correo transaccional por Azure Communication Services (ADR 0009 §5). Sin cadena de conexión no se envía nada.
 *
 * @param cadenaConexion la de ACS: {@code endpoint=https://…/;accesskey=…}
 * @param remitente dirección del dominio de ACS, p. ej. {@code DoNotReply@….azurecomm.net}
 * @param urlEnlaces base pública del backend; los enlaces van a {@code /cuenta/*.html} con el token en el fragmento
 */
@ConfigurationProperties("forja.correo")
public record PropiedadesCorreo(String cadenaConexion, String remitente, String urlEnlaces) {

	public boolean configurado() {
		return cadenaConexion != null && !cadenaConexion.isBlank();
	}
}
