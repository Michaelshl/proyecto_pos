package com.pos.venta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, DetalleVentaId> {

    List<DetalleVenta> findByIdNumeroFacturaOrderByIdLinea(String numeroFactura);

    // Borra las líneas de los borradores creados antes del límite; va antes de borrar los borradores (clave foránea).
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from DetalleVenta d where d.id.numeroFactura in "
            + "(select v.numeroFactura from Venta v where v.estado = 'EN_PROCESO' and v.fecha < :limite)")
    int borrarLineasDeBorradoresAnterioresA(@Param("limite") LocalDateTime limite);

    // Última línea usada por la factura (0 si todavía no tiene productos).
    @Query("select coalesce(max(d.id.linea), 0) from DetalleVenta d where d.id.numeroFactura = :factura")
    int ultimaLinea(@Param("factura") String factura);
}
