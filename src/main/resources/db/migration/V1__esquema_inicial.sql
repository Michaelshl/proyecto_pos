-- V1: esquema del Sprint 1 (usuarios, roles, categorías y tokens revocados).
-- Todas las llaves primarias son datos propios de la entidad; ninguna es autoincremental.
-- Una migración ya aplicada NO se edita: cualquier cambio va en un archivo V2, V3...

CREATE TABLE roles (
    nombre      VARCHAR(50)  NOT NULL,
    descripcion VARCHAR(255),
    CONSTRAINT pk_roles PRIMARY KEY (nombre)
) ENGINE = InnoDB;

CREATE TABLE rol_permisos (
    rol_nombre VARCHAR(50)  NOT NULL,
    permiso    VARCHAR(255) NOT NULL,
    CONSTRAINT pk_rol_permisos PRIMARY KEY (rol_nombre, permiso),
    CONSTRAINT fk_rol_permisos_rol FOREIGN KEY (rol_nombre) REFERENCES roles (nombre)
) ENGINE = InnoDB;

CREATE TABLE usuarios (
    cedula              VARCHAR(20)  NOT NULL,
    nombre              VARCHAR(255),
    apellido            VARCHAR(255),
    email               VARCHAR(255) NOT NULL,
    password            VARCHAR(255) NOT NULL,
    activo              TINYINT(1)   NOT NULL DEFAULT 1,
    motivo_inactivacion VARCHAR(500),
    rol_nombre          VARCHAR(50),
    CONSTRAINT pk_usuarios PRIMARY KEY (cedula),
    CONSTRAINT uk_usuarios_email UNIQUE (email),
    CONSTRAINT fk_usuarios_rol FOREIGN KEY (rol_nombre) REFERENCES roles (nombre)
) ENGINE = InnoDB;

CREATE TABLE categorias (
    nombre      VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255),
    CONSTRAINT pk_categorias PRIMARY KEY (nombre)
) ENGINE = InnoDB;

CREATE TABLE tokens_revocados (
    jti        VARCHAR(255) NOT NULL,
    expiracion DATETIME(6)  NOT NULL,
    CONSTRAINT pk_tokens_revocados PRIMARY KEY (jti)
) ENGINE = InnoDB;
