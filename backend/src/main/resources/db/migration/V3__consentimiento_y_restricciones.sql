-- Consentimiento y limitaciones del usuario (ADR 0004, enmiendas 3, 5 y 8; ADR 0007). Fechas en UTC.
-- Ambas tablas cuelgan de `usuario` con ON DELETE CASCADE: borrar la cuenta borra todo.

CREATE TABLE consentimiento (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id     BIGINT      NOT NULL,
    tipo           ENUM ('fotos_cuerpo', 'medidas_salud', 'lesiones', 'limitaciones', 'suplementos') NOT NULL,
    version_texto  VARCHAR(20) NOT NULL,                   -- versión del texto que el usuario vio al darlo
    dado_en        DATETIME(6) NOT NULL,
    revocado_en    DATETIME(6) NULL,
    -- 1 mientras el consentimiento sigue vivo y NULL cuando se revoca: como NULL no choca en un índice
    -- único, el índice deja un solo consentimiento vivo por usuario y tipo y guarda todos los revocados.
    vivo           TINYINT GENERATED ALWAYS AS (IF(revocado_en IS NULL, 1, NULL)) VIRTUAL,
    CONSTRAINT uk_consentimiento_vivo UNIQUE (usuario_id, tipo, vivo),
    CONSTRAINT fk_consentimiento_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE restriccion (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid           BINARY(16)     NOT NULL,                -- también va en los datos asociados del cifrado
    usuario_id     BIGINT         NOT NULL,
    -- Restricción entera serializada y cifrada (AES-GCM, ADR 0004 enmienda 8): ni el tipo ni la región en claro.
    datos          VARBINARY(4096) NOT NULL,
    creado_en      DATETIME(6)    NOT NULL,
    actualizado_en DATETIME(6)    NOT NULL,
    CONSTRAINT uk_restriccion_uuid UNIQUE (uuid),
    CONSTRAINT fk_restriccion_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX ix_restriccion_usuario ON restriccion (usuario_id);
