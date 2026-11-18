-- Preferencias de los ajustes del plan (PRD v0.1): lo que el usuario quita, enfoca o fija con «una serie más/menos»
-- y si quiere más variedad. Se respetan cada vez que se genera o regenera el plan.
-- Son preferencias de entreno, no dato de salud: van en claro. Fechas en UTC.
-- Todo cuelga de `usuario` con ON DELETE CASCADE: borrar la cuenta lo borra.
-- Se numera V9 porque la V8 la está creando otra rama a la vez; Flyway admite el hueco.

CREATE TABLE preferencia_plan (
    usuario_id     BIGINT      NOT NULL PRIMARY KEY,
    variedad       BOOLEAN     NOT NULL,             -- prefiere ejercicios distintos a los del plan anterior
    actualizado_en DATETIME(6) NOT NULL,
    CONSTRAINT fk_preferencia_plan_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Un ejercicio quitado («no recomendar», «reemplazar») o con sus series de la semana fijadas, nunca las dos cosas.
CREATE TABLE preferencia_ejercicio (
    usuario_id    BIGINT  NOT NULL,
    ejercicio_id  BIGINT  NOT NULL,
    excluido      BOOLEAN NOT NULL,
    series_semana TINYINT NULL,
    PRIMARY KEY (usuario_id, ejercicio_id),
    CONSTRAINT ck_preferencia_ejercicio_tipo CHECK ((excluido AND series_semana IS NULL)
        OR (NOT excluido AND series_semana BETWEEN 2 AND 30)),
    CONSTRAINT fk_preferencia_ejercicio_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT fk_preferencia_ejercicio_ejercicio FOREIGN KEY (ejercicio_id) REFERENCES ejercicio (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Músculos a enfocar (los prioritarios del solver), como mucho 2.
CREATE TABLE preferencia_musculo (
    usuario_id BIGINT NOT NULL,
    musculo_id INT    NOT NULL,
    PRIMARY KEY (usuario_id, musculo_id),
    CONSTRAINT fk_preferencia_musculo_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT fk_preferencia_musculo_musculo FOREIGN KEY (musculo_id) REFERENCES musculo (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
