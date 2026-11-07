package com.pos.producto;

import java.util.Arrays;
import java.util.Optional;

// Lista cerrada de motivos de un ajuste de stock (RF-019). Cada motivo dice si suma, resta o puede ser
// cualquiera de las dos, para que un ajuste no sea "escribir el número que se quiera": quien saca un
// producto no puede disfrazarlo de ingreso, y una salida siempre cae en un motivo de pérdida o de conteo.
public enum MotivoAjuste {

    INGRESO_COMPRA("Ingreso por compra al proveedor", Sentido.ENTRADA),
    DEVOLUCION_CLIENTE("Devolución de un cliente", Sentido.ENTRADA),
    DANO("Producto dañado", Sentido.SALIDA),
    VENCIMIENTO("Producto vencido", Sentido.SALIDA),
    PERDIDA("Pérdida o robo", Sentido.SALIDA),
    CONTEO_FISICO("Corrección por conteo físico", Sentido.AMBOS);

    public enum Sentido { ENTRADA, SALIDA, AMBOS }

    private final String descripcion;
    private final Sentido sentido;

    MotivoAjuste(String descripcion, Sentido sentido) {
        this.descripcion = descripcion;
        this.sentido = sentido;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Sentido getSentido() {
        return sentido;
    }

    // ¿Una cantidad (positiva suma, negativa resta) es coherente con este motivo?
    public boolean admite(int cantidad) {
        return switch (sentido) {
            case ENTRADA -> cantidad > 0;
            case SALIDA -> cantidad < 0;
            case AMBOS -> cantidad != 0;
        };
    }

    public static Optional<MotivoAjuste> desde(String nombre) {
        if (nombre == null) {
            return Optional.empty();
        }
        String limpio = nombre.trim();
        return Arrays.stream(values()).filter(m -> m.name().equals(limpio)).findFirst();
    }
}
