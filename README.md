# POS: Sistema de Punto de Venta (API REST)

Backend de un sistema de punto de venta para comercio minorista. Proyecto de Desarrollo de Software II, Universidad del Valle (sede Yumbo). La interfaz usa por ahora el nombre de tienda de ejemplo **Allmarket** (provisional).

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

Las tablas las crea Flyway al arrancar (ver *Migraciones de la base de datos*), y los roles `ADMIN` y `CAJERO` también.

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

`POST /api/auth/login` (cédula y contraseña) devuelve `{ "token": "...", "nombre": "Nombre Apellido" }`; el nombre lo usa la interfaz para el encabezado ("Conectado como ..."). El JWT dura 2 horas. Se envía en cada petición:

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
| `GET /api/usuarios/sugerencias?q=` | Apoyo de RF-005, 006, 007 y 009 (autocompletado) | ADMIN |
| `PATCH /api/usuarios/{cedula}/inactivar` | RF-007 | ADMIN |
| `PATCH /api/usuarios/{cedula}/rol` | RF-009 | ADMIN |
| `POST /api/roles` | RF-008 | ADMIN |
| `POST /api/categorias` | RF-010 | ADMIN |
| `GET /api/categorias` | RF-012 | ADMIN y CAJERO |
| `POST /api/productos` | RF-013 | ADMIN |
| `PUT /api/productos/{codigo}` | RF-014 | ADMIN |
| `GET /api/productos/{codigo}` | RF-015 | ADMIN y CAJERO |
| `GET /api/productos?categoria=&activo=&nombre=` | RF-015, RF-016 (filtros opcionales) | ADMIN y CAJERO |
| `GET /api/productos/sugerencias?q=` | Apoyo de RF-015 (autocompletado) | ADMIN y CAJERO |
| `PATCH /api/productos/{codigo}/inactivar` | RF-017 | ADMIN |
| `GET /api/productos/{codigo}/stock` | RF-018 | ADMIN |
| `POST /api/productos/{codigo}/ajustes` | RF-019 | ADMIN |
| `POST /api/clientes` | RF-020 | ADMIN y CAJERO |
| `PUT /api/clientes/{numeroDocumento}` | RF-021 | ADMIN y CAJERO |
| `GET /api/clientes/{numeroDocumento}` | Apoyo de RF-021; base del RF-022 (Sprint 3) | ADMIN y CAJERO |
| `GET /api/clientes/sugerencias?q=` | Apoyo de RF-021 (autocompletado) | ADMIN y CAJERO |

Todos los errores responden con el mismo formato:

```json
{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "...", "path": "/api/..." }
```

Un token ausente, inválido, vencido o revocado responde 401; un rol sin permiso, 403. Un ajuste de stock que choca con otra operación sobre el mismo producto responde 409 (bloqueo optimista con `@Version`): basta con repetirlo.

## Estructura

```
src/main/java/com/pos/
  usuario/      Usuarios (entidad, repositorio, servicio, controlador y dto/)
  auth/         Login y logout
  rol/          Roles y sus permisos
  categoria/    Categorías
  producto/     Productos, inventario y ajustes de stock
  cliente/      Clientes
  security/     JWT, filtro, reglas por rol y tokens revocados
  exception/    Manejo global de errores
  config/       Carga inicial de roles y del primer administrador
postman/        Colección de Postman
```

## Arquitectura

Monolito modular con **paquetes por funcionalidad**: cada módulo (`usuario`, `auth`, `rol`, `categoria`, `producto`, `cliente`) agrupa su entidad, repositorio, servicio, controlador y `dto/`, y dentro sigue el orden controlador → servicio → repositorio. Lo transversal (`security`, `config`, `exception`) va aparte.

Regla de dependencias, sin ciclos (la flecha significa "usa a"):

```
auth ──► security ──► usuario ──► rol
  └──────────────────►┘
config ──► usuario, rol          categoria (independiente)
producto ──► categoria
```

Un módulo nuevo solo debe depender de otros módulos "hacia abajo", como `producto ──► categoria`. Nada puede depender de `auth`.

La interfaz (`index.html`) también se divide por módulos: una pestaña por cada uno (Usuarios, Productos y Clientes con subpestañas, Roles, Categorías). Las de administrador se ocultan al cajero; en Productos el cajero solo ve Listado y Consultar, y Clientes la usan los dos roles.

## Llaves primarias

Ninguna tabla usa llaves autogeneradas; cada una usa un dato propio de la entidad:

| Tabla | Llave primaria |
|---|---|
| `usuarios` | `cedula` |
| `roles` | `nombre` (en mayúsculas, ej. `ADMIN`) |
| `categorias` | `nombre` |
| `tokens_revocados` | `jti` |
| `productos` | `codigo` (SKU numérico de 4 a 14 dígitos, guardado como texto para no perder ceros) |
| `ajustes_stock` | (`producto_codigo`, `consecutivo`): llave compuesta |
| `clientes` | `numero_documento` (el `tipo_documento` es una columna normal). Con NIT, `nombre` es la razón social y no hay apellido |

`usuarios.rol_nombre` y `rol_permisos.rol_nombre` son llaves foráneas hacia `roles.nombre`.

## Repositorio

Ramas: `main` (código estable, entregado) y `develop` (trabajo en curso; cada funcionalidad nueva sale de ahí en una rama `feature/...`). La entrega del Sprint 1 quedó marcada con la etiqueta `sprint-1`.

## Migraciones de la base de datos

El esquema lo manejan scripts SQL versionados con [Flyway](https://flywaydb.org), en `src/main/resources/db/migration/`:

```
V1__esquema_inicial.sql      usuarios, roles, rol_permisos, categorias, tokens_revocados
V2__productos_y_clientes.sql productos, ajustes_stock, clientes
V3__clientes_razon_social.sql apellido opcional y nombre de hasta 150 caracteres (razón social de un NIT)
V4__...                      (siguientes sprints)
```

Al arrancar, Flyway compara los scripts con la tabla `flyway_schema_history` de la base y ejecuta solo los que faltan, así que una base nueva o una antigua quedan al día sin borrar datos. Hibernate solo valida (`ddl-auto=validate`): si una entidad no coincide con la base, la aplicación no arranca y el mensaje dice qué columna falta.

Para cambiar el esquema:
1. Crear un archivo nuevo con el siguiente número, por ejemplo `V4__ventas.sql`. Guion bajo doble entre el número y la descripción.
2. **Nunca editar un script que ya se aplicó**: Flyway detecta el cambio y no deja arrancar. Una corrección va en un script nuevo.
3. Actualizar la entidad JPA y el diagrama (`docs/modelo-de-datos.md`).

Las pruebas automáticas usan H2 y arman el esquema desde las entidades, sin Flyway.

Si la base `pos_db` se creó antes de Flyway (con `ddl-auto=update`), hay que recrearla una sola vez: `DROP DATABASE pos_db; CREATE DATABASE pos_db;`.

## Deuda técnica

Mejoras de seguridad y calidad conocidas que se dejaron para más adelante (el proyecto no es comercial):

- **Usuario de MySQL con permisos mínimos.** La aplicación se conecta con `root` por defecto (`DB_USER`). Lo recomendable es crear un usuario `pos_app` con permisos solo sobre `pos_db` (`SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP, REFERENCES`; los de estructura los necesita Flyway). Opcional en desarrollo local, obligatorio si algún día se despliega en un servidor.
- Límite de intentos (rate limiting) en el login.
- CORS sin configurar y Swagger accesible sin autenticación.
- `spring.jpa.show-sql` activo y aviso de `open-in-view` al arrancar.
- `@Data` de Lombok en la entidad `Usuario`.
- Los permisos de `rol_permisos` todavía no gobiernan el acceso: las rutas se protegen por rol en `SecurityConfig`.
- No existe una función para reactivar un usuario inactivo (no está en los requisitos).
- Historial de Git: un commit de mayo de 2026 incluyó una contraseña de MySQL y un `JWT_SECRET` en `application.properties`. Ya se movieron a variables de entorno y se rotaron las claves, por lo que las del historial no sirven; el historial no se reescribió.
