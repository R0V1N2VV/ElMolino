# Los cuatro pilares de POO en El Molino

La aplicación Java no agrega clases de ejemplo que no se usen: los cuatro
pilares se aplican en el código que conecta la interfaz con la base de datos.

## 1. Encapsulamiento

`model/Persona.java`, `Cliente.java` y `Usuario.java` mantienen sus atributos
como `private`. Otras clases no modifican los datos directamente: usan getters
y setters, por ejemplo `cliente.getEmail()` o `usuario.setRol(...)`.

Esto protege el estado de los objetos y concentra el acceso a sus datos.

Código real de `model/Persona.java`:

```java
public abstract class Persona {
    private String nombre;
    private String apellido;
    private String correo;

    public final String getNombre() { return nombre; }
    public final void setNombre(String nombre) { this.nombre = nombre; }
}
```

Los atributos no pueden modificarse desde cualquier clase con
`persona.nombre = ...`; deben pasar por sus métodos públicos.

## 2. Abstracción

`Persona` es una clase `abstract`: representa lo que comparten las personas
del sistema, sin permitir crear una persona incompleta. También `BaseDAO<T>`
es abstracta y reúne el mecanismo común de consulta JDBC.

Cada DAO concreto solo indica cómo convertir una fila de MySQL en su objeto.

Código real de `model/Persona.java`:

```java
public abstract class Persona {
    public abstract String getDescripcionCorta();
}
```

`Persona` no representa una persona específica: reúne lo esencial que
comparten Cliente y Usuario. `BaseDAO<T>` hace lo mismo con el trabajo común
de los DAO, sin decidir qué entidad concreta se está leyendo.

## 3. Herencia

`Cliente extends Persona` y `Usuario extends Persona`. Ambos heredan nombre,
apellido, correo y `getNombreCompleto()`, pero cada uno conserva sus propios
datos: DNI y teléfono en Cliente; rol y contraseña cifrada en Usuario.

`ClienteDAO`, `UsuarioDAO` y `ReservaAcompananteDAO` también heredan de
`BaseDAO<T>`.

Código real de `model/Cliente.java`:

```java
public class Cliente extends Persona {
    private int idCliente;
    private int dni;
    private String telefono;

    public Cliente(int idCliente, int dni, String nombre, String apellido,
                   String telefono, String correo, String direccion) {
        super(nombre, apellido, correo);
        this.idCliente = idCliente;
    }
}
```

## 4. Polimorfismo

`Persona` exige `getDescripcionCorta()`. Cliente y Usuario lo implementan de
forma diferente. Como `toString()` de Persona llama a ese método, Java elige
automáticamente la versión correcta según el objeto real.

En los DAO ocurre lo mismo: `BaseDAO` llama a `mapear(...)`, pero al ejecutar
la consulta Java usa la implementación de `ClienteDAO`, `UsuarioDAO` o
`ReservaAcompananteDAO`.

Código real de `model/Cliente.java` y `model/Usuario.java`:

```java
// Cliente
@Override
public String getDescripcionCorta() {
    return getApellido() + ", " + getNombre() + " (DNI " + dni + ")";
}

// Usuario
@Override
public String getDescripcionCorta() {
    return getNombreCompleto().trim() + " (" + rol + ")";
}
```

Cuando una variable se trata como `Persona`, Java ejecuta automáticamente la
versión de `getDescripcionCorta()` que corresponda al objeto real. Esa es la
parte clave del polimorfismo.

## Dónde mostrarlo en la entrega

1. Abrí `src/main/java/com/elmolino/model/Persona.java`: abstracción y
   encapsulamiento.
2. Abrí `Cliente.java` y `Usuario.java`: herencia y polimorfismo.
3. Abrí `dao/BaseDAO.java`: abstracción y polimorfismo aplicados a la conexión
   con la base de datos.
4. Abrí `dao/ClienteDAO.java`: muestra cómo una clase hija implementa
   `mapear(...)` para convertir una fila SQL en un `Cliente`.

No son ejemplos sueltos: esas clases se utilizan al iniciar sesión, consultar
clientes y trabajar con los datos de la aplicación.
