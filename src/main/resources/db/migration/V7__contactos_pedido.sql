-- V7: contactos de pedido (Sprint 3). Quien hace una nota de pedido y NO es un cliente registrado deja su celular,
-- nombre, correo y dirección aquí, para poder entregarle el pedido y reconocerlo en los próximos. No son clientes
-- para facturación: `clientes` exige documento y esta tabla no. La llave es el celular (dato propio de la persona).
-- Una migración ya aplicada NO se edita: la V6 ya corrió, por eso estos cambios van en un archivo nuevo.

CREATE TABLE contactos_pedido (
    telefono  VARCHAR(20)  NOT NULL,
    nombre    VARCHAR(150) NOT NULL,
    correo    VARCHAR(150) NOT NULL,
    direccion VARCHAR(255) NOT NULL,
    CONSTRAINT pk_contactos_pedido PRIMARY KEY (telefono)
) ENGINE = InnoDB;

-- La nota de pedido de un contacto (sin cliente registrado) apunta a su celular.
ALTER TABLE ventas
    ADD COLUMN contacto_telefono VARCHAR(20) NULL AFTER es_pedido,
    ADD CONSTRAINT fk_ventas_contacto FOREIGN KEY (contacto_telefono) REFERENCES contactos_pedido (telefono);
