package com.pos.producto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AjusteStockRepository extends JpaRepository<AjusteStock, AjusteStockId> {

    // Último consecutivo usado por el producto (0 si todavía no tiene ajustes).
    @Query("select coalesce(max(a.id.consecutivo), 0) from AjusteStock a where a.id.productoCodigo = :codigo")
    int ultimoConsecutivo(@Param("codigo") String codigo);
}
