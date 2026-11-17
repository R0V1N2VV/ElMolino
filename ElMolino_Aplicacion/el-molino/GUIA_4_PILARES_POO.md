# Los cuatro pilares de POO en El Molino

La aplicación Java no agrega clases de ejemplo que no se usen: los cuatro
pilares se aplican en el código que conecta la interfaz con la base de datos.

## 1. Encapsulamiento

`model/Persona.java`, `Cliente.java` y `Usuario.java` mantienen sus atributos
como `private`. Otras clases no modifican los datos directamente: usan getters
y setters, por ejemplo `cliente.getEmail()` o `usuario.setRol(...)`.

Esto protege el estado de los objetos y concentra el acceso a sus datos.

## 2. Abstracción

`Persona` es una clase `abstract`: representa lo que comparten las personas
del sistema, sin permitir crear una persona incompleta. También `BaseDAO<T>`
es abstracta y reúne el mecanismo común de consulta JDBC.

Cada DAO concreto solo indica cómo convertir una fila de MySQL en su objeto.

## 3. Herencia

`Cliente extends Persona` y `Usuario extends Persona`. Ambos heredan nombre,
apellido, correo y `getNombreCompleto()`, pero cada uno conserva sus propios
datos: DNI y teléfono en Cliente; rol y contraseña cifrada en Usuario.

`ClienteDAO`, `UsuarioDAO` y `ReservaAcompananteDAO` también heredan de
`BaseDAO<T>`.

## 4. Polimorfismo

`Persona` exige `getDescripcionCorta()`. Cliente y Usuario lo implementan de
forma diferente. Como `toString()` de Persona llama a ese método, Java elige
automáticamente la versión correcta según el objeto real.

En los DAO ocurre lo mismo: `BaseDAO` llama a `mapear(...)`, pero al ejecutar
la consulta Java usa la implementación de `ClienteDAO`, `UsuarioDAO` o
`ReservaAcompananteDAO`.
