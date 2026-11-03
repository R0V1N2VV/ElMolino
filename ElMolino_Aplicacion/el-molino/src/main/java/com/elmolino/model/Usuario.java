package com.elmolino.model;

public class Usuario {

    // Estos datos representan la cuenta activa y su rol dentro del sistema.
    private int idUsuario;
    private String nombre;
    private String apellido;
    private String correo;
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
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.apellido = apellido;
        this.correo = correo;
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
     * Devuelve nombre para que otra parte del programa pueda utilizarlo.
     */
    public String getNombre() { return nombre; }
    /**
     * Actualiza nombre con la información recibida.
     */
    public void setNombre(String nombre) { this.nombre = nombre; }

    /**
     * Devuelve apellido para que otra parte del programa pueda utilizarlo.
     */
    public String getApellido() { return apellido; }
    /**
     * Actualiza apellido con la información recibida.
     */
    public void setApellido(String apellido) { this.apellido = apellido; }

    /**
     * Devuelve email para que otra parte del programa pueda utilizarlo.
     */
    public String getEmail() { return correo; }
    /**
     * Actualiza email con la información recibida.
     */
    public void setEmail(String correo) { this.correo = correo; }

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
    public String toString() {
        return nombre + " " + apellido + " (" + rol + ")";
    }
}
