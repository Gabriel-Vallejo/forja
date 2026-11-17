-- Bloqueo de «generar plan» tras una bandera roja o un cribado urgente (ADR 0007, enmienda). Fechas en UTC.
-- Solo se guarda el resultado, nunca las respuestas. Cuelga de `usuario` con ON DELETE CASCADE.

CREATE TABLE bloqueo_salud (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid        BINARY(16)      NOT NULL,                  -- también va en los datos asociados del cifrado
    usuario_id  BIGINT          NOT NULL,
    -- Qué cuestionario lo puso: cada uno levanta solo el suyo. No es el nivel, así que va en claro.
    origen      ENUM ('cribado', 'banderas') NOT NULL,
    -- Nivel y fecha de evaluación serializados y cifrados (AES-GCM, ADR 0004 enmienda 8): es dato derivado de salud.
    -- Con un ENUM en claro se vería en la base «urgencias_hoy»; por eso va cifrado como `restriccion.datos`.
    datos       VARBINARY(256)  NOT NULL,
    CONSTRAINT uk_bloqueo_salud_uuid UNIQUE (uuid),
    CONSTRAINT uk_bloqueo_salud_usuario_origen UNIQUE (usuario_id, origen),
    CONSTRAINT fk_bloqueo_salud_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
