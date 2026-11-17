-- Ajustes del plan del usuario: días por semana, minutos por sesión y material. Se usan al generar o regenerar.
-- Son preferencias de entreno, no dato de salud: van en claro, sin cifrar. Fechas en UTC.
-- Todo cuelga de `usuario` con ON DELETE CASCADE: borrar la cuenta borra los ajustes.
-- Sin fila, el backend responde los valores por defecto (no se siembran filas).

CREATE TABLE perfil_entreno (
    usuario_id         BIGINT   NOT NULL PRIMARY KEY,         -- una fila por usuario
    dias_por_semana    TINYINT  NOT NULL,
    minutos_por_sesion SMALLINT NOT NULL,
    actualizado_en     DATETIME(6) NOT NULL,
    -- Mismos topes que EntradaRutina / SolicitudRutina (2-6 días, 30-120 minutos).
    CONSTRAINT ck_perfil_entreno_dias CHECK (dias_por_semana BETWEEN 2 AND 6),
    CONSTRAINT ck_perfil_entreno_minutos CHECK (minutos_por_sesion BETWEEN 30 AND 120),
    CONSTRAINT fk_perfil_entreno_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- El material va en una tabla puente y no en una lista serializada: el repo está en 3FN y así la FK a `material`
-- impide guardar códigos que no existen.
CREATE TABLE perfil_entreno_material (
    usuario_id  BIGINT NOT NULL,
    material_id INT    NOT NULL,
    PRIMARY KEY (usuario_id, material_id),
    CONSTRAINT fk_perfil_entreno_material_perfil FOREIGN KEY (usuario_id)
        REFERENCES perfil_entreno (usuario_id) ON DELETE CASCADE,
    CONSTRAINT fk_perfil_entreno_material_material FOREIGN KEY (material_id) REFERENCES material (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
