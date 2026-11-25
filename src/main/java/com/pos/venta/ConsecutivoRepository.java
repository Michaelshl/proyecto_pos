package com.pos.venta;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConsecutivoRepository extends JpaRepository<Consecutivo, String> {

    // Lee la fila con bloqueo de escritura (SELECT ... FOR UPDATE): otro cajero que pida el siguiente número
    // espera a que esta transacción termine, así nunca reciben el mismo número de factura.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Consecutivo c where c.tipo = :tipo")
    Optional<Consecutivo> bloquear(@Param("tipo") String tipo);
}
