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

La aplicación lee `config.properties` desde la carpeta raíz del proyecto al ejecutarse. Esta entrega ya incluye ese archivo configurado para la base de El Molino. En Eclipse, si la aplicación informa que no encuentra el archivo, abre **Run > Run Configurations... > Java Application > Main > Arguments > Working Directory**, selecciona **Other** y apunta a la carpeta raíz de este proyecto.

No subas `config.properties` a GitHub: contiene credenciales. Está excluido en `.gitignore`; para publicar el repositorio, incluí solo `config.properties.example`. La copia incluida en esta entrega es únicamente para ejecutar el proyecto localmente.

### Reloj para una demostración

Después de importar `BD/13_RELOJ_DEMOSTRACION.sql`, la cuenta web con rol `SUPERADMIN` puede entrar a **Mi cuenta → Preparar demostración**. Ahí se selecciona una fecha y una hora compartidas por la web y la aplicación Java. La app las consulta al realizar cada operación, sin editar `config.properties` ni reiniciar.

Usalo solo al exponer: elegí el día de ingreso desde las 14:00 para probar la llegada, y el día de egreso desde las 10:00 para probar el cobro final, el egreso y limpieza. Al terminar, usá **Finalizar demostración** para volver al reloj real.

### Habilitar el primer SuperAdmin

No hace falta agregar columnas a `alojamientos`: la aplicación usa el campo `estado` existente. Para que la columna de roles acepte `SUPERADMIN`, debe ser `VARCHAR(32)` o un tipo que incluya ese valor.

Para habilitar el primer SuperAdmin, crea desde la aplicación una cuenta con rol `ADMINISTRADOR`, reemplaza el correo de ejemplo en `sql/promover_superadmin_inicial.sql` y ejecuta ese `UPDATE` en MySQL. Luego inicia sesión con esa cuenta: desde **Usuarios** podrá crear y asignar todos los roles, incluido `SUPERADMIN`. El administrador común solamente puede crear administradores.

## Módulos disponibles

- Clientes: consulta y edición. Las altas se realizan desde la página web.
- Alojamientos y acompañantes: consulta, edición y eliminación de registros existentes.
- Reservas: consulta, edición y cancelación; valida fechas, capacidad, acompañantes asociados y cruces, y conserva el registro cancelado.
- Acompañantes por reserva: edición o eliminación de una asociación existente.
- Actividades: Coordinación puede crear, editar y dar de baja actividades. La grilla muestra datos operativos y la ocupación de la fecha con más participantes; el cálculo incluye titulares y acompañantes. Al darla de baja se conserva el historial y se cancelan las inscripciones futuras y cargos pendientes.
- Inscripciones: consulta y alta desde la aplicación; muestra clientes y actividades por nombre, comprueba el cupo por fecha y evita duplicar inscripciones activas del mismo cliente.
- Pagos: solo consulta para los roles habituales; SuperAdmin puede gestionarlos.
- Usuarios: gestión por Administrador y SuperAdmin. El SuperAdmin puede asignar cualquier rol; el Administrador común solo puede crear cuentas Administrador y no puede modificar cuentas SuperAdmin.

El menú muestra los módulos según el rol y ya no incluye Espacios Recreativos. `SUPERADMIN` puede acceder a todos los módulos y asignar cualquier rol. `RECEPCION` registra llegadas, puede habilitar un alojamiento o ponerlo en mantenimiento/fuera de servicio e indica el motivo. Al cerrar una estadía, el sistema pasa la unidad automáticamente a **En limpieza**. `LIMPIEZA` ve solamente esa cola y, cuando termina su trabajo, usa **Finalizar limpieza** para marcar la unidad como `Disponible`. Cada cambio queda en el Registro de auditoría. Las pantallas de gestión tienen **Volver al menú** y las relaciones muestran nombres legibles.

## Mejoras operativas recientes

- La aplicación y la web rechazan actividades del mismo cliente que se superponen en horarios expresados como `HH:MM a HH:MM`.
- La web aplica el mismo control a cada acompañante seleccionado, para que tampoco pueda superponer dos actividades.
- Si una actividad no tiene cupo, la web registra al cliente en una lista de espera que aparece en **Mi cuenta** y en los informes de la app. Importá `BD/12_MEJORAS_OPERATIVAS.sql` en una base existente.
- La web cierra sesiones inactivas después de 30 minutos y limita los intentos fallidos de inicio de sesión en esa sesión.
- Los cuatro pilares de POO están explicados con código real en [GUIA_4_PILARES_POO.md](GUIA_4_PILARES_POO.md).

En **Registro de auditoría**, el botón **Volver este cambio** puede revertir ediciones de clientes, alojamientos, actividades, inscripciones, acompañantes y reservas, siempre que el registro no haya cambiado después. Las altas, eliminaciones y cancelaciones requieren operaciones específicas y no se revierten desde ese botón.

El proyecto debe usar las tablas del esquema MySQL entregado para El Molino. No vuelvas a importar ni ejecutar el volcado completo sobre una base que ya contiene información.

Para cargar un alojamiento de demostración sin duplicarlo, ejecuta `sql/ejemplo_alojamiento.sql` en la base configurada. Luego abre **Alojamientos**, selecciona `D-ALOJ-01` y pulsa **Editar** para cambiar tipo, capacidad, precio por noche o estado. Al guardar, la aplicación actualiza MySQL y vuelve a consultar la lista.

La pantalla de acceso no ofrece registro público. Las cuentas se crean desde la gestión de usuarios por el administrador. Las contraseñas nuevas se guardan con hash PBKDF2. Las cuentas antiguas en texto plano se convierten al primer inicio de sesión correcto.

## Tecnologías

- Java 17+
- Swing con FlatLaf
- Maven
- MySQL Connector/J
