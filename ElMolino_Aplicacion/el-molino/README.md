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

No compartas `config.properties`: contiene credenciales. Está excluido en `.gitignore`; para publicar o enviar el proyecto, incluí solo `config.properties.example`. Si ya compartiste una copia con credenciales, cambiá la contraseña de la base de datos.

### Habilitar el primer SuperAdmin

No hace falta agregar columnas a `alojamientos`: la aplicación usa el campo `estado` existente. Para que la columna de roles acepte `SUPERADMIN`, debe ser `VARCHAR(32)` o un tipo que incluya ese valor.

Para habilitar el primer SuperAdmin, crea desde la aplicación una cuenta con rol `ADMINISTRADOR`, reemplaza el correo de ejemplo en `sql/promover_superadmin_inicial.sql` y ejecuta ese `UPDATE` en MySQL. Luego inicia sesión con esa cuenta: desde **Usuarios** podrá crear usuarios con los roles disponibles, pero no podrá asignar `SUPERADMIN`.

## Módulos disponibles

- Clientes: consulta y edición. Las altas se realizan desde la página web.
- Alojamientos y acompañantes: consulta, edición y eliminación de registros existentes.
- Reservas: consulta, edición y cancelación; valida fechas, capacidad, acompañantes asociados y cruces, y conserva el registro cancelado.
- Acompañantes por reserva: edición o eliminación de una asociación existente.
- Actividades: consulta y edición desde la aplicación. La grilla muestra datos operativos y la ocupación de la fecha con más participantes; el cálculo incluye titulares y acompañantes. Las altas se realizan desde la página web.
- Inscripciones: consulta y alta desde la aplicación; muestra clientes y actividades por nombre, comprueba el cupo por fecha y evita duplicar inscripciones activas del mismo cliente.
- Pagos: solo consulta para los roles habituales; SuperAdmin puede gestionarlos.
- Usuarios: gestión por el administrador. Desde la aplicación solo se pueden crear cuentas con rol `ADMINISTRADOR`; `SUPERADMIN` no se puede crear ni asignar desde el formulario.

El menú muestra los módulos según el rol y ya no incluye Espacios Recreativos. `SUPERADMIN` puede acceder a los informes y a todos los módulos, pero no puede crear ni asignar el rol `SUPERADMIN` desde la aplicación. `RECEPCION` puede cambiar el estado de un alojamiento entre `Disponible`, `Reservado` y `Fuera de servicio`. `LIMPIEZA` puede ver todos los tipos de alojamiento y cambiar entre `En limpieza` y `Disponible`; no puede modificar alojamientos que estén `Reservados` o en `Fuera de servicio`. Las pantallas de gestión tienen **Volver al menú**. Las relaciones muestran nombres legibles de clientes, actividades y alojamientos.

En **Registro de auditoría**, el botón **Volver este cambio** puede revertir ediciones de clientes, alojamientos, actividades, inscripciones, acompañantes y reservas, siempre que el registro no haya cambiado después. Las altas, eliminaciones y cancelaciones requieren operaciones específicas y no se revierten desde ese botón.

El proyecto debe usar las tablas del esquema MySQL entregado para El Molino. No vuelvas a importar ni ejecutar el volcado completo sobre una base que ya contiene información.

Para cargar un alojamiento de demostración sin duplicarlo, ejecuta `sql/ejemplo_alojamiento.sql` en la base configurada. Luego abre **Alojamientos**, selecciona `D-ALOJ-01` y pulsa **Editar** para cambiar tipo, capacidad, precio por noche o estado. Al guardar, la aplicación actualiza MySQL y vuelve a consultar la lista.

La pantalla de acceso no ofrece registro público. Las cuentas se crean desde la gestión de usuarios por el administrador. Las contraseñas nuevas se guardan con hash PBKDF2. Las cuentas antiguas en texto plano se convierten al primer inicio de sesión correcto.

## Tecnologías

- Java 17+
- Swing con FlatLaf
- Maven
- MySQL Connector/J
