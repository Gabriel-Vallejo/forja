-- Cuenta y autenticación (ADR 0009). Todas las fechas van en UTC.
-- Todo cuelga de `usuario` con ON DELETE CASCADE: borrar la cuenta borra lo demás (ADR 0004).

CREATE TABLE usuario (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid             BINARY(16)   NOT NULL,                -- es el `sub` del token de acceso
    correo           VARCHAR(254) NOT NULL,                -- ya normalizado: sin espacios y en minúsculas
    hash_contrasena  VARCHAR(255) NOT NULL,                -- Argon2id
    verificado       TINYINT(1)   NOT NULL DEFAULT 0,
    anio_nacimiento  SMALLINT     NOT NULL,                -- ADR 0004, enmienda 6: nunca la fecha completa
    mayor_de_edad    TINYINT(1)   NOT NULL,
    fallos_seguidos  INT          NOT NULL DEFAULT 0,
    bloqueado_hasta  DATETIME(6)  NULL,
    creado_en        DATETIME(6)  NOT NULL,
    CONSTRAINT uk_usuario_uuid UNIQUE (uuid),
    CONSTRAINT uk_usuario_correo UNIQUE (correo)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE dispositivo (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid              BINARY(16)   NOT NULL,               -- es el `sid` del token de acceso
    usuario_id        BIGINT       NOT NULL,
    nombre            VARCHAR(80)  NOT NULL,
    plataforma        ENUM ('movil', 'pc') NOT NULL,
    ultima_actividad  DATETIME(6)  NOT NULL,
    revocado_en       DATETIME(6)  NULL,
    CONSTRAINT uk_dispositivo_uuid UNIQUE (uuid),
    CONSTRAINT fk_dispositivo_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX ix_dispositivo_usuario ON dispositivo (usuario_id);

CREATE TABLE token_refresco (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    dispositivo_id  BIGINT      NOT NULL,
    familia         BINARY(16)  NOT NULL,
    hash            BINARY(32)  NOT NULL,                  -- SHA-256 del token (256 bits de azar: no hace falta sal)
    caduca_en       DATETIME(6) NOT NULL,
    usado_en        DATETIME(6) NULL,
    revocado_en     DATETIME(6) NULL,
    CONSTRAINT uk_token_refresco_hash UNIQUE (hash),
    CONSTRAINT fk_token_refresco_dispositivo FOREIGN KEY (dispositivo_id)
        REFERENCES dispositivo (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX ix_token_refresco_dispositivo ON token_refresco (dispositivo_id);

CREATE TABLE token_un_uso (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id   BIGINT      NOT NULL,
    tipo         ENUM ('verificacion', 'recuperacion') NOT NULL,
    hash         BINARY(32)  NOT NULL,                     -- SHA-256 del token
    caduca_en    DATETIME(6) NOT NULL,
    usado_en     DATETIME(6) NULL,
    CONSTRAINT uk_token_un_uso_hash UNIQUE (hash),
    CONSTRAINT fk_token_un_uso_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX ix_token_un_uso_usuario ON token_un_uso (usuario_id);
