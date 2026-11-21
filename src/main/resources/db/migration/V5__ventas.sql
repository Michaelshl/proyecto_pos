-- V5: módulo de ventas (Sprint 3): consecutivos, ventas y detalle_venta.
-- Diseño según docs/modelo-de-datos.md, con dos columnas que no estaban en el modelo aprobado en `ventas`
-- (fecha_anulacion y anulado_por: saber cuándo y quién anuló). El descuento (Sprint 5) y el pago (Sprint 4)
-- se agregan en sus sprints.
-- Una migración ya aplicada NO se edita: los cambios siguientes van en un archivo nuevo.

-- Contador de números de factura. La aplicación lee la fila con bloqueo, la aumenta y arma el número
-- (por ejemplo F-000001) dentro de la misma transacción, para que dos cajeros no obtengan el mismo número.
CREATE TABLE consecutivos (
    tipo         VARCHAR(20) NOT NULL,
    ultimo_valor BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_consecutivos PRIMARY KEY (tipo)
) ENGINE = InnoDB;

INSERT INTO consecutivos (tipo, ultimo_valor) VALUES ('FACTURA', 0);

-- estado: EN_PROCESO (borrador, no toca el stock), REGISTRADA o ANULADA (se valida en la aplicación).
-- cliente_documento es opcional: una venta puede hacerse sin cliente.
CREATE TABLE ventas (
    numero_factura    VARCHAR(20)    NOT NULL,
    fecha             DATETIME(6)    NOT NULL,
    estado            VARCHAR(20)    NOT NULL,
    total             DECIMAL(12, 2) NOT NULL DEFAULT 0,
    motivo_anulacion  VARCHAR(500),
    fecha_anulacion   DATETIME(6),
    anulado_por       VARCHAR(20),
    cajero_cedula     VARCHAR(20)    NOT NULL,
    cliente_documento VARCHAR(20),
    CONSTRAINT pk_ventas PRIMARY KEY (numero_factura),
    CONSTRAINT ck_ventas_total CHECK (total >= 0),
    CONSTRAINT fk_ventas_cajero FOREIGN KEY (cajero_cedula) REFERENCES usuarios (cedula),
    CONSTRAINT fk_ventas_cliente FOREIGN KEY (cliente_documento) REFERENCES clientes (numero_documento),
    CONSTRAINT fk_ventas_anulado_por FOREIGN KEY (anulado_por) REFERENCES usuarios (cedula)
) ENGINE = InnoDB;

-- Para listar ventas por rango de fechas y por estado (RF-031).
CREATE INDEX idx_ventas_fecha_estado ON ventas (fecha, estado);

-- Una línea por producto de la venta. precio_unitario guarda el precio del momento de la venta: si el
-- producto cambia de precio después, la venta anterior no se altera. El subtotal no se guarda: es
-- cantidad x precio_unitario.
CREATE TABLE detalle_venta (
    numero_factura  VARCHAR(20)    NOT NULL,
    linea           INT            NOT NULL,
    producto_codigo VARCHAR(30)    NOT NULL,
    cantidad        INT            NOT NULL,
    precio_unitario DECIMAL(12, 2) NOT NULL,
    CONSTRAINT pk_detalle_venta PRIMARY KEY (numero_factura, linea),
    CONSTRAINT ck_detalle_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_detalle_precio CHECK (precio_unitario >= 0),
    CONSTRAINT fk_detalle_venta FOREIGN KEY (numero_factura) REFERENCES ventas (numero_factura),
    CONSTRAINT fk_detalle_producto FOREIGN KEY (producto_codigo) REFERENCES productos (codigo)
) ENGINE = InnoDB;
