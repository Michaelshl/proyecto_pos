-- V3: un cliente con NIT es una empresa: tiene razón social y no tiene apellido.
-- La razón social se guarda en la columna `nombre` (ahora hasta 150 caracteres) y el apellido pasa a ser opcional.
-- Una migración ya aplicada NO se edita: por eso este cambio va en un archivo nuevo y no en V2.

ALTER TABLE clientes MODIFY nombre VARCHAR(150) NOT NULL;
ALTER TABLE clientes MODIFY apellido VARCHAR(100) NULL;

-- Los clientes NIT que ya existían con nombre y apellido (por ejemplo "Tienda" + "El Sol") pasan a una sola
-- razón social ("Tienda El Sol"). MySQL evalúa las asignaciones de izquierda a derecha, así que `nombre`
-- todavía ve el apellido antiguo cuando se calcula; CONCAT_WS ignora el apellido si ya era NULL.
UPDATE clientes
   SET nombre = CONCAT_WS(' ', nombre, apellido),
       apellido = NULL
 WHERE tipo_documento = 'NIT';
