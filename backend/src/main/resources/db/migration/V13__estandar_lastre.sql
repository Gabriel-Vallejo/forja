-- Dominada y fondo con lastre (OpenStreetlifting, datos CC0): su estándar es el lastre añadido, no la carga total.
ALTER TABLE estandar_fuerza
    MODIFY levantamiento ENUM ('sentadilla', 'banca', 'peso_muerto', 'dominada_lastre', 'fondo_lastre') NOT NULL,
    ADD COLUMN metrica ENUM ('kg_1rm', 'kg_lastre') NOT NULL DEFAULT 'kg_1rm' AFTER levantamiento;
