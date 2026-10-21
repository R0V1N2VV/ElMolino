package com.elmolino.util;

import com.elmolino.model.Usuario;

/**
 * Guarda al usuario conectado para que las pantallas consulten su identidad y rol.
 */
public final class SesionActual {

    private static Usuario usuario;

    private SesionActual() {
    }

    public static void iniciar(Usuario usuarioIniciado) {
        // Registra la cuenta que acaba de validar sus credenciales.
        usuario = usuarioIniciado;
    }

    public static Usuario get() {
        return usuario;
    }

    public static void cerrar() {
        // Borra la cuenta activa al salir de la aplicacion.
        usuario = null;
    }
}
