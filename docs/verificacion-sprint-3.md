# Verificación del Sprint 3

Guía para comprobar a mano lo que se va implementando en el Sprint 3. Se va ampliando con cada módulo. Para arrancar la aplicación y la base de datos, ver `verificacion-sprint-2.md` (sección 0).

## Ajustes pedidos el 2026-11-10

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | Pantalla de inicio de sesión: pulsar el **ojo** a la derecha de la contraseña | La contraseña se ve; al pulsar de nuevo, se oculta. Lo mismo en Usuarios → Crear. Al entrar o salir de la sesión, vuelve a quedar oculta |
| 2 | Entrar como cajero → Productos → Listado | Sin selector de estado y sin columnas de estado y acciones; solo productos activos, el de menor stock primero |
| 3 | Como cajero, consultar por código un producto inactivo | "Producto no encontrado"; tampoco sale en el autocompletado |
| 4 | Como administrador, Productos → Listado | Sigue el selector de estado y los inactivos aparecen al final; filtrar por "Inactivos" los muestra |

| 5 | Productos → Listado: escribir el código de un producto (por ejemplo `123456`) o su comienzo | Aparece ese producto (el texto busca por nombre o por el comienzo del código); un código inactivo no aparece para el cajero |
| 6 | Clientes → Actualizar: cargar un cliente, cambiar un dato y aceptar la confirmación | Se actualiza y el formulario se limpia solo (el mensaje "Cliente actualizado" queda a la vista) |
| 7 | Clientes → Actualizar: cargar un cliente y pulsar **Limpiar** | Se vacían todos los campos sin actualizar nada |

Por API, un cajero que pide `GET /api/productos?activo=false` recibe igualmente solo los activos.

## Clientes: consulta y listado (RF-022 y RF-023)

| RF | Qué hace | Quién | Dónde se prueba |
|---|---|---|---|
| RF-023 | Listar los clientes con su total | Cajero y administrador | Clientes → Listado |
| RF-022 | Consultar un cliente por documento o por nombre | Cajero y administrador | Clientes → Consultar y Clientes → Listado |

Al iniciar sesión, la pestaña **Clientes** abre en **Listado**.

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | Clientes → Listado | Tabla con todos los clientes (documento, nombre o razón social, correo, teléfono y dirección) ordenados por apellido; para una empresa (NIT), por razón social. Arriba dice "N cliente(s) encontrado(s)" |
| 2 | Escribir `ana` y pulsar Buscar (o Enter) | Solo los clientes cuyo documento empieza por el texto, o cuyo nombre o apellido lo contiene, sin importar mayúsculas |
| 3 | Escribir un texto que no existe | La tabla dice "Sin resultados" y el mensaje "No hay clientes que coincidan" |
| 4 | Vaciar el texto y Buscar | Vuelven todos |
| 5 | Clientes → Crear (o Actualizar) un cliente y volver a Listado | El listado ya refleja el cambio |
| 6 | Clientes → Consultar → escribir un documento exacto | Ficha completa del cliente |
| 7 | Consultar → escribir `ana` (no es un documento) | Si hay una sola coincidencia, su ficha; si hay varias, "N clientes coinciden. Elija uno:" con un botón **Ver** en cada una |
| 8 | Consultar → un texto que no existe | "No hay ningún cliente con ese documento o nombre" |
| 9 | Entrar como cajero | Las mismas pantallas funcionan igual |

Por API: `GET /api/clientes` (lista con `total`) y `GET /api/clientes?q=texto`. Sin token responde 401.
