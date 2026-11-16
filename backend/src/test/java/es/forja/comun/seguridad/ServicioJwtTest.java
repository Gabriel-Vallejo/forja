package es.forja.comun.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import es.forja.ClavesDePrueba;
import es.forja.JwtDePrueba;

/** JWT sin Spring: los tokens ajenos se fabrican a mano con la API de firma del JDK. */
class ServicioJwtTest {

	private static final Instant AHORA = Instant.parse("2026-11-16T12:00:00Z");
	private static final UUID USUARIO = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID DISPOSITIVO = UUID.fromString("22222222-2222-2222-2222-222222222222");

	private Clock reloj;
	private ServicioJwt jwt;

	@BeforeEach
	void preparar() throws Exception {
		reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
		jwt = construir(reloj);
	}

	private static ServicioJwt construir(Clock reloj) throws Exception {
		var privada = (Ed25519PrivateKeyParameters) PrivateKeyFactory.createKey(
				ClavesDePrueba.ACTUAL.getPrivate().getEncoded());
		var anterior = (Ed25519PublicKeyParameters) PublicKeyFactory.createKey(
				ClavesDePrueba.ANTERIOR.getPublic().getEncoded());
		return new ServicioJwt(reloj, ClavesDePrueba.KID_ACTUAL, privada, ClavesDePrueba.KID_ANTERIOR, anterior);
	}

	private static String forjar(String cabecera, String carga, KeyPair firmante) {
		return JwtDePrueba.forjar(cabecera, carga, firmante);
	}

	private static String cabecera(String alg, String kid) {
		return JwtDePrueba.cabecera(alg, kid);
	}

	private static String carga(long iat, long exp) {
		return JwtDePrueba.carga(USUARIO, DISPOSITIVO, iat, exp);
	}

	private static String base64Url(String texto) {
		return JwtDePrueba.base64Url(texto);
	}

	private static long segundos(Duration desplazamiento) {
		return AHORA.plus(desplazamiento).getEpochSecond();
	}

	@Test
	void aceptaUnTokenPropioConSubYSid() {
		String token = jwt.emitir(USUARIO, DISPOSITIVO);

		assertThat(jwt.validar(token)).contains(new ClaimsAcceso(USUARIO, DISPOSITIVO));
	}

	@Test
	void elTokenCaducaALosQuinceMinutos() throws Exception {
		String token = jwt.emitir(USUARIO, DISPOSITIVO);

		ServicioJwt casiCaducado = construir(Clock.fixed(AHORA.plus(Duration.ofMinutes(14)), ZoneOffset.UTC));
		ServicioJwt caducado = construir(Clock.fixed(AHORA.plus(Duration.ofMinutes(15)), ZoneOffset.UTC));

		assertThat(casiCaducado.validar(token)).isPresent();
		assertThat(caducado.validar(token)).isEmpty();
	}

	@Test
	void aceptaUnTokenFirmadoConLaClaveAnterior() throws Exception {
		String token = forjar(cabecera("EdDSA", ClavesDePrueba.KID_ANTERIOR),
				carga(segundos(Duration.ZERO), segundos(Duration.ofMinutes(5))), ClavesDePrueba.ANTERIOR);

		assertThat(jwt.validar(token)).isPresent();
	}

	@Test
	void rechazaUnKidDesconocido() throws Exception {
		String token = forjar(cabecera("EdDSA", "otro-kid"),
				carga(segundos(Duration.ZERO), segundos(Duration.ofMinutes(5))), ClavesDePrueba.ACTUAL);

		assertThat(jwt.validar(token)).isEmpty();
	}

	@Test
	void rechazaUnAlgDistintoDeEdDsaAunqueLaFirmaSeaBuena() throws Exception {
		for (String alg : new String[] { "none", "HS256", "RS256", "Ed25519", "eddsa" }) {
			String token = forjar(cabecera(alg, ClavesDePrueba.KID_ACTUAL),
					carga(segundos(Duration.ZERO), segundos(Duration.ofMinutes(5))), ClavesDePrueba.ACTUAL);

			assertThat(jwt.validar(token)).as("alg %s", alg).isEmpty();
		}
	}

	@Test
	void rechazaUnTokenCaducado() throws Exception {
		String token = forjar(cabecera("EdDSA", ClavesDePrueba.KID_ACTUAL),
				carga(segundos(Duration.ofMinutes(-20)), segundos(Duration.ofMinutes(-5))), ClavesDePrueba.ACTUAL);

		assertThat(jwt.validar(token)).isEmpty();
	}

	@Test
	void rechazaUnTokenFirmadoConOtraClaveConElKidActual() throws Exception {
		KeyPair intrusa = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
		String token = forjar(cabecera("EdDSA", ClavesDePrueba.KID_ACTUAL),
				carga(segundos(Duration.ZERO), segundos(Duration.ofMinutes(5))), intrusa);

		assertThat(jwt.validar(token)).isEmpty();
	}

	@Test
	void rechazaUnaCargaManipuladaConLaFirmaOriginal() {
		String[] partes = jwt.emitir(USUARIO, DISPOSITIVO).split("\\.");
		String otraCarga = base64Url(carga(segundos(Duration.ZERO), segundos(Duration.ofDays(365))));

		assertThat(jwt.validar(partes[0] + "." + otraCarga + "." + partes[2])).isEmpty();
	}

	@Test
	void rechazaLoQueNoEsUnJwt() {
		for (String basura : new String[] { "", "a", "a.b", "a.b.c", "a.b.c.d", "..", "eyJ.eyJ.", "ñ.ñ.ñ" }) {
			assertThat(jwt.validar(basura)).as("token '%s'", basura).isEmpty();
		}
	}

	@Test
	void rechazaUnTokenSinExpOSinSub() throws Exception {
		String sinExp = forjar(cabecera("EdDSA", ClavesDePrueba.KID_ACTUAL),
				"{\"sub\":\"" + USUARIO + "\",\"sid\":\"" + DISPOSITIVO + "\",\"iat\":1}", ClavesDePrueba.ACTUAL);
		String sinSub = forjar(cabecera("EdDSA", ClavesDePrueba.KID_ACTUAL),
				"{\"sid\":\"" + DISPOSITIVO + "\",\"iat\":1,\"exp\":" + segundos(Duration.ofMinutes(5)) + "}",
				ClavesDePrueba.ACTUAL);

		assertThat(jwt.validar(sinExp)).isEmpty();
		assertThat(jwt.validar(sinSub)).isEmpty();
	}

	@Test
	void sinClaveAnteriorSoloAceptaLaActual() throws Exception {
		var privada = (Ed25519PrivateKeyParameters) PrivateKeyFactory.createKey(
				ClavesDePrueba.ACTUAL.getPrivate().getEncoded());
		ServicioJwt soloActual = new ServicioJwt(reloj, ClavesDePrueba.KID_ACTUAL, privada, null, null);
		String delAnterior = forjar(cabecera("EdDSA", ClavesDePrueba.KID_ANTERIOR),
				carga(segundos(Duration.ZERO), segundos(Duration.ofMinutes(5))), ClavesDePrueba.ANTERIOR);

		assertThat(soloActual.validar(delAnterior)).isEmpty();
		assertThat(soloActual.validar(soloActual.emitir(USUARIO, DISPOSITIVO))).isPresent();
	}

	@Test
	void desdePropiedadesRechazaUnaClaveQueNoEsEd25519() {
		var propiedades = new PropiedadesJwt("k", Base64.getEncoder().encodeToString("basura".getBytes()), null, null);

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> ServicioJwt.desdePropiedades(reloj, propiedades))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("FORJA_JWT_CLAVE_PRIVADA_ACTUAL");
	}
}
