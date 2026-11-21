package com.pos.venta;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, String> {

    // Los filtros de la búsqueda de ventas (los borradores nunca aparecen). Todos son opcionales: el texto coincide
    // con el final del número de factura (por ejemplo sus últimos 4 dígitos), con el comienzo del documento del
    // cliente o con el comienzo del celular de un contacto de pedido; el producto, con su código o con parte de su
    // nombre. La fecha final es exclusiva. Se comparten entre la lista y los totales para que siempre coincidan.
    String FILTROS_DE_BUSQUEDA = """
            v.estado <> 'EN_PROCESO'
              and (:cajero is null or v.cajeroCedula = :cajero)
              and (:estado is null or v.estado = :estado)
              and (:texto is null or v.numeroFactura like concat('%', :texto)
                                  or v.clienteDocumento like concat(:texto, '%')
                                  or v.contactoTelefono like concat(:texto, '%'))
              and (:desde is null or v.fecha >= :desde)
              and (:hasta is null or v.fecha < :hasta)
              and (:producto is null or exists (
                    select 1 from DetalleVenta d
                    where d.id.numeroFactura = v.numeroFactura
                      and (d.productoCodigo like concat(:producto, '%')
                           or d.productoCodigo in (select p.codigo from Producto p
                                                   where lower(p.nombre) like lower(concat('%', :producto, '%'))))))
            """;

    // Las ventas más recientes que cumplen la búsqueda (el límite lo pone el Pageable).
    @Query("select v from Venta v where " + FILTROS_DE_BUSQUEDA + " order by v.fecha desc")
    List<Venta> buscar(@Param("texto") String texto, @Param("desde") LocalDateTime desde,
                       @Param("hasta") LocalDateTime hasta, @Param("producto") String producto,
                       @Param("estado") String estado, @Param("cajero") String cajero, Pageable limite);

    // Cuántas ventas cumplen la búsqueda en total (las calcula la base de datos, no se traen).
    @Query("select count(v) from Venta v where " + FILTROS_DE_BUSQUEDA)
    long contar(@Param("texto") String texto, @Param("desde") LocalDateTime desde,
                @Param("hasta") LocalDateTime hasta, @Param("producto") String producto,
                @Param("estado") String estado, @Param("cajero") String cajero);

    // Suma de las ventas REGISTRADAS que cumplen la búsqueda (nulo si no hay ninguna).
    @Query("select sum(v.total) from Venta v where v.estado = 'REGISTRADA' and " + FILTROS_DE_BUSQUEDA)
    BigDecimal sumarIngresos(@Param("texto") String texto, @Param("desde") LocalDateTime desde,
                             @Param("hasta") LocalDateTime hasta, @Param("producto") String producto,
                             @Param("estado") String estado, @Param("cajero") String cajero);

    // Borra los borradores creados antes del límite (las líneas se borran antes, con DetalleVentaRepository).
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Venta v where v.estado = 'EN_PROCESO' and v.fecha < :limite")
    int borrarBorradoresAnterioresA(@Param("limite") LocalDateTime limite);
}
