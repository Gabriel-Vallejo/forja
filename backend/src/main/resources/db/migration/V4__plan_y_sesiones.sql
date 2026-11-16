-- Plan versionado y registro de sesiones sin conexión (ADR 0008). Fechas en UTC.
-- Todo cuelga de `usuario` con ON DELETE CASCADE: borrar la cuenta borra todo (ADR 0004 §6).

-- Cada plan generado es una versión inmutable: nunca se actualiza, solo se añaden versiones nuevas.
CREATE TABLE plan_version (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid        BINARY(16)  NOT NULL,
    usuario_id  BIGINT      NOT NULL,
    creado_en   DATETIME(6) NOT NULL,
    estado      ENUM ('optimo', 'factible') NOT NULL,
    -- Traza del solver sin texto derivado de las limitaciones (dato de salud): ver ServicioPlan.
    traza       JSON        NOT NULL,
    CONSTRAINT uk_plan_version_uuid UNIQUE (uuid),
    CONSTRAINT fk_plan_version_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE,
    -- El plan actual es el último del usuario.
    INDEX ix_plan_version_usuario (usuario_id, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Objetivo de cada ejercicio de la versión y la regla de la siguiente sesión: si completas
-- repeticiones_max con rir_objetivo o menos en todas las series de trabajo, la próxima vez +incremento_kg.
CREATE TABLE ejercicio_plan (
    plan_version_id  BIGINT        NOT NULL,
    dia              SMALLINT      NOT NULL,
    orden            SMALLINT      NOT NULL,
    ejercicio_id     BIGINT        NOT NULL,
    series           SMALLINT      NOT NULL,
    repeticiones_min SMALLINT      NOT NULL,
    repeticiones_max SMALLINT      NOT NULL,
    rir_objetivo     TINYINT       NOT NULL,
    carga_kg         DECIMAL(6, 2) NULL,                   -- nula hasta la primera sesión de ese ejercicio
    incremento_kg    DECIMAL(5, 2) NOT NULL,               -- 0 con peso corporal: se progresa por repeticiones
    PRIMARY KEY (plan_version_id, dia, orden),
    CONSTRAINT fk_ejercicio_plan_version FOREIGN KEY (plan_version_id)
        REFERENCES plan_version (id) ON DELETE CASCADE,
    CONSTRAINT fk_ejercicio_plan_ejercicio FOREIGN KEY (ejercicio_id) REFERENCES ejercicio (id),
    CONSTRAINT ck_ejercicio_plan_rango CHECK (repeticiones_min <= repeticiones_max)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- La clave es el UUID v7 que el móvil le pone a la sesión: también es la clave de idempotencia.
CREATE TABLE sesion (
    uuid             BINARY(16)  NOT NULL PRIMARY KEY,
    usuario_id       BIGINT      NOT NULL,
    plan_version_id  BIGINT      NULL,                     -- nulo si se entrena sin plan
    dia_plan         SMALLINT    NULL,
    realizada_en     DATETIME(6) NOT NULL,                 -- reloj del móvil: lo que ve el usuario
    recibida_en      DATETIME(6) NOT NULL,                 -- reloj del servidor, para auditoría
    reloj_desfasado  BOOLEAN     NOT NULL,                 -- más de 24 h entre los dos relojes: para revisar
    CONSTRAINT fk_sesion_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE,
    -- Si se borrara una versión, se van con ella sus sesiones (solo ocurre al borrar la cuenta).
    CONSTRAINT fk_sesion_plan_version FOREIGN KEY (plan_version_id)
        REFERENCES plan_version (id) ON DELETE CASCADE,
    -- Listado por usuario desde una fecha.
    INDEX ix_sesion_usuario_fecha (usuario_id, realizada_en, uuid),
    INDEX ix_sesion_plan_version (plan_version_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Las series solo se añaden (ADR 0008 §3): una corrección es una serie con corrige_a y una anulación,
-- una serie con anulada = TRUE y corrige_a apuntando a la original.
CREATE TABLE serie_realizada (
    uuid          BINARY(16)    NOT NULL PRIMARY KEY,
    sesion_uuid   BINARY(16)    NOT NULL,
    ejercicio_id  BIGINT        NOT NULL,
    orden         SMALLINT      NOT NULL,
    peso_kg       DECIMAL(6, 2) NOT NULL,
    repeticiones  SMALLINT      NOT NULL,
    rir           TINYINT       NULL,
    rpe           DECIMAL(3, 1) NULL,
    calentamiento BOOLEAN       NOT NULL,
    -- Dolor 0-10 cifrado (AES-GCM, ADR 0004 enmienda 8); solo se guarda con el consentimiento de limitaciones.
    dolor         VARBINARY(64) NULL,
    corrige_a     BINARY(16)    NULL,                      -- serie del mismo usuario, comprobado en el servicio
    anulada       BOOLEAN       NOT NULL,
    realizada_en  DATETIME(6)   NOT NULL,
    CONSTRAINT fk_serie_sesion FOREIGN KEY (sesion_uuid)
        REFERENCES sesion (uuid) ON DELETE CASCADE,
    CONSTRAINT fk_serie_ejercicio FOREIGN KEY (ejercicio_id) REFERENCES ejercicio (id),
    -- Una serie solo se corrige una vez: así hay una única última versión válida y no se bifurca el historial.
    CONSTRAINT uk_serie_corrige_a UNIQUE (corrige_a),
    CONSTRAINT ck_serie_anulada_apunta CHECK (anulada = FALSE OR corrige_a IS NOT NULL),
    INDEX ix_serie_sesion (sesion_uuid, orden),
    -- Última sesión de un ejercicio, para la progresión.
    INDEX ix_serie_ejercicio (ejercicio_id, sesion_uuid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Última versión válida de cada serie: sin las corregidas, sin las anuladas y sin las propias anulaciones.
CREATE VIEW serie_vigente AS
SELECT s.uuid, s.sesion_uuid, s.ejercicio_id, s.orden, s.peso_kg, s.repeticiones, s.rir, s.rpe,
       s.calentamiento, s.dolor, s.corrige_a, s.anulada, s.realizada_en
FROM serie_realizada s
WHERE s.anulada = FALSE
  AND NOT EXISTS (SELECT 1 FROM serie_realizada c WHERE c.corrige_a = s.uuid);
