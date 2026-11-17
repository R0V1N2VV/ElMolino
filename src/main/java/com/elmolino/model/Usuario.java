package com.elmolino.model;

public class Usuario extends Persona {

    // Estos datos representan la cuenta activa y su rol dentro del sistema.
    private int idUsuario;
    private String hashContrasena;
    private String rol;

    /**
     * Construye el objeto, guarda sus datos iniciales y deja sus componentes preparados para usarse.
     */
    public Usuario() {
    }

    /**
     * Construye el objeto, guarda sus datos iniciales y deja sus componentes preparados para usarse.
     */
    public Usuario(int idUsuario, String nombre, String apellido, String correo,
                   String hashContrasena, String rol) {
        super(nombre, apellido, correo);
        this.idUsuario = idUsuario;
        this.hashContrasena = hashContrasena;
        this.rol = rol;
    }

    /**
     * Devuelve id usuario para que otra parte del programa pueda utilizarlo.
     */
    public int getIdUsuario() { return idUsuario; }
    /**
     * Actualiza id usuario con la información recibida.
     */
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    /**
     * Devuelve password hash para que otra parte del programa pueda utilizarlo.
     */
    public String getPasswordHash() { return hashContrasena; }
    /**
     * Actualiza password hash con la información recibida.
     */
    public void setPasswordHash(String hashContrasena) { this.hashContrasena = hashContrasena; }

    /** El rol define las secciones que puede usar la cuenta. */
    public String getRol() { return rol; }
    /**
     * Actualiza rol con la información recibida.
     */
    public void setRol(String rol) { this.rol = rol; }

    /**
     * Devuelve el texto que representa al objeto cuando se muestra en listas, tablas o mensajes.
     */
    @Override
    public String getDescripcionCorta() {
        return getNombreCompleto().trim() + " (" + rol + ")";
    }
}
