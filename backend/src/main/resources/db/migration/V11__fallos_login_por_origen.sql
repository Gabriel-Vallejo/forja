-- Los fallos de contraseña pasan a contarse por cuenta y origen, en memoria (FallosLogin, ADR 0009 §6 enmienda
-- 2026-11-22): la espera por cuenta dejaba a cualquiera bloquear la de otro hasta 1 h.
ALTER TABLE usuario
    DROP COLUMN fallos_seguidos,
    DROP COLUMN bloqueado_hasta;
