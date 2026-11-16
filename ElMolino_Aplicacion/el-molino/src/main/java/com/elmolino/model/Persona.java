package com.elmolino.model;

/**
 * Abstracción de los datos comunes a las personas que usa el sistema.
 * Las subclases deciden qué información adicional mostrar en sus listas.
 */
public abstract class Persona {
    private String nombre;
    private String apellido;
    private String correo;

    protected Persona() { }

    protected Persona(String nombre, String apellido, String correo) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.correo = correo;
    }

    public final String getNombre() { return nombre; }
    public final void setNombre(String nombre) { this.nombre = nombre; }
    public final String getApellido() { return apellido; }
    public final void setApellido(String apellido) { this.apellido = apellido; }
    public final String getEmail() { return correo; }
    public final void setEmail(String correo) { this.correo = correo; }

    public final String getNombreCompleto() {
        return ((nombre == null) ? "" : nombre.trim()) + " "
                + ((apellido == null) ? "" : apellido.trim());
    }

    /** Cada tipo de persona aporta su propia representación visible. */
    public abstract String getDescripcionCorta();

    @Override
    public final String toString() { return getDescripcionCorta(); }
}
