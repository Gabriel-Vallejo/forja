-- Catálogo de ejercicios (ADR 0005 y 0007). MySQL 8.4, 3FN.
-- Las tablas de referencia se siembran aquí; los ejercicios, en R__catalogo_semilla.sql.

-- Músculos que trabaja un ejercicio.
CREATE TABLE musculo (
    id     INT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL,
    nombre VARCHAR(60) NOT NULL,
    CONSTRAINT uk_musculo_codigo UNIQUE (codigo)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Patrón de movimiento: la columna vertebral de la escalera de sustitución.
CREATE TABLE patron_movimiento (
    id     INT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL,
    nombre VARCHAR(60) NOT NULL,
    CONSTRAINT uk_patron_movimiento_codigo UNIQUE (codigo)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Región corporal que carga un ejercicio; las restricciones del usuario apuntan a estas.
CREATE TABLE region (
    id     INT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL,
    nombre VARCHAR(60) NOT NULL,
    CONSTRAINT uk_region_codigo UNIQUE (codigo)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE material (
    id     INT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL,
    nombre VARCHAR(60) NOT NULL,
    CONSTRAINT uk_material_codigo UNIQUE (codigo)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE ejercicio (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_externo           VARCHAR(120) NOT NULL,
    nombre_original      VARCHAR(200) NOT NULL,
    nombre_es            VARCHAR(200) NULL,
    patron_id            INT NOT NULL,
    mecanica             ENUM ('compuesto', 'aislamiento') NULL,
    nivel                ENUM ('principiante', 'intermedio', 'avanzado') NOT NULL,
    categoria            VARCHAR(40) NOT NULL,
    carga_axial          BOOLEAN NOT NULL,
    impacto              BOOLEAN NOT NULL,
    agarre               ENUM ('ninguno', 'ligero', 'pesado') NOT NULL,
    lateralidad          ENUM ('bilateral', 'unilateral') NOT NULL,
    por_encima_cabeza    BOOLEAN NOT NULL,
    posicion_estirada    BOOLEAN NOT NULL,
    familia_sustitucion  VARCHAR(60) NULL,
    rango_movimiento     ENUM ('completo', 'reducido') NOT NULL,
    implemento_id        INT NOT NULL,
    apto_fuerza          BOOLEAN NOT NULL,
    -- derivado: reglas automáticas; curado: fijado a mano. revisado: lo ha mirado una persona.
    origen_etiquetado    ENUM ('derivado', 'curado') NOT NULL,
    revisado             BOOLEAN NOT NULL DEFAULT FALSE,
    fuente               VARCHAR(60) NOT NULL,
    licencia             VARCHAR(60) NOT NULL,
    autor                VARCHAR(120) NULL,
    version_fuente       VARCHAR(64) NOT NULL,
    CONSTRAINT uk_ejercicio_id_externo UNIQUE (id_externo),
    CONSTRAINT fk_ejercicio_patron FOREIGN KEY (patron_id) REFERENCES patron_movimiento (id),
    CONSTRAINT fk_ejercicio_implemento FOREIGN KEY (implemento_id) REFERENCES material (id),
    -- Búsqueda por patrón al armar cada día de la rutina.
    INDEX idx_ejercicio_patron (patron_id),
    -- Escalera de sustitución: variantes de una familia, de más a menos rango.
    INDEX idx_ejercicio_familia (familia_sustitucion, rango_movimiento),
    -- Cola de revisión: curados sin revisar y derivados pendientes de curar.
    INDEX idx_ejercicio_origen (origen_etiquetado, revisado)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- rol primario pesa 1.0 y secundario 0.5; el CHECK impide que se mezclen.
CREATE TABLE ejercicio_musculo (
    ejercicio_id BIGINT NOT NULL,
    musculo_id   INT NOT NULL,
    rol          ENUM ('primario', 'secundario') NOT NULL,
    peso         DECIMAL(2, 1) NOT NULL,
    PRIMARY KEY (ejercicio_id, musculo_id),
    CONSTRAINT fk_ejercicio_musculo_ejercicio FOREIGN KEY (ejercicio_id) REFERENCES ejercicio (id),
    CONSTRAINT fk_ejercicio_musculo_musculo FOREIGN KEY (musculo_id) REFERENCES musculo (id),
    CONSTRAINT ck_ejercicio_musculo_peso CHECK (
        (rol = 'primario' AND peso = 1.0) OR (rol = 'secundario' AND peso = 0.5)),
    -- Qué ejercicios trabajan un músculo, primarios primero.
    INDEX idx_ejercicio_musculo_musculo (musculo_id, rol)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE ejercicio_region (
    ejercicio_id BIGINT NOT NULL,
    region_id    INT NOT NULL,
    PRIMARY KEY (ejercicio_id, region_id),
    CONSTRAINT fk_ejercicio_region_ejercicio FOREIGN KEY (ejercicio_id) REFERENCES ejercicio (id),
    CONSTRAINT fk_ejercicio_region_region FOREIGN KEY (region_id) REFERENCES region (id),
    -- Qué ejercicios cargan una región (para las restricciones del usuario).
    INDEX idx_ejercicio_region_region (region_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE ejercicio_material (
    ejercicio_id BIGINT NOT NULL,
    material_id  INT NOT NULL,
    PRIMARY KEY (ejercicio_id, material_id),
    CONSTRAINT fk_ejercicio_material_ejercicio FOREIGN KEY (ejercicio_id) REFERENCES ejercicio (id),
    CONSTRAINT fk_ejercicio_material_material FOREIGN KEY (material_id) REFERENCES material (id),
    -- Qué ejercicios puede hacer quien solo tiene cierto material.
    INDEX idx_ejercicio_material_material (material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO musculo (codigo, nombre) VALUES
    ('abdominales', 'Abdominales'),
    ('isquiotibiales', 'Isquiotibiales'),
    ('aductores', 'Aductores'),
    ('cuadriceps', 'Cuádriceps'),
    ('biceps', 'Bíceps'),
    ('hombros', 'Hombros'),
    ('pecho', 'Pecho'),
    ('espalda_media', 'Espalda media'),
    ('gemelos', 'Gemelos'),
    ('gluteos', 'Glúteos'),
    ('lumbares', 'Lumbares'),
    ('dorsales', 'Dorsales'),
    ('triceps', 'Tríceps'),
    ('trapecio', 'Trapecio'),
    ('antebrazos', 'Antebrazos'),
    ('cuello', 'Cuello'),
    ('abductores', 'Abductores');

INSERT INTO patron_movimiento (codigo, nombre) VALUES
    ('sentadilla', 'Sentadilla'),
    ('bisagra', 'Bisagra de cadera'),
    ('zancada', 'Zancada'),
    ('empuje_horizontal', 'Empuje horizontal'),
    ('empuje_vertical', 'Empuje vertical'),
    ('traccion_horizontal', 'Tracción horizontal'),
    ('traccion_vertical', 'Tracción vertical'),
    ('aislamiento_brazo', 'Aislamiento de brazo'),
    ('aislamiento_pierna', 'Aislamiento de pierna'),
    ('core', 'Core'),
    ('acarreo', 'Acarreo'),
    ('pliometria', 'Pliometría'),
    ('movilidad', 'Movilidad'),
    ('cardio', 'Cardio');

INSERT INTO region (codigo, nombre) VALUES
    ('columna_lumbar', 'Columna lumbar'),
    ('columna_cervical', 'Columna cervical'),
    ('hombro', 'Hombro'),
    ('codo', 'Codo'),
    ('muneca', 'Muñeca'),
    ('cadera', 'Cadera'),
    ('rodilla', 'Rodilla'),
    ('tobillo', 'Tobillo');

INSERT INTO material (codigo, nombre) VALUES
    ('barra', 'Barra'),
    ('mancuerna', 'Mancuerna'),
    ('maquina', 'Máquina'),
    ('polea', 'Polea'),
    ('peso_corporal', 'Peso corporal'),
    ('kettlebell', 'Kettlebell'),
    ('banda', 'Banda elástica'),
    ('barra_ez', 'Barra EZ'),
    ('balon_medicinal', 'Balón medicinal'),
    ('fitball', 'Fitball'),
    ('rodillo', 'Rodillo'),
    ('otro', 'Otro');
