package com.elmolino.model;

public class Usuario {

    private int idUsuario;
    private String nombre;
    private String apellido;
    private String email;
    private String passwordHash;
    private String rol;

    public Usuario() {
    }

    public Usuario(int idUsuario, String nombre, String apellido, String email,
                   String passwordHash, String rol) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.passwordHash = passwordHash;
        this.rol = rol;
    }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    /** Uno de: ADMINISTRADOR, RECEPCION, ADMINISTRACION, COORDINADOR, GERENCIA */
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    @Override
    public String toString() {
        return nombre + " " + apellido + " (" + rol + ")";
    }
}
