package com.pos.venta;

import com.pos.auditoria.Auditoria;
import com.pos.cliente.Cliente;
import com.pos.cliente.ClienteRepository;
import com.pos.producto.Producto;
import com.pos.producto.ProductoRepository;
import com.pos.venta.dto.ContactoPedidoRequest;
import com.pos.venta.dto.ContactoPedidoResponse;
import com.pos.venta.dto.LineaVentaRequest;
import com.pos.venta.dto.LineaVentaResponse;
import com.pos.venta.dto.VentaCrearRequest;
import com.pos.venta.dto.VentaListaResponse;
import com.pos.venta.dto.VentaResponse;
import com.pos.venta.dto.VentaResumenResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// Ventas (Sprint 3). Una venta nace como BORRADOR (EN_PROCESO) con un número propio (B-000001) y no toca el stock.
// Al registrarla recibe su número de factura (F-000001), se descuenta el stock de todos sus productos y el borrador
// desaparece, todo en una sola transacción: si algo falla, no cambia nada. Un borrador que se cancela, o que nadie
// termina, se borra: no interesa conservarlo.
@Service
public class VentaService {

    static final String TIPO_FACTURA = "FACTURA";
    static final String PREFIJO_FACTURA = "F-";
    static final String TIPO_BORRADOR = "BORRADOR";
    static final String PREFIJO_BORRADOR = "B-";

    // Una venta sin cliente es de "Consumidor final" (documento genérico, sin crear un cliente falso en la base).
    static final String CONSUMIDOR_FINAL_NOMBRE = "Consumidor final";
    static final String CONSUMIDOR_FINAL_DOCUMENTO = "222222222222";

    // La búsqueda muestra como máximo las 200 ventas más recientes (los totales sí cuentan todas), y al cajero no
    // se le permite buscar en un rango de fechas de más de un año.
    static final int MAX_RESULTADOS = 200;
    static final long MAX_DIAS_CAJERO = 366;

    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleRepository;
    private final ConsecutivoRepository consecutivoRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final ContactoPedidoRepository contactoRepository;

    public VentaService(VentaRepository ventaRepository, DetalleVentaRepository detalleRepository,
                        ConsecutivoRepository consecutivoRepository, ProductoRepository productoRepository,
                        ClienteRepository clienteRepository, ContactoPedidoRepository contactoRepository) {
        this.ventaRepository = ventaRepository;
        this.detalleRepository = detalleRepository;
        this.consecutivoRepository = consecutivoRepository;
        this.productoRepository = productoRepository;
        this.clienteRepository = clienteRepository;
        this.contactoRepository = contactoRepository;
    }

    // RF-025: crea el borrador con su número propio. El cliente es opcional (sin cliente = Consumidor final).
    @Transactional
    public VentaResponse crear(VentaCrearRequest request, String cedulaCajero, boolean esAdmin) {
        String documento = vacioANulo(request.getClienteDocumento());
        boolean esPedido = Boolean.TRUE.equals(request.getNotaPedido());
        if (documento != null && !clienteRepository.existsById(documento)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cliente no existe");
        }
        // Una nota de pedido es de un cliente registrado o, si no lo es, de un contacto de pedido (celular, nombre,
        // correo y dirección). El contacto no es un cliente para facturación: se guarda aparte.
        String telefonoContacto = esPedido && documento == null ? contactoDePedido(request.getContacto()) : null;

        Venta venta = new Venta();
        venta.setNumeroFactura(siguienteNumero(TIPO_BORRADOR, PREFIJO_BORRADOR));
        venta.setFecha(LocalDateTime.now());
        venta.setEstado(EstadoVenta.EN_PROCESO);
        venta.setTotal(BigDecimal.ZERO);
        venta.setCajeroCedula(cedulaCajero);
        venta.setClienteDocumento(documento);
        venta.setEsPedido(esPedido);
        venta.setContactoTelefono(telefonoContacto);
        ventaRepository.save(venta);

        Auditoria.accion("Venta iniciada", "borrador " + venta.getNumeroFactura()
                + (documento != null ? ", cliente " + documento
                : telefonoContacto != null ? ", contacto de pedido " + Enmascarar.telefono(telefonoContacto)
                : ", consumidor final")
                + (esPedido ? ", nota de pedido" : ""));
        return respuesta(venta, List.of(), List.of(), esAdmin);
    }

    // RF-030: el detalle de una venta por su número. El administrador ve cualquiera; el cajero, solo las suyas
    // (las demás responden como si no existieran).
    @Transactional(readOnly = true)
    public VentaResponse consultar(String numero, String cedulaUsuario, boolean esAdmin) {
        Venta venta = obtener(numero);
        if (!esAdmin && !venta.getCajeroCedula().equals(cedulaUsuario)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada");
        }
        return respuesta(venta, detalleRepository.findByIdNumeroFacturaOrderByIdLinea(venta.getNumeroFactura()),
                List.of(), esAdmin);
    }

    // Buscar ventas registradas o anuladas por los últimos dígitos de la factura o el documento del cliente
    // (texto), por rango de fechas o por producto. El cajero solo ve las suyas y no puede buscar en un rango de
    // más de un año; el administrador ve todas y filtra por cajero y estado. Es la base del RF-031.
    // El total y los ingresos son de TODAS las ventas que cumplen la búsqueda (los calcula la base de datos); la
    // lista trae solo las 200 más recientes. Al cajero la lista no le revela quién es el cliente: ve el documento
    // enmascarado y, para saber quién es, abre la venta.
    @Transactional(readOnly = true)
    public VentaListaResponse buscar(String texto, LocalDate desde, LocalDate hasta, String producto,
                                     String estado, String cajero, String cedulaUsuario, boolean esAdmin) {
        String estadoFiltro = vacioANulo(estado);
        if (estadoFiltro != null && !EstadoVenta.REGISTRADA.equals(estadoFiltro)
                && !EstadoVenta.ANULADA.equals(estadoFiltro)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado debe ser REGISTRADA o ANULADA");
        }
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha final no puede ser anterior a la inicial");
        }
        if (!esAdmin && desde != null
                && ChronoUnit.DAYS.between(desde, hasta != null ? hasta : LocalDate.now()) > MAX_DIAS_CAJERO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El rango de fechas no puede superar un año: acorte el período");
        }
        String cajeroFiltro = esAdmin ? vacioANulo(cajero) : cedulaUsuario;
        String textoFiltro = vacioANulo(limpiarComodines(texto));
        String productoFiltro = vacioANulo(limpiarComodines(producto));
        LocalDateTime desdeFecha = desde == null ? null : desde.atStartOfDay();
        LocalDateTime hastaFecha = hasta == null ? null : hasta.plusDays(1).atStartOfDay();

        long total = ventaRepository.contar(textoFiltro, desdeFecha, hastaFecha, productoFiltro, estadoFiltro, cajeroFiltro);
        BigDecimal ingresos = ventaRepository.sumarIngresos(textoFiltro, desdeFecha, hastaFecha, productoFiltro,
                estadoFiltro, cajeroFiltro);
        List<Venta> ventas = ventaRepository.buscar(textoFiltro, desdeFecha, hastaFecha, productoFiltro,
                estadoFiltro, cajeroFiltro, PageRequest.of(0, MAX_RESULTADOS));

        Map<String, Cliente> clientes = clienteRepository.findAllById(ventas.stream()
                .map(Venta::getClienteDocumento).filter(d -> d != null).distinct().toList()).stream()
                .collect(Collectors.toMap(Cliente::getNumeroDocumento, c -> c));
        Map<String, ContactoPedido> contactos = contactoRepository.findAllById(ventas.stream()
                .map(Venta::getContactoTelefono).filter(t -> t != null).distinct().toList()).stream()
                .collect(Collectors.toMap(ContactoPedido::getTelefono, c -> c));
        List<VentaResumenResponse> filas = ventas.stream().map(v -> resumen(v, clientes, contactos, esAdmin)).toList();
        return new VentaListaResponse(total, ingresos == null ? BigDecimal.ZERO : ingresos, total > filas.size(), filas);
    }

    private VentaResumenResponse resumen(Venta v, Map<String, Cliente> clientes, Map<String, ContactoPedido> contactos,
                                         boolean esAdmin) {
        boolean contacto = v.getContactoTelefono() != null;
        boolean consumidorFinal = v.getClienteDocumento() == null && !contacto;
        String nombre;
        String documento;
        if (consumidorFinal) {
            nombre = CONSUMIDOR_FINAL_NOMBRE;
            documento = CONSUMIDOR_FINAL_DOCUMENTO;
        } else if (contacto) {
            ContactoPedido c = contactos.get(v.getContactoTelefono());
            nombre = esAdmin && c != null ? c.getNombre() : null;
            documento = esAdmin ? v.getContactoTelefono() : Enmascarar.telefono(v.getContactoTelefono());
        } else {
            Cliente c = clientes.get(v.getClienteDocumento());
            nombre = esAdmin && c != null ? nombreCompleto(c) : null;
            documento = esAdmin ? v.getClienteDocumento() : Enmascarar.documento(v.getClienteDocumento());
        }
        return VentaResumenResponse.builder()
                .numeroFactura(v.getNumeroFactura())
                .fecha(v.getFecha())
                .estado(v.getEstado())
                .total(v.getTotal())
                .cajeroCedula(v.getCajeroCedula())
                .clienteDocumento(documento)
                .clienteNombre(nombre)
                .esPedido(v.getEsPedido())
                .consumidorFinal(consumidorFinal)
                .contactoPedido(contacto)
                .build();
    }

    // Consulta un contacto de pedido por su celular (el cajero lo escribe, así que lo conoce). El cajero recibe el
    // correo y la dirección enmascarados; el administrador, completos.
    @Transactional(readOnly = true)
    public ContactoPedidoResponse consultarContacto(String telefono, boolean esAdmin) {
        ContactoPedido c = contactoRepository.findById(telefono.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No hay un contacto de pedido con ese celular"));
        return new ContactoPedidoResponse(c.getTelefono(), c.getNombre(),
                esAdmin ? c.getCorreo() : Enmascarar.correo(c.getCorreo()),
                esAdmin ? c.getDireccion() : Enmascarar.direccion(c.getDireccion()));
    }

    // RF-026: agrega un producto al borrador. Si el producto ya está, suma la cantidad a su línea. Un producto
    // inactivo no puede venderse (RF-017). Si el stock no alcanza solo se avisa: no se podrá registrar hasta que
    // alcance.
    @Transactional
    public VentaResponse agregarProducto(String numero, LineaVentaRequest request,
                                         String cedulaUsuario, boolean esAdmin) {
        Venta venta = obtenerEditable(numero, cedulaUsuario, esAdmin);
        Producto producto = productoRepository.findById(request.getCodigo().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
        if (!Boolean.TRUE.equals(producto.getActivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El producto " + producto.getCodigo() + " está inactivo y no puede venderse");
        }

        List<DetalleVenta> lineas = new ArrayList<>(
                detalleRepository.findByIdNumeroFacturaOrderByIdLinea(venta.getNumeroFactura()));
        Optional<DetalleVenta> existente = lineas.stream()
                .filter(l -> l.getProductoCodigo().equals(producto.getCodigo())).findFirst();

        int cantidadEnLaVenta;
        if (existente.isPresent()) {
            DetalleVenta linea = existente.get();
            cantidadEnLaVenta = linea.getCantidad() + request.getCantidad();
            linea.setCantidad(cantidadEnLaVenta);
            detalleRepository.save(linea);
        } else {
            DetalleVenta linea = new DetalleVenta();
            linea.setId(new DetalleVentaId(venta.getNumeroFactura(),
                    detalleRepository.ultimaLinea(venta.getNumeroFactura()) + 1));
            linea.setProductoCodigo(producto.getCodigo());
            linea.setCantidad(request.getCantidad());
            linea.setPrecioUnitario(producto.getPrecioVenta());
            detalleRepository.save(linea);
            lineas.add(linea);
            cantidadEnLaVenta = request.getCantidad();
        }

        List<String> advertencias = new ArrayList<>();
        if (cantidadEnLaVenta > producto.getStock()) {
            advertencias.add("Stock insuficiente de " + producto.getCodigo() + ": hay " + producto.getStock()
                    + " y la venta lleva " + cantidadEnLaVenta + ". No se podrá registrar hasta que alcance");
        }

        venta.setTotal(total(lineas));
        ventaRepository.save(venta);
        Auditoria.accion("Producto agregado a venta", "borrador " + venta.getNumeroFactura() + ", producto "
                + producto.getCodigo() + ", cantidad " + request.getCantidad());
        return respuesta(venta, lineas, advertencias, esAdmin);
    }

    // RF-027: quita una línea del borrador.
    @Transactional
    public VentaResponse eliminarLinea(String numero, int linea, String cedulaUsuario, boolean esAdmin) {
        Venta venta = obtenerEditable(numero, cedulaUsuario, esAdmin);
        DetalleVentaId id = new DetalleVentaId(venta.getNumeroFactura(), linea);
        DetalleVenta detalle = detalleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La línea " + linea + " no existe en la venta " + venta.getNumeroFactura()));
        detalleRepository.delete(detalle);

        List<DetalleVenta> lineas = detalleRepository.findByIdNumeroFacturaOrderByIdLinea(venta.getNumeroFactura());
        venta.setTotal(total(lineas));
        ventaRepository.save(venta);
        Auditoria.accion("Producto quitado de venta", "borrador " + venta.getNumeroFactura()
                + ", línea " + linea + ", producto " + detalle.getProductoCodigo());
        return respuesta(venta, lineas, List.of(), esAdmin);
    }

    // Cancela el borrador: borra sus líneas y el borrador. No queda nada guardado.
    @Transactional
    public void cancelar(String numero, String cedulaUsuario, boolean esAdmin) {
        Venta borrador = obtenerEditable(numero, cedulaUsuario, esAdmin);
        eliminarBorrador(borrador, detalleRepository.findByIdNumeroFacturaOrderByIdLinea(borrador.getNumeroFactura()));
        Auditoria.accion("Venta cancelada", "borrador " + borrador.getNumeroFactura());
    }

    // RF-025: registra la venta. Valida todos los productos, descuenta el stock, le asigna el número de factura
    // y borra el borrador. Todo o nada.
    @Transactional
    public VentaResponse registrar(String numero, String cedulaUsuario, boolean esAdmin) {
        Venta borrador = obtenerEditable(numero, cedulaUsuario, esAdmin);
        List<DetalleVenta> lineas = detalleRepository.findByIdNumeroFacturaOrderByIdLinea(borrador.getNumeroFactura());
        if (lineas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La venta no tiene productos");
        }

        for (DetalleVenta linea : lineas) {
            Producto producto = productoRepository.findById(linea.getProductoCodigo())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Producto " + linea.getProductoCodigo() + " no encontrado"));
            if (!Boolean.TRUE.equals(producto.getActivo())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El producto " + producto.getCodigo() + " está inactivo y no puede venderse");
            }
            if (producto.getStock() < linea.getCantidad()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock insuficiente de "
                        + producto.getCodigo() + ": hay " + producto.getStock() + " y la venta lleva "
                        + linea.getCantidad());
            }
            producto.setStock(producto.getStock() - linea.getCantidad());
            productoRepository.save(producto);
        }

        // El número de factura se asigna solo ahora, así no quedan huecos por borradores abandonados.
        Venta venta = new Venta();
        venta.setNumeroFactura(siguienteNumero(TIPO_FACTURA, PREFIJO_FACTURA));
        venta.setFecha(LocalDateTime.now());
        venta.setEstado(EstadoVenta.REGISTRADA);
        venta.setTotal(total(lineas));
        venta.setCajeroCedula(borrador.getCajeroCedula());
        venta.setClienteDocumento(borrador.getClienteDocumento());
        venta.setEsPedido(borrador.getEsPedido());
        venta.setContactoTelefono(borrador.getContactoTelefono());
        ventaRepository.save(venta);

        List<DetalleVenta> definitivas = new ArrayList<>();
        for (DetalleVenta linea : lineas) {
            DetalleVenta copia = new DetalleVenta();
            copia.setId(new DetalleVentaId(venta.getNumeroFactura(), linea.getId().getLinea()));
            copia.setProductoCodigo(linea.getProductoCodigo());
            copia.setCantidad(linea.getCantidad());
            copia.setPrecioUnitario(linea.getPrecioUnitario());
            definitivas.add(detalleRepository.save(copia));
        }
        eliminarBorrador(borrador, lineas);

        Auditoria.accion("Venta registrada", "factura " + venta.getNumeroFactura() + ", total " + venta.getTotal()
                + ", " + definitivas.size() + " línea(s)");
        return respuesta(venta, definitivas, List.of(), esAdmin);
    }

    // Borra los borradores creados antes del límite: los que nadie terminó (se cerró la pantalla, se cayó la
    // conexión). Con un límite en el futuro borra todos (se usa al arrancar la aplicación).
    @Transactional
    public int limpiarBorradores(LocalDateTime antesDe) {
        detalleRepository.borrarLineasDeBorradoresAnterioresA(antesDe);
        return ventaRepository.borrarBorradoresAnterioresA(antesDe);
    }

    // ---- apoyo ----

    // Lee el contador con bloqueo, lo aumenta y arma el número (F-000001, B-000001). Si la fila no existe (las
    // pruebas arman la base sin Flyway), la crea.
    private String siguienteNumero(String tipo, String prefijo) {
        Consecutivo consecutivo = consecutivoRepository.bloquear(tipo).orElseGet(() -> {
            Consecutivo nuevo = new Consecutivo();
            nuevo.setTipo(tipo);
            nuevo.setUltimoValor(0L);
            return nuevo;
        });
        consecutivo.setUltimoValor(consecutivo.getUltimoValor() + 1);
        consecutivoRepository.save(consecutivo);
        return String.format(prefijo + "%06d", consecutivo.getUltimoValor());
    }

    // Las líneas se borran antes que la venta (clave foránea).
    private void eliminarBorrador(Venta borrador, List<DetalleVenta> lineas) {
        detalleRepository.deleteAll(lineas);
        ventaRepository.delete(borrador);
    }

    private Venta obtener(String numero) {
        return ventaRepository.findById(numero.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada"));
    }

    // Solo se modifica un borrador, y solo quien lo creó (o un administrador).
    private Venta obtenerEditable(String numero, String cedulaUsuario, boolean esAdmin) {
        Venta venta = obtener(numero);
        if (!EstadoVenta.EN_PROCESO.equals(venta.getEstado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La venta " + venta.getNumeroFactura() + " está " + venta.getEstado() + " y no se puede modificar");
        }
        if (!esAdmin && !venta.getCajeroCedula().equals(cedulaUsuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo quien creó la venta, o un administrador, puede modificarla");
        }
        return venta;
    }

    private static BigDecimal total(List<DetalleVenta> lineas) {
        return lineas.stream()
                .map(l -> l.getPrecioUnitario().multiply(BigDecimal.valueOf(l.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Arma la respuesta. Los datos de contacto del cliente (correo, y teléfono y dirección en una nota de pedido)
    // salen completos solo para el administrador; el cajero los recibe enmascarados.
    private VentaResponse respuesta(Venta venta, List<DetalleVenta> lineas, List<String> advertencias,
                                    boolean esAdmin) {
        Map<String, String> nombres = productoRepository
                .findAllById(lineas.stream().map(DetalleVenta::getProductoCodigo).toList()).stream()
                .collect(Collectors.toMap(Producto::getCodigo, Producto::getNombre, (a, b) -> a));
        List<LineaVentaResponse> lineasRespuesta = lineas.stream().map(l -> LineaVentaResponse.builder()
                .linea(l.getId().getLinea())
                .productoCodigo(l.getProductoCodigo())
                .productoNombre(nombres.get(l.getProductoCodigo()))
                .cantidad(l.getCantidad())
                .precioUnitario(l.getPrecioUnitario())
                .subtotal(l.getPrecioUnitario().multiply(BigDecimal.valueOf(l.getCantidad())))
                .build()).toList();

        Optional<Cliente> cliente = venta.getClienteDocumento() == null ? Optional.empty()
                : clienteRepository.findById(venta.getClienteDocumento());
        Optional<ContactoPedido> contacto = venta.getContactoTelefono() == null ? Optional.empty()
                : contactoRepository.findById(venta.getContactoTelefono());
        boolean esContacto = venta.getContactoTelefono() != null;
        boolean consumidorFinal = venta.getClienteDocumento() == null && !esContacto;
        boolean pedido = Boolean.TRUE.equals(venta.getEsPedido());

        // Los datos de contacto son los del cliente registrado o, en una nota de pedido sin cliente, los del contacto.
        String nombre = consumidorFinal ? CONSUMIDOR_FINAL_NOMBRE
                : esContacto ? contacto.map(ContactoPedido::getNombre).orElse(null)
                : cliente.map(VentaService::nombreCompleto).orElse(null);
        String correo = cliente.map(Cliente::getEmail).orElseGet(() -> contacto.map(ContactoPedido::getCorreo).orElse(null));
        String telefono = esContacto ? venta.getContactoTelefono() : cliente.map(Cliente::getTelefono).orElse(null);
        String direccion = cliente.map(Cliente::getDireccion)
                .orElseGet(() -> contacto.map(ContactoPedido::getDireccion).orElse(null));

        return VentaResponse.builder()
                .numeroFactura(venta.getNumeroFactura())
                .fecha(venta.getFecha())
                .estado(venta.getEstado())
                .total(venta.getTotal())
                .cajeroCedula(venta.getCajeroCedula())
                .clienteDocumento(consumidorFinal ? CONSUMIDOR_FINAL_DOCUMENTO : venta.getClienteDocumento())
                .clienteNombre(nombre)
                .consumidorFinal(consumidorFinal)
                .clienteCorreo(esAdmin ? correo : Enmascarar.correo(correo))
                .esPedido(pedido)
                .clienteTelefono(pedido ? (esAdmin ? telefono : Enmascarar.telefono(telefono)) : null)
                .clienteDireccion(pedido ? (esAdmin ? direccion : Enmascarar.direccion(direccion)) : null)
                .contactoPedido(esContacto)
                .motivoAnulacion(venta.getMotivoAnulacion())
                .lineas(lineasRespuesta)
                .advertencias(advertencias)
                .build();
    }

    // El contacto de una nota de pedido sin cliente registrado. Si el celular ya está guardado se usa ese contacto;
    // si es nuevo se exigen nombre, correo y dirección y se guarda para los próximos pedidos. No es un cliente.
    private String contactoDePedido(ContactoPedidoRequest contacto) {
        String telefono = contacto == null ? null : vacioANulo(contacto.getTelefono());
        if (telefono == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Una nota de pedido necesita un cliente registrado o el celular del contacto");
        }
        if (contactoRepository.existsById(telefono)) {
            return telefono;
        }
        String nombre = vacioANulo(contacto.getNombre());
        String correo = vacioANulo(contacto.getCorreo());
        String direccion = vacioANulo(contacto.getDireccion());
        if (nombre == null || correo == null || direccion == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Es un contacto nuevo: el nombre, el correo y la dirección son obligatorios");
        }
        ContactoPedido nuevo = new ContactoPedido();
        nuevo.setTelefono(telefono);
        nuevo.setNombre(nombre);
        nuevo.setCorreo(correo);
        nuevo.setDireccion(direccion);
        contactoRepository.save(nuevo);
        Auditoria.accion("Contacto de pedido registrado", "celular " + Enmascarar.telefono(telefono));
        return telefono;
    }

    // Una persona tiene nombre y apellido; una empresa (NIT) solo razón social, guardada en "nombre".
    private static String nombreCompleto(Cliente c) {
        return c.getApellido() == null || c.getApellido().isBlank() ? c.getNombre() : c.getNombre() + " " + c.getApellido();
    }

    // Se quitan % y _ para que el texto escrito no funcione como comodín del LIKE.
    private static String limpiarComodines(String texto) {
        return texto == null ? null : texto.replaceAll("[%_\\\\]", "").trim();
    }

    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
