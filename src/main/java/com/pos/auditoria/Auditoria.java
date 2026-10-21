package com.pos.auditoria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

// Registro de lo que pasa en seguridad y de las acciones sensibles: quién, qué y sobre qué.
// Va al log normal con el nombre "AUDITORIA" (no hay tabla aparte). Nunca se registran
// contraseñas ni tokens: solo identificadores (cédula, código, número de documento) y motivos.
public final class Auditoria {

    private static final Logger log = LoggerFactory.getLogger("AUDITORIA");
    private static final int MAX_VALOR = 60;
    private static final int MAX_DETALLE = 300;

    private Auditoria() {
    }

    // Una acción hecha por el usuario autenticado de la petición actual.
    public static void accion(String accion, String detalle) {
        log.info("{} por {}: {}", accion, actorActual(), sanear(detalle, MAX_DETALLE));
    }

    // Un evento normal que no depende de un usuario autenticado (un login exitoso).
    public static void evento(String evento, String detalle) {
        log.info("{}: {}", evento, sanear(detalle, MAX_DETALLE));
    }

    // Un evento de seguridad que no es una acción de un usuario (login fallido, 401, 403).
    public static void alerta(String evento, String detalle) {
        log.warn("{}: {}", evento, sanear(detalle, MAX_DETALLE));
    }

    // La cédula del usuario autenticado, o "anónimo" si no hay sesión (login, pruebas).
    public static String actorActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            return "anónimo";
        }
        return limpiar(auth.getName());
    }

    // Un valor suelto que escribió el cliente (una cédula en el login, una ruta): se recorta y se le
    // quitan saltos de línea y caracteres de control, para que nadie pueda inventar líneas falsas en el log.
    public static String limpiar(String texto) {
        return sanear(texto, MAX_VALOR);
    }

    private static String sanear(String texto, int maximo) {
        if (texto == null) {
            return "";
        }
        String limpio = texto.replaceAll("\\p{Cntrl}", " ").trim();
        return limpio.length() > maximo ? limpio.substring(0, maximo) + "…" : limpio;
    }
}
