# Verificación del Sprint 2

Guía para comprobar a mano que los requisitos del Sprint 2 funcionan. Se hizo una verificación automática el 2026-10-11 (resultados al final); esta guía es para repetirla en pantalla.

## Qué se implementó

| RF | Qué hace | Quién | Dónde se prueba |
|---|---|---|---|
| RF-013 | Crear producto (código único) | Administrador | Productos → Crear |
| RF-014 | Actualizar producto | Administrador | Productos → Actualizar |
| RF-015 | Consultar producto por código o nombre | Cajero y administrador | Productos → Consultar |
| RF-016 | Listar productos con filtros de categoría y estado | Cajero y administrador | Productos → Listado |
| RF-017 | Inactivar producto con motivo | Administrador | Productos → Inactivar |
| RF-018 | Consultar stock con alerta de stock mínimo | Administrador | Productos → Stock |
| RF-019 | Ajustar stock con motivo (queda en el historial) | Administrador | Productos → Stock |
| RF-020 | Crear cliente (documento único) | Cajero y administrador | Clientes → Crear |
| RF-021 | Actualizar cliente | Cajero y administrador | Clientes → Actualizar |

Además: autocompletado en Consultar, Actualizar, Inactivar y Stock (productos) y en Consultar y Actualizar (clientes), y ventanas de confirmación al actualizar, inactivar y ajustar stock.

## 0. Preparación

1. Arrancar la aplicación como siempre (puerto 8081). Flyway crea las tablas nuevas solo: en el log debe aparecer un mensaje de Flyway migrando a la versión 2 (`productos y clientes`). Los datos de `pos_db` no se tocan: la migración solo agrega las tablas nuevas.
2. Entrar a <http://localhost:8081> como administrador.
3. Si no existe un usuario cajero, crearlo en Usuarios → Crear (rol `CAJERO`).
4. Si prefieres no usar `pos_db`: arrancar con las variables de entorno `DB_URL=jdbc:mysql://localhost:3306/<otra_base>?createDatabaseIfNotExist=true`, `SERVER_PORT=8090` y un `ADMIN_CEDULA`/`ADMIN_PASSWORD` de prueba. **Esa base de prueba hay que borrarla después** (ver el último apartado).

## 1. Como administrador: productos

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | Categorías → crear `Bebidas` (si no existe) | Categoría creada |
| 2 | Productos → Crear: código `100001`, nombre `Coca-Cola 1.5L`, precio `5500`, stock inicial `10`, stock mínimo `5`, categoría `Bebidas` | "Producto 100001 creado"; el formulario se limpia |
| 3 | Repetir el mismo código `100001` | Error "Ya existe un producto con ese código" |
| 3b | En el campo del código intentar escribir `BEB-001` | Solo quedan los dígitos (`001`): el código es un SKU numérico de 4 a 14 dígitos. Por API, un código con letras o fuera de ese largo responde 400 con "El código (SKU) debe tener entre 4 y 14 dígitos, solo números" |
| 4 | Crear dejando el precio vacío | "Complete el precio, el stock inicial y el stock mínimo" |
| 5 | Listado → Buscar | Aparece `100001` con precio `$ 5.500` y stock 10 |
| 6 | Listado → filtrar por categoría `Bebidas`, estado `Activos`, o nombre `cola` | La lista se filtra y dice cuántos productos hay |
| 7 | Consultar → escribir `col` → elegir `100001 · Coca-Cola 1.5L` de la lista (mouse o flechas + Enter) | Ficha completa con precio, stock disponible y estado |
| 8 | Actualizar → elegir `100001`, cambiar el precio a `5600` → Actualizar | La ventana muestra solo `Precio: $ 5.500 → $ 5.600`; al aceptar, "Producto actualizado" |
| 9 | Actualizar sin cambiar nada | "No hay cambios para guardar" |
| 10 | Stock → elegir `100001` → Consultar stock | "stock actual 10, stock mínimo 5" |
| 11 | Stock → ajuste `-5`, motivo `Producto vencido` → Ajustar | La ventana muestra stock actual 10, ajuste -5, stock nuevo 5; al aceptar: "Stock actualizado: 5 (ALERTA: igual o menor al mínimo de 5)" |
| 12 | Stock → ajuste `-50` | "El ajuste dejaría el stock en negativo" y no pide confirmación |
| 13 | Stock → ajuste `0` o sin motivo | Mensaje de validación |
| 14 | Listado | `100001` muestra stock 5 con la insignia amarilla "Stock bajo" |
| 15 | Inactivar → elegir `100001`, motivo `Descontinuado` → Inactivar | La ventana advierte que no podrá incluirse en ventas ni reactivarse; al aceptar: "Producto inactivado" |
| 16 | Listado → estado `Inactivos` | `100001` aparece como Inactivo y ya no tiene botón Inactivar |
| 17 | Consultar `100001` | La ficha muestra el estado Inactivo y el motivo |

## 2. Como cajero: productos

Cerrar sesión y entrar como cajero.

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | Mirar las pestañas | Solo Categorías, Productos y Clientes (no Usuarios ni Roles) |
| 2 | Productos | Solo las subpestañas Listado y Consultar; la tabla no tiene columna de acciones |
| 3 | Listado y Consultar | Funcionan igual que para el administrador (incluida la insignia "Stock bajo") |

La restricción real la hace el servidor, no la pantalla: con el cajero, las rutas de crear, actualizar, inactivar, stock y ajustes responden 403 (lo comprueba la colección de Postman y las pruebas automáticas).

## 3. Clientes (cajero o administrador)

Los requisitos del Sprint 2 son solo crear y actualizar (RF-020, RF-021). La pestaña **Consultar** es un apoyo para poder ver y elegir a un cliente al actualizarlo; el RF-022 (consultar por documento o nombre) y el RF-023 (listar clientes) están planeados para el Sprint 3 y todavía no existe el listado.

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | Clientes → Crear: tipo `CC`, documento `1020304050`, nombre `Ana`, apellido `Lopez` (correo, teléfono y dirección vacíos) | "Cliente Ana Lopez creado (CC 1020304050)" |
| 2 | Crear otro: al elegir el tipo `NIT`, el campo "Nombre" pasa a ser **"Razón social"** y desaparece el apellido. Documento `900123456-7`, razón social `Distribuciones El Sol S.A.S.` | Se crea (el guion es válido) y el mensaje dice "Cliente Distribuciones El Sol S.A.S. creado (NIT 900123456-7)" |
| 2b | Consultar ese NIT | La ficha dice "Razón social" y no muestra apellido |
| 2c | Crear una persona (`CC`) sin apellido | Error "El apellido es obligatorio" (solo las personas lo necesitan) |
| 3 | Repetir el documento `1020304050` | "Ya existe un cliente con ese número de documento" |
| 4 | Crear con correo `abc` | Error de formato de correo |
| 5 | Crear con teléfono `abc` | Error de formato de teléfono |
| 6 | Consultar → escribir `lop` → elegir de la lista | Ficha con documento, nombre y `(sin correo)`, `(sin teléfono)`, `(sin dirección)` |
| 7 | Actualizar → elegir al cliente → se cargan sus datos → cambiar el teléfono a `3109876543` | La ventana muestra solo `Teléfono: (vacío) → 3109876543`; al aceptar: "Cliente actualizado" |
| 8 | Actualizar sin cambios | "No hay cambios para guardar" |
| 9 | Consultar un documento que no existe | "No existe un cliente con ese documento..." |

## 4. Colección de Postman

1. Importar `postman/POS_API.postman_collection.json`.
2. Rellenar las variables de la colección: `baseUrl` (por ejemplo `http://localhost:8081`), `adminCedula` y `adminPassword`.
3. Ejecutar la colección completa (Run). Resultado esperado: **59 requests y 59 pruebas, todas en verde** (el 2026-10-11 la colección tenía 56 y pasó completa; las 3 nuevas, del SKU y del NIT, aún no se han ejecutado en Postman). **No ejecutarla contra `pos_db`**: crea productos y clientes de prueba (e inactiva uno); usar una base aparte. Se puede ejecutar varias veces: los casos de "crear" aceptan 201 o 400 si ya existe.

Las carpetas nuevas son **Productos** (16), **Clientes** (10) y 7 casos nuevos dentro de **Control por rol (Cajero)**.

## 5. Pruebas automáticas

```bash
./mvnw test
```

Resultado esperado: **144 pruebas, 0 fallos** (en Windows con certificados corporativos: `MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=WINDOWS-ROOT`). Desde el Sprint 1 (64 pruebas) se agregaron 80: productos, clientes, login con nombre, SKU numérico y NIT.

## Resultado de la verificación automática (2026-10-11)

| Verificación | Resultado |
|---|---|
| Pruebas automáticas | 130 de 130 en verde |
| Colección de Postman con newman, contra la aplicación y MySQL reales | 56 requests, 56 pruebas, 0 fallos (dos ejecuciones seguidas) |
| Arranque con MySQL: Flyway valida `V1` y `V2` y Hibernate valida el esquema (`ddl-auto=validate`) | Correcto, sin errores en el log |
| Interfaz probada en el navegador | Productos: listado, crear, autocompletado, ajuste de stock con alerta, vista de cajero y consultar por nombre. Clientes: crear (NIT), autocompletado, actualizar con confirmación y consultar |
| No probado en pantalla | Productos → Actualizar e Inactivar (sí probados por API y Postman), y los mensajes de error de validación de clientes (sí cubiertos por pruebas automáticas) |

## Antes de terminar: borrar las bases de prueba

Durante el desarrollo quedaron bases de prueba en el MySQL local. No las usa la aplicación y se pueden borrar:

```sql
DROP DATABASE IF EXISTS pos_s2_test;
DROP DATABASE IF EXISTS pos_flyway_test;
DROP DATABASE IF EXISTS pos_ui_test;
```

Si al verificar se usó otra base de prueba, borrarla también. **No borrar `pos_db`**: es la base de desarrollo.
