-- V4: el ajuste de stock ahora lleva un tipo de motivo de una lista cerrada (INGRESO_COMPRA, DEVOLUCION_CLIENTE,
-- DANO, VENCIMIENTO, PERDIDA, CONTEO_FISICO). El texto libre de `motivo` pasa a ser la observación.
-- Los ajustes que ya existían se marcan SIN_CLASIFICAR: no se inventa un tipo que nadie eligió.
-- Una migración ya aplicada NO se edita: por eso este cambio va en un archivo nuevo.

ALTER TABLE ajustes_stock ADD COLUMN tipo_motivo VARCHAR(30) NOT NULL DEFAULT 'SIN_CLASIFICAR' AFTER cantidad;
ALTER TABLE ajustes_stock ALTER COLUMN tipo_motivo DROP DEFAULT;
