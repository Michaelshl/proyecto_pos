# POS: Sistema de Punto de Venta (API REST)

Backend de un sistema de punto de venta para comercio minorista. Proyecto de Desarrollo de Software II, Universidad del Valle (sede Yumbo).

**Estado:** Sprint 1 completo (autenticación, usuarios, roles y categorías). Es una API REST con una página de pruebas en `http://localhost:8081` (`index.html`); también se prueba con Postman, Swagger o `requests.http`.

## Tecnologías

Java 17 · Spring Boot 3.5 · Spring Security 6 · Spring Data JPA (Hibernate) · JWT (jjwt 0.12.6) · MySQL 8 · Lombok · springdoc-openapi (Swagger UI)

## Requisitos

- JDK 17 o superior
- MySQL 8 o superior, en ejecución
- No hace falta instalar Maven: el proyecto incluye `mvnw`

## Instalación

**1. Crear la base de datos vacía**

```sql
CREATE DATABASE pos_db;
```

Las tablas se crean solas al arrancar (`ddl-auto=update`), y los roles `ADMIN` y `CAJERO` también.

**2. Configurar los secretos**

Copiar `.env.example` como `.env` (en la raíz del proyecto) y completarlo:

```
DB_PASSWORD=contraseña_de_tu_mysql
JWT_SECRET=una_clave_aleatoria_de_minimo_32_caracteres
ADMIN_CEDULA=cedula_del_primer_administrador
ADMIN_PASSWORD=contraseña_del_primer_administrador
```

Opcionalmente `DB_USER` (por defecto `root`), `DB_URL` (por defecto `jdbc:mysql://localhost:3306/pos_db`), `ADMIN_EMAIL` (por defecto `admin@pos.com`). También se pueden definir como variables de entorno del sistema. El archivo `.env` no se sube a Git.

Si al arrancar aparece `Access denied for user 'root'`, falta el `.env` o no está en la carpeta desde donde se ejecuta la aplicación.

**3. Arrancar**

```bash
./mvnw spring-boot:run
```

Está lista cuando el log muestra `Started PosApplication`. Queda en `http://localhost:8081`.

**4. Iniciar sesión como administrador**

Al arrancar con la base vacía, la aplicación crea sola el primer administrador: cédula `ADMIN_CEDULA` y contraseña `ADMIN_PASSWORD`. Solo lo hace si no existe ningún usuario; si ya hay usuarios, o si `ADMIN_CEDULA` o `ADMIN_PASSWORD` no están definidas, no crea nada (en ese caso el log lo avisa). La contraseña se guarda cifrada y no aparece en el log.

Desde ahí, los demás usuarios se crean por la API.

## Cómo probar

- **Postman:** importar `postman/POS_API.postman_collection.json` y ejecutar primero *Login admin*, que guarda el token para las demás peticiones. Antes de ejecutarla, completar las variables de colección `adminCedula` y `adminPassword` con las del primer administrador (`ADMIN_CEDULA` / `ADMIN_PASSWORD` del `.env`).
- **IntelliJ:** abrir `src/test/requests.http` y ejecutar cada bloque (el de Login guarda el token).
- **Swagger UI:** `http://localhost:8081/swagger-ui/index.html`. Hacer login, copiar el token y pegarlo en *Authorize*.

## Autenticación

`POST /api/auth/login` devuelve un JWT que dura 2 horas. Se envía en cada petición:

```
Authorization: Bearer <token>
```

`POST /api/auth/logout` revoca el token: deja de servir aunque no haya vencido.

## Endpoints

| Método y ruta | RF | Acceso |
|---|---|---|
| `POST /api/auth/login` | RF-001 | Público |
| `POST /api/auth/logout` | RF-002 | Usuario con token |
| `GET /api/usuarios` | | ADMIN |
| `POST /api/usuarios` | RF-004 | ADMIN |
| `PUT /api/usuarios/{cedula}` | RF-005 | ADMIN |
| `GET /api/usuarios/buscar?cedula=` o `?email=` | RF-006 | ADMIN |
| `PATCH /api/usuarios/{cedula}/inactivar` | RF-007 | ADMIN |
| `PATCH /api/usuarios/{cedula}/rol` | RF-009 | ADMIN |
| `POST /api/roles` | RF-008 | ADMIN |
| `POST /api/categorias` | RF-010 | ADMIN |
| `GET /api/categorias` | RF-012 | ADMIN y CAJERO |

Todos los errores responden con el mismo formato:

```json
{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "...", "path": "/api/..." }
```

Un token ausente, inválido, vencido o revocado responde 401; un rol sin permiso, 403.

## Estructura

```
src/main/java/com/pos/
  usuario/      Usuarios (entidad, repositorio, servicio, controlador y dto/)
  auth/         Login y logout
  rol/          Roles y sus permisos
  categoria/    Categorías
  security/     JWT, filtro, reglas por rol y tokens revocados
  exception/    Manejo global de errores
  config/       Carga inicial de roles y del primer administrador
postman/        Colección de Postman
```

## Arquitectura

Monolito modular con **paquetes por funcionalidad**: cada módulo (`usuario`, `auth`, `rol`, `categoria`) agrupa su entidad, repositorio, servicio, controlador y `dto/`, y dentro sigue el orden controlador → servicio → repositorio. Lo transversal (`security`, `config`, `exception`) va aparte.

Regla de dependencias, sin ciclos (la flecha significa "usa a"):

```
auth ──► security ──► usuario ──► rol
  └──────────────────►┘
config ──► usuario, rol          categoria (independiente)
```

Un módulo nuevo (por ejemplo `producto`) solo debe depender de otros módulos "hacia abajo": `producto ──► categoria`. Nada puede depender de `auth`.

La interfaz (`index.html`) también se divide por módulos: una pestaña por cada uno (Usuarios con subpestañas, Roles, Categorías) y las de administrador se ocultan al cajero.

## Llaves primarias

Ninguna tabla usa llaves autogeneradas; cada una usa un dato propio de la entidad:

| Tabla | Llave primaria |
|---|---|
| `usuarios` | `cedula` |
| `roles` | `nombre` (en mayúsculas, ej. `ADMIN`) |
| `categorias` | `nombre` |
| `tokens_revocados` | `jti` |

`usuarios.rol_nombre` y `rol_permisos.rol_nombre` son llaves foráneas hacia `roles.nombre`. Si la base ya existía con el esquema anterior hay que recrearla (`DROP DATABASE pos_db; CREATE DATABASE pos_db;`), porque `ddl-auto=update` no cambia llaves primarias.

## Repositorio

Ramas: `main` (código base) y `primer-entrega` (trabajo del Sprint 1).
