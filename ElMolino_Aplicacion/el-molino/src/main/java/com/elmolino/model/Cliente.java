package com.elmolino.model;

public class Cliente {

    // Guarda los datos que usa la tabla de clientes y sus formularios.
    private int idCliente;
    private int dni;
    private String nombre;
    private String apellido;
    private String telefono;
    private String correo;
    private String direccion;

    /**
     * Construye el objeto, guarda sus datos iniciales y deja sus componentes preparados para usarse.
     */
    public Cliente() {
    }

    /**
     * Construye el objeto, guarda sus datos iniciales y deja sus componentes preparados para usarse.
     */
    public Cliente(int idCliente, int dni, String nombre, String apellido,
                   String telefono, String correo, String direccion) {
        this.idCliente = idCliente;
        this.dni = dni;
        this.nombre = nombre;
        this.apellido = apellido;
        this.telefono = telefono;
        this.correo = correo;
        this.direccion = direccion;
    }

    /**
     * Devuelve id cliente para que otra parte del programa pueda utilizarlo.
     */
    public int getIdCliente() { return idCliente; }
    /**
     * Actualiza id cliente con la información recibida.
     */
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    /**
     * Devuelve dni para que otra parte del programa pueda utilizarlo.
     */
    public int getDni() { return dni; }
    /**
     * Actualiza dni con la información recibida.
     */
    public void setDni(int dni) { this.dni = dni; }

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
     * Devuelve telefono para que otra parte del programa pueda utilizarlo.
     */
    public String getTelefono() { return telefono; }
    /**
     * Actualiza telefono con la información recibida.
     */
    public void setTelefono(String telefono) { this.telefono = telefono; }

    /**
     * Devuelve email para que otra parte del programa pueda utilizarlo.
     */
    public String getEmail() { return correo; }
    /**
     * Actualiza email con la información recibida.
     */
    public void setEmail(String correo) { this.correo = correo; }

    /**
     * Devuelve direccion para que otra parte del programa pueda utilizarlo.
     */
    public String getDireccion() { return direccion; }
    /**
     * Actualiza direccion con la información recibida.
     */
    public void setDireccion(String direccion) { this.direccion = direccion; }

    /**
     * Devuelve el texto que representa al objeto cuando se muestra en listas, tablas o mensajes.
     */
    @Override
    public String toString() {
        return apellido + ", " + nombre + " (DNI " + dni + ")";
    }
}
