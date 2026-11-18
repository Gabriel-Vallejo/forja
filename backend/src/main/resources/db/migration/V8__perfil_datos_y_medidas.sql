-- Perfil manual completo (PRD v0.1): sexo, experiencia y objetivo (preferencias, en claro) y las medidas del cuerpo
-- (altura, peso y circunferencias), que son dato de salud. Fechas en UTC. El año de nacimiento vive en `usuario`.
-- Todo cuelga de `usuario` con ON DELETE CASCADE: borrar la cuenta borra lo demás (ADR 0004).

-- Preferencias: no son dato de salud. Una fila por usuario; sin fila, el backend responde `no_indica` y sin definir.
-- `sexo` solo sirve para la fórmula de % de grasa y los estándares de fuerza. `experiencia` y `objetivo` pueden quedar
-- sin definir (NULL) hasta que el usuario elija.
CREATE TABLE perfil_datos (
    usuario_id     BIGINT      NOT NULL PRIMARY KEY,
    sexo           ENUM ('hombre', 'mujer', 'no_indica') NOT NULL,
    experiencia    ENUM ('principiante', 'intermedio', 'avanzado') NULL,
    objetivo       ENUM ('fuerza', 'hipertrofia', 'fuerza_hipertrofia') NULL,
    actualizado_en DATETIME(6) NOT NULL,
    CONSTRAINT fk_perfil_datos_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Medidas del cuerpo: dato de salud (ADR 0004 §5 pide cifrar medidas y peso). Exigen el consentimiento `medidas_salud`;
-- revocarlo borra la fila (RF-46). Una sola columna cifrada (AES-GCM, JSON con las cinco medidas; la tabla, la columna
-- y el UUID del usuario van como datos asociados): no se busca ni se indexa por ellas. El % de grasa no se guarda.
CREATE TABLE perfil_medidas (
    usuario_id     BIGINT         NOT NULL PRIMARY KEY,
    datos          VARBINARY(512) NOT NULL,
    actualizado_en DATETIME(6)    NOT NULL,
    CONSTRAINT fk_perfil_medidas_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
