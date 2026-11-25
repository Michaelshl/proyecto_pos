# POS: Sistema de Punto de Venta (API REST)

Backend de un sistema de punto de venta para comercio minorista. Proyecto de Desarrollo de Software II, Universidad del Valle (sede Yumbo). La interfaz usa por ahora el nombre de tienda de ejemplo **Allmarket** (provisional).

**Estado:** Sprint 3 completo. Sprints 1 y 2: autenticación, usuarios, roles, categorías, productos, inventario y clientes. Sprint 3: consulta y listado de clientes y el módulo de ventas (registro, detalle, anulación, consulta, listado y factura). El medio de pago es del Sprint 4. Es una API REST con una página de pruebas en `http://localhost:8081` (`index.html`); también se prueba con Postman, Swagger o `requests.http`.

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
| `GET /api/productos?categoria=&activo=&nombre=` | RF-015, RF-016 (filtros opcionales; `nombre` también busca por el comienzo del código). Orden: activos primero y, dentro de cada grupo, menor stock primero; los inactivos al final. **El cajero solo ve los productos activos**: el filtro `activo` se ignora para él, el autocompletado no los incluye y consultar un inactivo por código responde 404 | ADMIN y CAJERO |
| `GET /api/productos/sugerencias?q=` | Apoyo de RF-015 (autocompletado) | ADMIN y CAJERO |
| `PATCH /api/productos/{codigo}/inactivar` | RF-017 | ADMIN |
| `GET /api/productos/{codigo}/stock` | RF-018 | ADMIN |
| `POST /api/productos/{codigo}/ajustes` | RF-019 | ADMIN |
| `POST /api/clientes` | RF-020 | ADMIN y CAJERO |
| `PUT /api/clientes/{numeroDocumento}` | RF-021 | ADMIN y CAJERO |
| `GET /api/clientes?q=` | RF-023 (listar, con total) y RF-022 (buscar por documento, nombre o apellido; sin `q` lista todos) | ADMIN y CAJERO |
| `GET /api/clientes/{numeroDocumento}` | RF-022 (consulta por documento) | ADMIN y CAJERO |
| `POST /api/ventas` | RF-025: crea el **borrador** (número `B-000001`); cliente opcional. Sin cliente, o con un documento que no está registrado, es de Consumidor final (222222222222); en el segundo caso avisa «El cliente no se encuentra registrado» y no guarda ese documento. Con `notaPedido` la cédula es obligatoria (ver *Nota de pedido*) | ADMIN y CAJERO |
| `POST /api/ventas/{numero}/productos` | RF-026: agrega un producto al borrador (avisa si el stock no alcanza; un inactivo da 400) | ADMIN y CAJERO |
| `DELETE /api/ventas/{numero}/productos/{linea}` | RF-027: quita una línea del borrador | ADMIN y CAJERO |
| `POST /api/ventas/{numero}/registrar` | RF-025: registra la venta: descuenta el stock (todo o nada), le asigna el número de factura (`F-000001`) y borra el borrador | ADMIN y CAJERO |
| `DELETE /api/ventas/{numero}` | Cancela el borrador: no queda nada guardado | ADMIN y CAJERO |
| `GET /api/ventas/{numero}` | RF-030: detalle de una venta. El cajero solo ve las suyas y con los datos de contacto del cliente enmascarados | ADMIN y CAJERO |
| `GET /api/ventas?texto=&desde=&hasta=&producto=` | RF-031 y búsqueda: ventas registradas o anuladas por los últimos dígitos de la factura, el documento del cliente o la cédula de un contacto, por fechas o por producto. Trae las 200 más recientes y el total y los ingresos de TODAS las que cumplen. El administrador también filtra por `estado` y `cajero`; el cajero solo busca entre las suyas, en rangos de a lo más un año, y la lista no le dice quién es el cliente (documento enmascarado) | ADMIN y CAJERO |
| `GET /api/ventas/{numero}/factura` | RF-032: factura de una venta registrada o anulada (la anulada lo indica con su motivo); un borrador da 400. El cajero solo emite las suyas | ADMIN y CAJERO |
| `POST /api/ventas/{numero}/anular` | RF-029: anula una venta registrada con un `motivo` (5 a 500 caracteres) y devuelve el stock. No se puede anular dos veces | ADMIN |
| `GET /api/contactos-pedido/{documento}` | Consulta un contacto de pedido por su cédula (celular, correo y dirección enmascarados para el cajero) | ADMIN y CAJERO |
| `GET /api/clientes/sugerencias?q=` | Apoyo de RF-021 (autocompletado) | ADMIN y CAJERO |

Todos los errores responden con el mismo formato:

```json
{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "...", "path": "/api/..." }
```

**Ajuste de stock (RF-019).** El administrador no ajusta el stock "a su gusto": cada ajuste lleva un `tipoMotivo` de una lista cerrada y una observación (`motivo`, mínimo 5 caracteres), y el tipo debe ser coherente con el signo de la cantidad:

| `tipoMotivo` | Admite |
|---|---|
| `INGRESO_COMPRA`, `DEVOLUCION_CLIENTE` | Solo sumar (cantidad positiva) |
| `DANO`, `VENCIMIENTO`, `PERDIDA` | Solo restar (cantidad negativa) |
| `CONTEO_FISICO` | Sumar o restar |

Así, sacar unidades no se puede declarar como una compra. Además queda quién lo hizo y cuándo en `ajustes_stock`, y las salidas y los ajustes grandes generan una alerta (WARN) en el log de auditoría. Los ajustes anteriores a la `V4` quedan como `SIN_CLASIFICAR`.

**Ventas (Sprint 3).** Una venta empieza como **borrador** con su propio número (`B-000001`) y no toca el stock. Al **registrarla** recibe el número de factura (`F-000001`, así no quedan huecos por borradores abandonados), se descuenta el stock de todos sus productos en una sola transacción (si uno falla, no cambia nada) y el borrador desaparece. Un borrador que se cancela, o del que se sale de la pantalla (cerrar sesión, cerrar la página), se borra; si no llega la petición, el servidor borra todos al arrancar y, cada 10 minutos, los de más de 30 minutos. Sin cliente, la venta es de *Consumidor final* (documento `222222222222`, sin crear un cliente en la base). El cajero solo ve y modifica sus ventas; su vista de los datos de contacto del cliente (correo, y teléfono y dirección en una nota de pedido) va **enmascarada** desde el servidor (`an****es@correo.test`, `315*******`); solo el administrador los ve completos.

**Nota de pedido y contactos de pedido.** En una nota de pedido la **cédula es obligatoria**. Si es de un cliente registrado, la venta queda a su nombre. Si no, queda a nombre de un **contacto de pedido** (`contactos_pedido`, llave: la cédula): la primera vez se exigen celular, nombre, correo y dirección, y en los siguientes pedidos basta la cédula. **No** son clientes para facturación: la factura sale a *Consumidor final* y aparte muestra los datos de entrega. El cajero ve la cédula, el celular, el correo y la dirección enmascarados en la factura; solo el administrador los ve completos.

**Anulación y factura (RF-029 y RF-032).** Solo el administrador anula. La venta no se borra: pasa a `ANULADA` y guarda el motivo, la fecha y quién la anuló, y el stock de todos sus productos se devuelve en la misma transacción. Una venta anulada no se reactiva. La factura se ve y se imprime desde la pantalla; es un documento interno, **no** una factura electrónica ante la DIAN. Todavía no incluye medio de pago (Sprint 4) ni calcula impuestos.

**Búsqueda de ventas.** Para no revelar a quién pertenece una factura por parecerse un documento, la lista no muestra al cajero el nombre del cliente (solo el documento enmascarado): para confirmarlo abre la venta con «Ver». Un rango de fechas enorme no colapsa el sistema: la lista trae como máximo las 200 ventas más recientes, el total y los ingresos los suma la base de datos sobre todas las que cumplen, y al cajero no se le permite buscar en más de un año.

**Clientes (RF-020 y RF-021).** Tipos de documento: `CC`, `CE`, `TI`, `NIT`, `PP` y `PPT` (permiso por protección temporal). El correo y el teléfono son obligatorios al crear y al actualizar; la dirección es opcional. Los clientes que ya existían sin correo o teléfono conservan esos campos vacíos hasta que se actualicen, y ahí se les exige completarlos.

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
  venta/        Ventas: encabezado, detalle, consecutivos, contactos de pedido, anulación y factura
  security/     JWT, filtro, reglas por rol y tokens revocados
  exception/    Manejo global de errores
  config/       Carga inicial de roles y del primer administrador
  auditoria/    Registro de eventos de seguridad y acciones sensibles (log)
postman/        Colección de Postman
```

## Arquitectura

Monolito modular con **paquetes por funcionalidad**: cada módulo (`usuario`, `auth`, `rol`, `categoria`, `producto`, `cliente`, `venta`) agrupa su entidad, repositorio, servicio, controlador y `dto/`, y dentro sigue el orden controlador → servicio → repositorio. Lo transversal (`security`, `config`, `exception`) va aparte.

Regla de dependencias, sin ciclos (la flecha significa "usa a"):

```
auth ──► security ──► usuario ──► rol
  └──────────────────►┘
config ──► usuario, rol          categoria (independiente)
producto ──► categoria
venta ──► producto, cliente
```

Un módulo nuevo solo debe depender de otros módulos "hacia abajo", como `producto ──► categoria`. Nada puede depender de `auth`.

La interfaz (`index.html`) también se divide por módulos: una pestaña por cada uno (Usuarios, Productos, Clientes y Ventas con subpestañas, Roles, Categorías). Las de administrador se ocultan al cajero; en Productos el cajero solo ve Listado y Consultar, y Clientes la usan los dos roles.

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
| `consecutivos` | `tipo` (`FACTURA`, `BORRADOR`) |
| `ventas` | `numero_factura` (lo genera la aplicación) |
| `detalle_venta` | (`numero_factura`, `linea`): llave compuesta |
| `contactos_pedido` | `documento` (la cédula) |

`usuarios.rol_nombre` y `rol_permisos.rol_nombre` son llaves foráneas hacia `roles.nombre`.

## Repositorio

Ramas: `main` (código estable, entregado) y `develop` (trabajo en curso; cada funcionalidad nueva sale de ahí en una rama `feature/...`). Las entregas quedan marcadas con etiquetas: `sprint-1`, `sprint-2` y `sprint-3`.

## Migraciones de la base de datos

El esquema lo manejan scripts SQL versionados con [Flyway](https://flywaydb.org), en `src/main/resources/db/migration/`:

```
V1__esquema_inicial.sql      usuarios, roles, rol_permisos, categorias, tokens_revocados
V2__productos_y_clientes.sql productos, ajustes_stock, clientes
V3__clientes_razon_social.sql apellido opcional y nombre de hasta 150 caracteres (razón social de un NIT)
V4__ajustes_stock_tipo_motivo.sql  tipo de motivo (lista cerrada) en cada ajuste de stock
V5__ventas.sql                     consecutivos (números de factura), ventas y detalle_venta
V6__ventas_borradores_y_pedido.sql contador de borradores (B-000001) y marca de nota de pedido
V7__contactos_pedido.sql           contactos de pedido (quien pide sin ser cliente registrado)
V8__contactos_pedido_por_cedula.sql la cédula pasa a ser la llave de los contactos de pedido
V9__...                      (siguientes sprints)
```

Al arrancar, Flyway compara los scripts con la tabla `flyway_schema_history` de la base y ejecuta solo los que faltan, así que una base nueva o una antigua quedan al día sin borrar datos. Hibernate solo valida (`ddl-auto=validate`): si una entidad no coincide con la base, la aplicación no arranca y el mensaje dice qué columna falta.

Para cambiar el esquema:
1. Crear un archivo nuevo con el siguiente número, por ejemplo `V9__pagos.sql`. Guion bajo doble entre el número y la descripción.
2. **Nunca editar un script que ya se aplicó**: Flyway detecta el cambio y no deja arrancar. Una corrección va en un script nuevo.
3. Actualizar la entidad JPA y el diagrama (`docs/modelo-de-datos.md`).

Las pruebas automáticas usan H2 y arman el esquema desde las entidades, sin Flyway.

Si la base `pos_db` se creó antes de Flyway (con `ddl-auto=update`), hay que recrearla una sola vez: `DROP DATABASE pos_db; CREATE DATABASE pos_db;`.

## Logs

La aplicación escribe en la consola y en `logs/pos.log` (la carpeta `logs/` no se sube a Git). El archivo rota al llegar a 10 MB, conserva 14 días y no pasa de 100 MB en total. Las consultas SQL no se imprimen; para depurar se arranca con la variable de entorno `SHOW_SQL=true`.

Los eventos de seguridad y las acciones sensibles salen con el nombre `AUDITORIA`, con quién, qué y sobre qué (nunca contraseñas ni tokens):

| Evento | Nivel | Ejemplo |
|---|---|---|
| Login exitoso / fallido / rechazado por usuario inactivo | INFO / WARN / WARN | `Login fallido: cédula '1000000002'` |
| Acceso sin sesión válida (401) y acceso denegado (403) | WARN | `Acceso denegado (403): usuario 1000000002, POST /api/productos desde 127.0.0.1` |
| Cierre de sesión | INFO | `Cierre de sesión por 1000000002: sesión terminada` |
| Crear usuario, rol, categoría, producto o cliente; actualizar usuario, producto o cliente; asignar rol; inactivar usuario o producto; ajustar stock | INFO | `Ajuste de stock por 1000000001: producto 100001, cantidad -5, stock nuevo 5, tipo VENCIMIENTO, motivo: Producto vencido` |
| Iniciar, cancelar o registrar una venta; agregar o quitar un producto; registrar un contacto de pedido | INFO | `Venta registrada por 1000000002: factura F-000001, total 13500.00, 1 línea(s)` |
| Anular una venta | WARN | `Venta anulada por 1000000001: factura F-000001, total 13500.00, stock devuelto de 1 línea(s), motivo: ...` |
| Borradores de venta que nadie terminó, borrados por el servidor | INFO | `Borradores de venta sin terminar borrados: 1 borrador(es)` |
| Ajuste de stock que es una salida (cantidad negativa) o es grande (más del 20 % del stock o 50 unidades o más) | WARN | `ALERTA ajuste de stock (salida): usuario 1000000001, producto 100001, cantidad -5, ...` |

Lo que escribe el cliente (una cédula en el login, una ruta) se recorta y se le quitan los saltos de línea, para que nadie pueda inventar líneas falsas en el log.

## Deuda técnica

Mejoras de seguridad y calidad conocidas que se dejaron para más adelante (el proyecto no es comercial):

- **Usuario de MySQL con permisos mínimos.** La aplicación se conecta con `root` por defecto (`DB_USER`). Lo recomendable es crear un usuario `pos_app` con permisos solo sobre `pos_db` (`SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP, REFERENCES`; los de estructura los necesita Flyway). Opcional en desarrollo local, obligatorio si algún día se despliega en un servidor.
- Límite de intentos (rate limiting) en el login.
- CORS sin configurar y Swagger accesible sin autenticación.
- Aviso de `open-in-view` al arrancar.
- **Controles del ajuste de stock que se podrían agregar:** (a) un **límite por ajuste** que rechace los ajustes muy grandes en vez de solo alertar, y (b) **aprobación de un segundo usuario**: el administrador solicita el ajuste y otra persona lo aprueba (requiere una tabla de solicitudes y su pantalla). Hoy el control es la lista cerrada de motivos, la coherencia con el signo, el historial en `ajustes_stock` y las alertas en el log.
- **Auditoría solo en el log:** las acciones sensibles quedan en el archivo de log, no en una tabla de la base; el log rota y se borra con el tiempo, así que no sirve como historial permanente. Tampoco se registran las consultas (lecturas).
- `@Data` de Lombok en la entidad `Usuario`.
- Los permisos de `rol_permisos` todavía no gobiernan el acceso: las rutas se protegen por rol en `SecurityConfig`.
- No existe una función para reactivar un usuario inactivo (no está en los requisitos).
- Las ventas no calculan impuestos: el texto del RF-025 menciona subtotal, impuesto y total, y hoy la venta maneja subtotal por línea y total. El medio de pago (RF-025 y RF-032) llega con los pagos del Sprint 4.
- Historial de Git: un commit de mayo de 2026 incluyó una contraseña de MySQL y un `JWT_SECRET` en `application.properties`. Ya se movieron a variables de entorno y se rotaron las claves, por lo que las del historial no sirven; el historial no se reescribió.
