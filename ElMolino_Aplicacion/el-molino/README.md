# El Molino - aplicación de escritorio

Aplicación de escritorio en Java Swing para la gestión del Complejo Recreativo El Molino. El proyecto usa Maven, MySQL y FlatLaf; Maven descarga las dependencias necesarias al importarlo en Eclipse.

Para entender la estructura, el inicio de sesión, los módulos y sus permisos, consultá [CODIGO_EXPLICADO.md](CODIGO_EXPLICADO.md).

## Abrir en Eclipse IDE

1. Instala un JDK 17 o superior y una versión de Eclipse IDE for Java Developers que incluya soporte Maven (m2e).
2. En Eclipse, selecciona **File > Import... > Maven > Existing Maven Projects**.
3. En **Root Directory**, elige esta carpeta (`el-molino`), selecciona el `pom.xml` y termina la importación. Espera a que Maven resuelva las dependencias.
4. En **Project > Properties > Java Compiler**, usa nivel 17 si Eclipse no lo detectó automáticamente.
5. En **Package Explorer**, abre `src/main/java`, clic derecho en `com.elmolino.Main` y selecciona **Run As > Java Application**.

No importes la carpeta contenedora `ElMolino` ni la carpeta `mvan`: el proyecto Eclipse/Maven es esta carpeta, donde está este `pom.xml`.

## Configuración de MySQL

La aplicación lee `config.properties` desde la carpeta raíz del proyecto al ejecutarse. Crea ese archivo copiando `config.properties.example` y completa los valores de conexión proporcionados por el responsable de la base de datos. En Eclipse, si la aplicación informa que no encuentra el archivo, abre **Run > Run Configurations... > Java Application > Main > Arguments > Working Directory**, selecciona **Other** y apunta a la carpeta raíz de este proyecto.

No subas `config.properties` al repositorio: contiene credenciales y ya está excluido en `.gitignore`.

## Módulos disponibles

- Clientes: consulta y edición. Las altas se realizan desde la página web.
- Alojamientos, espacios y acompañantes: consulta, edición y eliminación de registros existentes.
- Reservas: consulta, edición y cancelación; valida fechas, capacidad y cruces, y conserva el registro cancelado.
- Acompañantes por reserva: edición o eliminación de una asociación existente.
- Categorías: consulta las categorías creadas desde la página web.
- Actividades: consulta y edición desde la aplicación. La grilla muestra la categoría, los datos operativos, inscriptos frente al cupo, lugares disponibles y estado; los conteos se calculan desde `inscripcionActividad`. Las altas se realizan desde la página web.
- Inscripciones: consulta y alta desde la aplicación; comprueba el cupo y no gestiona asistencia.
- Pagos: solo consulta.
- Usuarios: gestión y asignación de roles por el administrador.

El menú muestra los módulos según el rol. Las pantallas de gestión tienen **Volver al menú**. Las relaciones entre módulos usan listas para elegir clientes, alojamientos, espacios, actividades y usuarios existentes.

El proyecto debe usar las tablas del esquema MySQL entregado para El Molino. No vuelvas a importar ni ejecutar el volcado completo sobre una base que ya contiene información.

La web y la app comparten una sola base. Las pantallas de categorías, actividades e inscripciones se actualizan automáticamente cada 10 segundos y al volver a enfocarlas; también se pueden refrescar con el botón **Actualizar**. Para que esto funcione, `config.properties` debe apuntar a la misma base configurada en `php/Conexion.php` del sitio.

Los roles `coordinador_actividades` y `COORDINADOR` se aceptan tanto en la página como en la aplicación, por lo que cualquiera de los dos permite acceder a categorías, actividades e inscripciones.

PHP y Java utilizan el mismo formato PBKDF2 para las contraseñas nuevas. Una cuenta web antigua se migra automáticamente al iniciar sesión correctamente en la página; luego puede ingresar también en la aplicación con la misma contraseña.

Para cargar un alojamiento de demostración sin duplicarlo, ejecuta `sql/ejemplo_alojamiento.sql` en la base configurada. Luego abre **Alojamientos**, selecciona `D-ALOJ-01` y pulsa **Editar** para cambiar tipo, capacidad, precio por noche o estado. Al guardar, la aplicación actualiza MySQL y vuelve a consultar la lista.

La pantalla de acceso no ofrece registro público. Las cuentas se crean desde la gestión de usuarios por el administrador. Las contraseñas nuevas se guardan con hash PBKDF2. Las cuentas antiguas en texto plano se convierten al primer inicio de sesión correcto.

## Tecnologías

- Java 17+
- Swing con FlatLaf
- Maven
- MySQL Connector/J
