-- V2: Sprint 2 (productos, ajustes de stock y clientes).
-- Todas las llaves primarias son datos propios de la entidad; ninguna es autoincremental.
-- Una migración ya aplicada NO se edita: cualquier cambio va en un archivo V3, V4...

CREATE TABLE productos (
    codigo              VARCHAR(30)    NOT NULL,
    nombre              VARCHAR(150)   NOT NULL,
    descripcion         VARCHAR(255),
    precio_venta        DECIMAL(12, 2) NOT NULL,
    stock               INT            NOT NULL DEFAULT 0,
    stock_minimo        INT            NOT NULL DEFAULT 0,
    activo              TINYINT(1)     NOT NULL DEFAULT 1,
    motivo_inactivacion VARCHAR(500),
    version             BIGINT         NOT NULL DEFAULT 0,
    categoria_nombre    VARCHAR(100)   NOT NULL,
    CONSTRAINT pk_productos PRIMARY KEY (codigo),
    CONSTRAINT fk_productos_categoria FOREIGN KEY (categoria_nombre)
        REFERENCES categorias (nombre) ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE TABLE ajustes_stock (
    producto_codigo VARCHAR(30)  NOT NULL,
    consecutivo     INT          NOT NULL,
    usuario_cedula  VARCHAR(20)  NOT NULL,
    cantidad        INT          NOT NULL,
    motivo          VARCHAR(255) NOT NULL,
    fecha           DATETIME(6)  NOT NULL,
    CONSTRAINT pk_ajustes_stock PRIMARY KEY (producto_codigo, consecutivo),
    CONSTRAINT fk_ajustes_stock_producto FOREIGN KEY (producto_codigo) REFERENCES productos (codigo),
    CONSTRAINT fk_ajustes_stock_usuario FOREIGN KEY (usuario_cedula) REFERENCES usuarios (cedula)
) ENGINE = InnoDB;

CREATE TABLE clientes (
    numero_documento VARCHAR(20)  NOT NULL,
    tipo_documento   VARCHAR(20)  NOT NULL,
    nombre           VARCHAR(100) NOT NULL,
    apellido         VARCHAR(100) NOT NULL,
    email            VARCHAR(150),
    telefono         VARCHAR(20),
    direccion        VARCHAR(255),
    CONSTRAINT pk_clientes PRIMARY KEY (numero_documento)
) ENGINE = InnoDB;
