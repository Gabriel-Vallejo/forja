-- Percentiles de fuerza de OpenPowerlifting (RF-30, ADR 0005): los rellena R__estandares_fuerza.sql, generado por
-- database/estandares/calcular.py. Un grupo es levantamiento + sexo + franja de peso; peso_hasta_kg NULL = sin techo.
CREATE TABLE estandar_fuerza (
    levantamiento  ENUM ('sentadilla', 'banca', 'peso_muerto')  NOT NULL,
    sexo           ENUM ('hombre', 'mujer')                      NOT NULL,
    peso_desde_kg  DECIMAL(5, 1)                                 NOT NULL,
    peso_hasta_kg  DECIMAL(5, 1)                                 NULL,
    percentil      TINYINT UNSIGNED                              NOT NULL,
    kg             DECIMAL(6, 1)                                 NOT NULL,
    muestra        INT UNSIGNED                                  NOT NULL,
    PRIMARY KEY (levantamiento, sexo, peso_desde_kg, percentil),
    CONSTRAINT ck_estandar_percentil CHECK (percentil BETWEEN 1 AND 99)
);
