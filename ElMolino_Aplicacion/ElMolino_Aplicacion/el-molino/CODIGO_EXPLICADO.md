# Guía del código de El Molino

Esta guía explica cómo se organiza la aplicación y dónde encontrar cada parte. Está hecha en Java y se conecta a una base MySQL. La página web se usa para cargar la mayoría de los datos; la aplicación sirve para consultarlos y gestionarlos.

## 1. Recorrido general

Al abrir la aplicación:

1. `Main` inicia la pantalla de ingreso.
2. `VentanaLogin` busca al usuario y comprueba su contraseña.
3. `SesionActual` guarda al usuario y su rol mientras usa la app.
4. `VentanaPrincipal` muestra las secciones disponibles para ese rol.
5. Al abrir una sección, un DAO lee o cambia los datos en MySQL.

Las consultas se ejecutan en segundo plano para que la ventana siga respondiendo.

## 2. Carpetas y clases principales

El código está dentro de `src/main/java/com/elmolino`.

| Archivo o carpeta | Para qué sirve |
| --- | --- |
| `Main.java` | Inicia la aplicación y abre el ingreso. |
| `ui/` | Contiene las ventanas y los formularios. |
| `dao/` | Contiene el código que consulta o cambia la base de datos. |
| `model/` | Define objetos como clientes y usuarios. |
| `util/Conexion.java` | Abre la conexión con MySQL. |
| `util/Config.java` | Lee los datos de conexión. |
| `util/SesionActual.java` | Guarda quién inició sesión. |
| `util/SeguridadPassword.java` | Protege y comprueba las contraseñas. |
| `pom.xml` | Define Java, Maven y las librerías necesarias. |

### Ventanas

- `VentanaLogin`: pantalla para ingresar.
- `VentanaPrincipal`: menú principal, con accesos según el rol.
- `VentanaClientes`: muestra y permite editar clientes.
- `VentanaGestion`: ventana compartida por alojamientos, reservas, pagos, actividades, usuarios y otros módulos.
- `VentanaAcompanantesReserva`: permite editar o quitar acompañantes de una reserva.
- `VentanaInformes`: ventana antigua de informes; ya no aparece en el menú.

## 3. Permisos y acciones disponibles

Cada rol ve distintas secciones en el menú.

| Sección | Qué se puede hacer en la app |
| --- | --- |
| Clientes | Consultar y editar. Se agregan desde la web. |
| Alojamientos, espacios y acompañantes | Consultar, editar y eliminar. No se agregan desde la app. |
| Reservas | Consultar, editar y cancelar. |
| Acompañantes por reserva | Editar o quitar una asociación. |
| Pagos | Solo consultar. |
| Actividades | Consultar y editar datos operativos, cupo, precio y disponibilidad calculada desde las inscripciones. Las altas se realizan desde la web. |
| Inscripciones | Consultar y agregar. La asistencia no se cambia desde esta pantalla. |
| Usuarios | El administrador puede crear usuarios y asignar roles. |
| Informes | Ya no aparecen en el menú. |

Los accesos dependen del rol: Recepción, Administración, Coordinación, Administrador o Gerencia. Gerencia no tiene secciones visibles por ahora, porque se quitó el acceso a informes.

## 4. Cómo se configura un módulo

En `GestionDAO` se define cada módulo: su tabla de MySQL y los campos que usa. `VentanaGestion` lee esa definición para armar la tabla y los formularios.

Para cambiar los campos de una sección, buscá su definición en `GestionDAO`. Para cambiar el menú o quién ve cada sección, revisá `VentanaPrincipal` y `VentanaGestion`.

Al guardar, el programa revisa que los datos tengan el formato correcto. Por ejemplo, valida las fechas de las reservas y el cupo de las actividades.

## 5. Conexión con MySQL

La aplicación necesita un archivo llamado `config.properties` en su carpeta de ejecución. Tiene que incluir los datos de conexión:

```properties
db.host=servidor
db.port=3306
db.name=nombre_de_base
db.user=usuario
db.password=contraseña
```

Usá `config.properties.example` como plantilla y completalo con los datos que te dieron. No compartas el archivo real: contiene la contraseña de la base.

## 6. Ejecutar desde Eclipse

1. Importá como proyecto Maven la carpeta que contiene `pom.xml`.
2. Usá JDK 17 o superior y esperá a que Maven cargue las librerías.
3. En `src/main/java`, abrí `com.elmolino.Main` y elegí **Run As → Java Application**.
4. Si no encuentra `config.properties`, configurá el **Working Directory** para que apunte a la carpeta raíz del proyecto.

También podés ejecutar desde esa carpeta con:

```text
mvn compile exec:java
```

## 7. Cambios y límites

- El código de la página web no está en esta carpeta. Por eso, esta guía solo describe la aplicación Java.
- La base puede conservar datos como horarios o duración aunque ya no aparezcan en la aplicación.
- La web agrega clientes, actividades y reservas. La app permite gestionarlos. Las excepciones son las inscripciones y los usuarios creados por un administrador.
