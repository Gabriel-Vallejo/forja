-- Al borrar una cuenta, los hashes de sus refrescos vivos (ADR 0009 §9). Sin usuario ni dispositivo: solo sirve para
-- que un móvil con sesión abierta reciba 410 al refrescar y borre lo que guarda de esa cuenta. Se purga al caducar.
CREATE TABLE refresco_cuenta_borrada (
    hash      BINARY(32)  NOT NULL PRIMARY KEY,                -- SHA-256 del token, como en token_refresco
    caduca_en DATETIME(6) NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX ix_refresco_cuenta_borrada_caduca ON refresco_cuenta_borrada (caduca_en);
