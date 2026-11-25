-- V8: la cédula pasa a ser la llave de los contactos de pedido. En una nota de pedido la cédula es obligatoria y se
-- guarda para reconocer a la persona en los próximos pedidos (el celular puede cambiar). Los contactos siguen
-- separados de `clientes`: no son clientes para facturación.
-- Los contactos anteriores no tenían cédula, así que la tabla debe estar vacía al aplicar esta migración; en pos_db
-- el único que había era de prueba y se retiró aparte, con respaldo. Una migración ya aplicada NO se edita.

ALTER TABLE ventas DROP FOREIGN KEY fk_ventas_contacto;

ALTER TABLE ventas
    CHANGE COLUMN contacto_telefono contacto_documento VARCHAR(20) NULL;

ALTER TABLE contactos_pedido
    ADD COLUMN documento VARCHAR(20) NOT NULL FIRST,
    DROP PRIMARY KEY,
    ADD CONSTRAINT pk_contactos_pedido PRIMARY KEY (documento);

ALTER TABLE ventas
    ADD CONSTRAINT fk_ventas_contacto FOREIGN KEY (contacto_documento) REFERENCES contactos_pedido (documento);
