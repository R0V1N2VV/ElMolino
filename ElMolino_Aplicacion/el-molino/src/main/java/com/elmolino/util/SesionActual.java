package com.elmolino.util;

import com.elmolino.model.Usuario;

/**
 * Guarda al usuario conectado para que las pantallas consulten su identidad y rol.
 */
public final class SesionActual {

    private static Usuario usuario;

    /**
     * Construye el objeto, guarda sus datos iniciales y deja sus componentes preparados para usarse.
     */
    private SesionActual() {
    }

    /**
     * Prepara e inicia el proceso o recurso necesario para continuar.
     */
    public static void iniciar(Usuario usuarioIniciado) {
        // Registra la cuenta que acaba de validar sus credenciales.
        usuario = usuarioIniciado;
    }

    /**
     * Devuelve el valor solicitado para que otra parte del programa pueda utilizarlo.
     */
    public static Usuario get() {
        return usuario;
    }

    /**
     * Cierra el recurso o la sesión actual y libera su estado asociado.
     */
    public static void cerrar() {
        // Borra la cuenta activa al salir de la aplicacion.
        usuario = null;
    }
}
