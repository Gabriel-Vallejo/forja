-- Tests de movilidad caseros (RF-08, ADR 0007): solo el tramo de cada test y cuándo se hizo. Fechas en UTC.
-- Una fila por usuario y test; sin fila, el test está sin hacer. No se guarda la medida exacta (ADR 0004 §4).
-- Exigen el consentimiento `medidas_salud`: revocarlo borra las filas (RF-46). Van en claro, como en el
-- diagrama de docs/arquitectura.md: el tramo no está en la lista de columnas cifradas del ADR 0004 §5.
-- Cuelga de `usuario` con ON DELETE CASCADE: borrar la cuenta lo borra.

CREATE TABLE test_movilidad (
    usuario_id BIGINT      NOT NULL,
    tipo       ENUM ('tobillo', 'cadera', 'hombro') NOT NULL,
    tramo      ENUM ('limitado', 'medio', 'bueno')  NOT NULL,
    hecho_en   DATETIME(6) NOT NULL,
    PRIMARY KEY (usuario_id, tipo),
    CONSTRAINT fk_test_movilidad_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
