package com.elmolino.model;

public class Usuario {

    // Estos datos representan la cuenta activa y su rol dentro del sistema.
    private int idUsuario;
    private String nombre;
    private String apellido;
    private String correo;
    private String hashContrasena;
    private String rol;

    public Usuario() {
    }

    public Usuario(int idUsuario, String nombre, String apellido, String correo,
                   String hashContrasena, String rol) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.apellido = apellido;
        this.correo = correo;
        this.hashContrasena = hashContrasena;
        this.rol = rol;
    }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getEmail() { return correo; }
    public void setEmail(String correo) { this.correo = correo; }

    public String getPasswordHash() { return hashContrasena; }
    public void setPasswordHash(String hashContrasena) { this.hashContrasena = hashContrasena; }

    /** El rol define las secciones que puede usar la cuenta. */
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    @Override
    public String toString() {
        return nombre + " " + apellido + " (" + rol + ")";
    }
}
