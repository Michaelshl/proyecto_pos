package com.pos.venta;

// Datos de contacto del cliente que el cajero ve parcialmente (con asteriscos). Solo el administrador ve el dato
// completo. Se aplica en el servidor, no en la pantalla, para que el dato completo nunca llegue al cajero.
public final class Enmascarar {

    private static final String ASTERISCOS = "****";

    private Enmascarar() {
    }

    // ana.torres@correo.test -> an****es@correo.test (primeras y últimas letras antes de la arroba; el dominio se ve).
    public static String correo(String correo) {
        if (correo == null || correo.isBlank()) {
            return null;
        }
        String limpio = correo.trim();
        int arroba = limpio.indexOf('@');
        if (arroba < 0) {
            return limpio.length() > 2 ? limpio.substring(0, 2) + ASTERISCOS : ASTERISCOS;
        }
        String local = limpio.substring(0, arroba);
        String dominio = limpio.substring(arroba);
        if (local.length() >= 5) {
            return local.substring(0, 2) + ASTERISCOS + local.substring(local.length() - 2) + dominio;
        }
        return (local.isEmpty() ? "" : local.substring(0, 1)) + ASTERISCOS + dominio;
    }

    // 1010101010 -> 10******10 (solo los dos primeros y los dos últimos); sirve para cédulas y NIT. Con la lista de
    // resultados de una búsqueda el cajero no ve quién es el cliente hasta que abre la venta.
    public static String documento(String documento) {
        if (documento == null || documento.isBlank()) {
            return null;
        }
        String limpio = documento.trim();
        return limpio.length() > 4 ? limpio.substring(0, 2) + "******" + limpio.substring(limpio.length() - 2) : "****";
    }

    // 3150245878 -> 315******* (solo los primeros tres dígitos).
    public static String telefono(String telefono) {
        if (telefono == null || telefono.isBlank()) {
            return null;
        }
        String limpio = telefono.trim();
        return limpio.length() > 3 ? limpio.substring(0, 3) + "*******" : "***";
    }

    // Calle 10 # 5-20, Cali -> Calle 10*** (solo el comienzo de la dirección).
    public static String direccion(String direccion) {
        if (direccion == null || direccion.isBlank()) {
            return null;
        }
        String limpio = direccion.trim();
        return limpio.length() > 8 ? limpio.substring(0, 8).trim() + "***" : "***";
    }
}
