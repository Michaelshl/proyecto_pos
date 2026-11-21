-- V6: borradores de venta con su propia numeración y marca de nota de pedido (Sprint 3).
-- El número de factura (F-000001) solo se consume al REGISTRAR la venta, para no dejar huecos en la numeración.
-- Mientras la venta es un borrador se identifica con un número propio (B-000001), tomado de este contador.
-- Una migración ya aplicada NO se edita: la V5 ya corrió, por eso estos cambios van en un archivo nuevo.

INSERT INTO consecutivos (tipo, ultimo_valor) VALUES ('BORRADOR', 0);

-- Una nota de pedido es una venta que se entrega o se despacha: el cliente debe estar registrado y se muestran
-- sus datos de contacto (enmascarados para el cajero).
ALTER TABLE ventas ADD COLUMN es_pedido TINYINT(1) NOT NULL DEFAULT 0 AFTER cliente_documento;
