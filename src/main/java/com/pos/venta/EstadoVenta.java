package com.pos.venta;

// Estados de una venta. En la base se guardan como texto (no como ENUM nativo), igual que los demás
// catálogos, y los valida la aplicación.
public final class EstadoVenta {

    // Borrador: se agregan y quitan productos; no toca el stock.
    public static final String EN_PROCESO = "EN_PROCESO";
    // Confirmada: descontó el stock.
    public static final String REGISTRADA = "REGISTRADA";
    // Anulada (RF-029): devolvió el stock.
    public static final String ANULADA = "ANULADA";

    private EstadoVenta() {
    }
}
