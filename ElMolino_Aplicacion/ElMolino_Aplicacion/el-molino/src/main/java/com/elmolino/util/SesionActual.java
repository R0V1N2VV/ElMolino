package com.elmolino.util;

import com.elmolino.model.Usuario;

/**
 * Guarda el usuario que inicio sesion para que cualquier pantalla
 * pueda consultar quien esta usando la aplicacion y con que rol.
 */
public final class SesionActual {

    private static Usuario usuario;

    private SesionActual() {
    }

    public static void iniciar(Usuario u) {
        usuario = u;
    }

    public static Usuario get() {
        return usuario;
    }

    public static void cerrar() {
        usuario = null;
    }
}
