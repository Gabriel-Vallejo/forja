package es.forja.comun.repositorio;

import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/** Paso entre los tipos de Java y los de MySQL: UUID en BINARY(16), fechas en DATETIME(6) UTC. */
public final class Conversiones {

	private static final int BYTES_UUID = 16;

	private Conversiones() {
	}

	public static byte[] aBytes(UUID uuid) {
		return ByteBuffer.allocate(BYTES_UUID).putLong(uuid.getMostSignificantBits())
				.putLong(uuid.getLeastSignificantBits()).array();
	}

	public static UUID aUuid(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		return new UUID(buffer.getLong(), buffer.getLong());
	}

	public static LocalDateTime aSql(Instant instante) {
		return instante == null ? null : LocalDateTime.ofInstant(instante, ZoneOffset.UTC);
	}

	public static Instant instante(ResultSet rs, String columna) throws SQLException {
		LocalDateTime fecha = rs.getObject(columna, LocalDateTime.class);
		return fecha == null ? null : fecha.toInstant(ZoneOffset.UTC);
	}

	public static UUID uuid(ResultSet rs, String columna) throws SQLException {
		return aUuid(rs.getBytes(columna));
	}
}
