package com.elmolino.model;

public class Cliente {

    private int idCliente;
    private int dni;
    private String nombre;
    private String apellido;
    private String telefono;
    private String email;
    private String direccion;

    public Cliente() {
    }

    public Cliente(int idCliente, int dni, String nombre, String apellido,
                   String telefono, String email, String direccion) {
        this.idCliente = idCliente;
        this.dni = dni;
        this.nombre = nombre;
        this.apellido = apellido;
        this.telefono = telefono;
        this.email = email;
        this.direccion = direccion;
    }

    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    public int getDni() { return dni; }
    public void setDni(int dni) { this.dni = dni; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    @Override
    public String toString() {
        return apellido + ", " + nombre + " (DNI " + dni + ")";
    }
}
