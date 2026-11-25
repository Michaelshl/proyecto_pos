package com.pos.venta;

import com.pos.auditoria.Auditoria;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// Red de seguridad: los borradores de venta que nadie terminó no se conservan. La pantalla los cancela al salir,
// pero si se cae la conexión o se apaga el equipo, el servidor los borra: todos al arrancar y, cada 10 minutos,
// los que llevan más de 30 minutos.
@Component
public class LimpiezaDeBorradores {

    static final int MINUTOS_DE_VIDA = 30;

    private final VentaService ventaService;

    public LimpiezaDeBorradores(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar() {
        int borrados = ventaService.limpiarBorradores(LocalDateTime.now().plusDays(1));
        if (borrados > 0) {
            Auditoria.evento("Borradores de venta borrados al arrancar", borrados + " borrador(es)");
        }
    }

    @Scheduled(initialDelay = 600_000, fixedDelay = 600_000)
    public void periodica() {
        int borrados = ventaService.limpiarBorradores(LocalDateTime.now().minusMinutes(MINUTOS_DE_VIDA));
        if (borrados > 0) {
            Auditoria.evento("Borradores de venta sin terminar borrados", borrados + " borrador(es)");
        }
    }
}
