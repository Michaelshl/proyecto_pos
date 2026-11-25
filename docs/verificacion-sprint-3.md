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

## Ventas (RF-025, RF-026, RF-027, RF-030 y base del RF-031)

| RF | Qué hace | Dónde se prueba |
|---|---|---|
| RF-025 | Crear una venta (borrador) y registrarla: recibe su número de factura y descuenta el stock | Ventas → Nueva venta |
| RF-026 | Agregar un producto a la venta | Ventas → Nueva venta |
| RF-027 | Quitar un producto de la venta | Ventas → Nueva venta |
| RF-030 | Consultar una venta | Ventas → Buscar |
| RF-031 (base) | Buscar ventas por factura, documento, fecha o producto | Ventas → Buscar |

Antes: tener un producto activo con stock (por ejemplo `100001`), uno inactivo, un cliente con correo, teléfono y dirección, y el cajero. Las migraciones `V5` y `V6` se aplican solas al arrancar.

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | Ventas → Nueva venta: el campo solo pide "Cédula / NIT del cliente"; dejarlo vacío → Iniciar venta | Cabecera "Cliente: Consumidor final (222222222222)". **No** muestra número de factura ni estado |
| 2 | Agregar `100001` ×2 | Una línea con el precio del producto, su subtotal y el total. Agregar de nuevo suma a la misma línea. Al final de la línea hay un tarro de basura para quitarla |
| 3 | Agregar más de lo que hay en stock | Se agrega, pero sale el aviso "Stock insuficiente..." |
| 4 | Agregar el producto inactivo / un código que no existe | "El producto ... está inactivo..." / "Producto no encontrado" |
| 5 | **Registrar venta** con stock insuficiente | Lo rechaza, la venta sigue como borrador y el stock no cambia |
| 6 | **Quitar** esa línea y **Registrar venta** → aceptar | "Venta registrada: factura F-..."; pasa a `REGISTRADA`; los botones cambian a "Nueva venta" |
| 7 | Productos → Listado | El stock bajó en lo vendido, y solo al registrar |
| 8 | Iniciar otra venta, agregar un producto y pulsar **Cancelar venta** → aceptar | Vuelve a la pantalla de iniciar; no queda nada guardado (el stock no cambió) |
| 9 | Iniciar una venta, agregar un producto y **cerrar sesión** (o cerrar la pestaña) | El borrador se borra; al volver a entrar no hay nada pendiente |
| 10 | Mirar las facturas registradas en la base | La numeración es `F-000001`, `F-000002`... **sin huecos**, aunque haya habido borradores cancelados |
| 11 | Iniciar una venta con un cliente registrado y marcar **Nota de pedido** | Muestra el cliente, "Correo para la factura: an****es@..." y "Teléfono: 315******* · Dirección: Calle 10***" |
| 12 | Lo mismo como **administrador** | Correo, teléfono y dirección completos |
| 13 | Marcar **Nota de pedido** sin cliente | Aparecen los campos del contacto: celular, nombre, correo y dirección. Al escribir un cliente, desaparecen |
| 13b | Escribir un celular nuevo → **Buscar contacto** | "Ese celular no está registrado: complete nombre, correo y dirección". Completar y **Iniciar venta**: la cabecera dice "Contacto de pedido: ..." con los datos enmascarados |
| 13c | Registrar esa venta y hacer otro pedido con el **mismo celular** → Buscar contacto | "Contacto registrado: Carlos Pérez..." (correo y dirección enmascarados) y ya no pide los datos |
| 13d | Nuevo contacto sin correo o sin dirección | Lo rechaza ("el nombre, el correo y la dirección son obligatorios") |
| 14 | Ventas → Buscar: los últimos 4 dígitos de una factura | Aparece esa factura. También por documento del cliente o por el celular de un contacto |
| 14b | Buscar por parte de un documento (por ejemplo `1010`) con dos clientes parecidos | La lista **no dice quién es el cliente**: solo muestra el documento enmascarado (`10******10`). Al pulsar **Ver** aparece el nombre y se confirma que es la persona correcta |
| 15 | Buscar por fecha (desde/hasta) o por producto (código o nombre) | Las ventas que cumplen, con el total de ingresos de las registradas |
| 16 | **Ver** una venta del resultado; pulsar **Ocultar** (el mismo botón) | Muestra su detalle (estado, cajero, cliente, total y líneas) y al pulsar otra vez lo oculta |
| 16b | Pulsar **Limpiar** en Buscar | Se vacían los filtros, los resultados y el detalle |
| 17 | Como cajero, buscar la venta de otro cajero | No aparece. El administrador ve todas y filtra por cajero y estado |
| 17b | Como cajero, buscar desde el año 2003 (o cualquier rango de más de un año) | "El rango de fechas no puede superar un año: acorte el período". El administrador sí puede; la lista trae las 200 más recientes y el total de ingresos cuenta todas |
| 18 | Cerrar sesión con la pestaña Ventas abierta | Solo se ve el inicio de sesión, con la cédula vacía (se borra al entrar con éxito) |

Los borradores que nadie terminó (por ejemplo, si se cae la conexión) los borra el servidor: todos al arrancar y, cada 10 minutos, los de más de 30 minutos.
