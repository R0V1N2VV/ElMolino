# Guía de archivos de la página y de la base de datos

Esta guía explica **para qué sirve cada archivo importante**. No explica cada línea de código: la idea es que puedas ubicarte, saber dónde buscar y explicar el proyecto con palabras simples.

## 1. Cómo trabaja la página

Cuando una persona abre una página, por ejemplo `actividad.php`, ese archivo prepara la pantalla y llama a clases de la carpeta `php/`. Esas clases validan lo que pidió el usuario y consultan MySQL. Con el resultado, el archivo PHP genera el HTML que ve el navegador.

El recorrido general es:

1. La página recibe una acción del usuario.
2. Un controlador o gestor revisa los datos y aplica las reglas.
3. Un repositorio ejecuta consultas en la base de datos.
4. La página muestra el resultado.

## 2. Archivos principales de la página

| Archivo | Qué hace |
|---|---|
| `index.php` | Es la portada pública. Muestra la presentación de El Molino, alojamientos, actividades, espacios, navegación, acceso a la cuenta y cantidad de elementos de “Mi estadía”. |
| `actividades.php` | Es la portada de actividades. Carga las categorías y las actividades destacadas o en tendencia. |
| `categoria-actividades.php` | Muestra las actividades de una categoría. Recibe filtros como búsqueda, edad, momento del día, modalidad y orden. |
| `actividad.php` | Muestra todos los datos de una actividad. También permite elegir una fecha, seleccionar titular y acompañantes, inscribirse, modificar la inscripción o cancelarla. Solo ofrece fechas dentro de una estadía con la seña acreditada. |
| `administrar-actividades.php` | Es el panel del coordinador. Permite crear, editar y eliminar categorías y actividades. Verifica el rol antes de mostrar la gestión. |
| `alojamientos.php` | Muestra cabañas, habitaciones y hoteles agrupados por capacidad. Consulta fechas, disponibilidad y permite agregar una opción a “Mi estadía”. |
| `carrito.php` | Muestra los alojamientos elegidos antes de reservar. Permite revisar fechas, cantidad de personas, importes y quitar elementos. Todavía no bloquea definitivamente una unidad. |
| `compra.php` | Es el último paso de la reserva. Vuelve a comprobar disponibilidad, asigna una unidad física, permite elegir acompañantes, calcula el total y la seña y crea el pedido en la base. |
| `compra_confirmada.php` | Muestra el pedido recién creado, el monto de la seña, el vencimiento y los datos para transferir. El cliente puede informar el número de operación para que administración lo verifique. |
| `script.js` | Contiene el comportamiento que ocurre en el navegador: menú móvil, menú del usuario, modo dislexia, idioma, animaciones, flechas para bajar, formularios y confirmación de actividades pagas. No consulta directamente MySQL. |

## 3. Archivos de la carpeta `php/`

### Conexión, sesión y seguridad

| Archivo | Qué hace |
|---|---|
| `php/Conexion.php` | Abre la conexión de PHP con MySQL usando PDO. Lee servidor, puerto, nombre de base, usuario y contraseña desde variables de entorno o valores de configuración. |
| `php/Sesion.php` | Inicia y administra la sesión del navegador. Guarda el usuario conectado, el carrito, mensajes temporales y el token CSRF que protege los formularios. |
| `php/Autenticacion.php` | Comprueba correo y contraseña durante el inicio de sesión. Busca el usuario en la base, verifica el hash de la contraseña y recupera su rol y su identificador de cliente. |
| `php/Autorizacion.php` | Decide si un usuario tiene permiso para una acción. Por ejemplo, impide que una cuenta común abra la gestión de actividades. |
| `php/SeguridadPassword.php` | Crea y verifica hashes seguros de contraseñas compatibles entre PHP y la aplicación Java. Nunca guarda la contraseña original. |
| `php/Utilidades.php` | Reúne funciones pequeñas usadas en distintas páginas: escapar texto, redirigir, generar códigos y otras tareas comunes. |

### Actividades

| Archivo | Qué hace |
|---|---|
| `php/Actividad.php` | Representa una actividad como objeto. Guarda sus datos y sabe calcular qué fechas son válidas según los días configurados. |
| `php/CategoriaActividad.php` | Representa una categoría de actividades, por ejemplo Deportes, Talleres o Naturaleza. |
| `php/FuenteActividades.php` | Define qué operaciones debe tener cualquier fuente de actividades. Funciona como un contrato para que el controlador pueda trabajar con MySQL o con un catálogo alternativo. |
| `php/CatalogoActividadesPredeterminadas.php` | Contiene actividades de respaldo. Se usa en modo de solo lectura si la tabla de actividades todavía no está disponible. |
| `php/RepositorioActividades.php` | Realiza las consultas SQL de categorías y actividades: listar, buscar, filtrar, crear, modificar, eliminar y calcular ocupación por fecha. |
| `php/ControladorActividades.php` | Recibe lo solicitado por las páginas, valida filtros y formularios y coordina al repositorio y al gestor de imágenes. |
| `php/RepositorioInscripciones.php` | Guarda y cancela inscripciones. Controla cupos por fecha, comprueba la estadía confirmada y valida que los acompañantes estén hospedados durante el día elegido. |
| `php/ControladorInscripciones.php` | Interpreta el formulario de inscripción y decide si debe guardar participantes, cambiar la fecha o cancelar la inscripción. |
| `php/GestorImagenActividad.php` | Valida y guarda las imágenes subidas para categorías o actividades. También evita nombres de archivo inseguros. |
| `php/inicio_actividades.php` | Carga todas las clases necesarias para las páginas de actividades y crea los objetos principales. Si la base falla, activa el catálogo predeterminado. |

### Alojamientos, reservas y pagos

| Archivo | Qué hace |
|---|---|
| `php/GestorCarritoAlojamiento.php` | Es la lógica principal de alojamientos. Consulta disponibilidad, agrupa unidades, administra “Mi estadía”, calcula precios, crea pedidos, reservas y asociaciones de acompañantes. |
| `php/GestorReservasCliente.php` | Permite modificar los acompañantes de una reserva y cancelar una estadía antes del ingreso. También marca si corresponde gestionar un reintegro. |
| `php/ConfiguracionPagos.php` | Lee de forma segura el titular, alias, CBU y CUIT que se muestran para pagar la seña, desde el archivo local o desde variables de entorno. |
| `php/config_pago.php` | Archivo sencillo para completar los datos bancarios en una instalación local. Si se usan variables de entorno, tienen prioridad. |
| `php/GestorPagosSena.php` | Registra que el cliente informó una transferencia. Crea un pago pendiente con el monto de la seña y el número de operación. La acreditación final se hace desde Java. |

### Cuenta y presentación visual

| Archivo | Qué hace |
|---|---|
| `php/GestorAcompanantesCliente.php` | Permite agregar y quitar acompañantes después de crear la cuenta. Al quitar uno deja de mostrarlo y lo retira de reservas y actividades futuras, pero conserva el historial. |
| `php/Vista.php` | Genera fragmentos visuales reutilizables, por ejemplo la marca, el menú de usuario, tarjetas de actividades y textos de precios. |

## 4. Archivos de la carpeta `registro/`

| Archivo | Qué hace |
|---|---|
| `registro/inicio.php` | Carga las clases básicas que necesitan el registro, el inicio de sesión, la cuenta y los correos. |
| `registro/login.php` | Muestra y procesa el formulario de inicio de sesión. Si las credenciales son correctas, guarda el usuario en la sesión. |
| `registro/login.html` | Es una redirección de compatibilidad hacia `login.php`. No contiene la lógica real del inicio de sesión. |
| `registro/registro.php` | Muestra el registro por pasos: datos personales y acompañantes, contraseña y envío del código de verificación. |
| `registro/registro.html` | Es una redirección de compatibilidad hacia `registro.php`. |
| `registro/gestor_registro.php` | Valida todos los datos del alta. Guarda temporalmente el registro, verifica el código y finalmente crea cliente, usuario y acompañantes en una transacción. |
| `registro/verificar.php` | Muestra y procesa el código recibido por correo para terminar el registro. |
| `registro/reenviar_codigo.php` | Genera y envía un nuevo código cuando el anterior venció o no llegó. |
| `registro/cuenta.php` | Es el perfil del cliente. Muestra actividades, alojamientos, estado de la seña y acompañantes. Permite informar el pago, agregar o quitar acompañantes, modificar huéspedes y cancelar reservas. |
| `registro/cerrar_sesion.php` | Valida la solicitud y elimina la sesión del usuario. |
| `registro/mailer.php` | Prepara y envía los correos de verificación y los resúmenes de reservas mediante PHPMailer. |
| `registro/mail_config.php` | Guarda la configuración del servidor de correo. Debe protegerse porque puede contener la contraseña de aplicación del correo emisor. |

## 5. Archivos de estilos

`estilos/estilos.css` es el índice general. No contiene todo el diseño: importa los módulos de `estilos/modulos/` en un orden fijo.

| Archivo | Qué diseña |
|---|---|
| `01-base-y-paleta.css` | Colores, tipografías, reinicio básico y componentes generales. |
| `02-navegacion.css` | Cabecera, barra de navegación y menú móvil. |
| `03-portada.css` | Portada principal y su imagen destacada. |
| `04-nosotros.css` | Sección institucional “Quiénes somos”. |
| `05-alojamientos-inicio.css` | Resumen de alojamientos que aparece en la portada. |
| `06-actividades-inicio.css` | Resumen de actividades que aparece en la portada. |
| `07-espacios.css` | Tarjetas y presentación de espacios recreativos. |
| `08-visita.css` | Información práctica para planificar una visita. |
| `09-pie-y-cierre.css` | Pie de página y cierre visual de las pantallas. |
| `10-responsive-tablet.css` | Ajustes para pantallas medianas y tablets. |
| `11-responsive-movil.css` | Ajustes para teléfonos. |
| `12-cuenta-y-estadia.css` | Menú de cuenta y elementos iniciales de “Mi estadía”. |
| `13-acceso-y-registro.css` | Formularios de acceso, registro y verificación. |
| `14-pagina-actividades.css` | Portada, filtros y listados de actividades. |
| `15-detalle-actividad.css` | Ficha de una actividad, participantes, fecha y requisito de estadía. |
| `16-administracion-actividades.css` | Panel y formularios del coordinador. |
| `17-categorias-actividades.css` | Tarjetas, fotografías y composición de categorías. |
| `18-ajustes-formularios.css` | Correcciones finales de los formularios de actividades. |
| `19-catalogo-alojamientos.css` | Selector de cabañas, habitaciones y hoteles. |
| `20-reserva-alojamientos.css` | Carrito, confirmación, acompañantes y pago de la seña. |
| `21-marca-y-desplazamiento.css` | Logo, marca y botones para bajar a la siguiente sección. |
| `22-accesibilidad-y-animaciones.css` | Modo dislexia, idioma y animaciones suaves. |
| `23-estados-alojamiento.css` | Estados de disponibilidad y avisos discretos. |
| `24-ajustes-visuales.css` | Pequeños retoques para que la página se vea más natural. |
| `25-mensajes-discretos.css` | Evita los carteles verdes o rojos grandes y convierte los mensajes en textos más simples. |
| `26-perfil-cliente.css` | Diseño de Mi cuenta, reservas, pago y administración de acompañantes. |

## 6. Carpetas que no contienen lógica propia de la página

| Carpeta o archivo | Para qué sirve |
|---|---|
| `imgs/` | Guarda logos, fotografías, íconos y las imágenes subidas para actividades. |
| `vendor/` | Contiene PHPMailer y archivos generados por Composer. Son dependencias externas y no conviene modificarlos. |
| `ElMolino_Aplicacion/` | Contiene la aplicación de escritorio Java. Usa la misma base, pero no forma parte de la página web. |

# Base de datos

## 7. Cómo se conecta la página

La conexión web está en `php/Conexion.php`. Ese archivo crea un objeto PDO y apunta a la base `s27_El Molino`. Las páginas no deberían escribir consultas por su cuenta cuando ya existe un repositorio o gestor para hacerlo.

La aplicación Java usa JDBC desde `com.elmolino.util.Conexion` y lee sus datos desde `ElMolino_Aplicacion/el-molino/config.properties`. Para que web y Java reflejen lo mismo, ambos deben usar el mismo servidor y el mismo nombre de base.

## 8. Qué guarda cada tabla

| Tabla | Qué guarda |
|---|---|
| `cliente` | Datos personales del huésped: DNI, nombre, apellido, teléfono, correo y dirección. Es la persona que puede reservar. |
| `usuarios` | Datos necesarios para iniciar sesión: correo, hash de contraseña y rol. Se relaciona con `cliente` por el mismo correo. |
| `registro_pendiente` | Datos temporales mientras la persona todavía no confirmó el código enviado por correo. Después de verificar, se elimina. |
| `acompañante` | Personas vinculadas a un cliente. El campo `activo` indica si todavía deben aparecer en la cuenta y en nuevas selecciones. |
| `alojamientos` | Unidades físicas disponibles: número interno, tipo, capacidad, precio, descripción y estado. |
| `reservas` | Una unidad asignada a un cliente durante fechas concretas. Guarda cantidad de huéspedes, precio y estado de la reserva. |
| `pedido_alojamiento` | Cabecera de una compra. Agrupa una o más reservas y guarda total, método de pago, porcentaje y monto de seña, vencimiento y estados. |
| `detalle_pedido_alojamiento` | Une el pedido con cada reserva y conserva el detalle utilizado al comprar: fechas, noches, tipo y subtotal. |
| `pago` | Pagos asociados a reservas. Diferencia seña, saldo o reintegro y permite usar estados `PENDIENTE`, `CONFIRMADO` o `ANULADO`. |
| `reservaAcompañante` | Tabla intermedia que indica qué acompañantes se hospedan en cada reserva. |
| `EspacioRecreativo` | Espacios físicos del complejo, con ubicación, horarios y si requieren reserva. |
| `CategoriaActividad` | Categorías usadas para ordenar las actividades. |
| `Actividad` | Información completa de cada actividad: categoría, horarios, días, cupo por fecha, precio, modalidad, responsable e imagen. |
| `inscripcionActividad` | Inscripción principal de un cliente a una actividad. Guarda la fecha elegida y si participa el titular. |
| `inscripcionActividadAcompanante` | Acompañantes incluidos en la inscripción principal. Cada fila ocupa un lugar del cupo. |

## 9. Relaciones más importantes

- Un `cliente` puede tener muchos `acompañante`.
- Un `cliente` puede tener muchos `pedido_alojamiento`.
- Un pedido puede contener varias `reservas` mediante `detalle_pedido_alojamiento`.
- Cada reserva usa una unidad de `alojamientos`.
- Una reserva puede tener varios acompañantes mediante `reservaAcompañante`.
- Una `CategoriaActividad` puede tener muchas actividades.
- Un cliente puede tener una inscripción por actividad; la fecha elegida está en `inscripcionActividad`.
- Los acompañantes de esa inscripción están en `inscripcionActividadAcompanante`.
- Los pagos se asocian a una reserva; a través del detalle se sabe a qué pedido pertenecen.

## 10. Estados principales

### Reserva

- `PENDIENTE_SENA`: fue solicitada, pero el pago todavía no está acreditado.
- `CONFIRMADA`: administración confirmó la seña.
- `CANCELADA`: fue cancelada antes de la estadía.
- `FINALIZADA`: la estadía terminó.

### Pago del pedido

- `SENA_PENDIENTE`: todavía no se informó una transferencia.
- `SENA_INFORMADA`: el cliente informó el número de operación y espera verificación.
- `SENA_PAGADA`: administración confirmó la seña.
- `PAGADO`: el pedido está completamente pagado.
- `REEMBOLSO_PENDIENTE`: se canceló una reserva que tenía dinero acreditado.

### Pago individual

- `PENDIENTE`: está informado, pero todavía no se verificó.
- `CONFIRMADO`: administración comprobó el pago.
- `ANULADO`: el registro no debe contarse como dinero recibido.

## 11. Cómo funciona la seña

1. `compra.php` crea el pedido y calcula una seña del 30 %.
2. El cliente transfiere usando los datos mostrados por la página.
3. `GestorPagosSena.php` guarda el número de operación como pago `PENDIENTE`.
4. Administración abre Pagos en la aplicación Java y cambia ese pago a `CONFIRMADO`.
5. Java suma las señas confirmadas del pedido.
6. Si alcanza el monto pedido, cambia el pedido a `SENA_PAGADA` y las reservas a `CONFIRMADA`.
7. Recién entonces la web permite elegir actividades dentro de esas fechas.

## 12. Archivos SQL

| Archivo | Cuándo usarlo |
|---|---|
| `BD/s27_El_Molino_completa.sql` | Para crear una instalación nueva o vacía. Incluye la estructura completa y el catálogo inicial de alojamientos. |
| `BD/00_ACTUALIZAR_BASE_EXISTENTE.sql` | Es el archivo que se debe ejecutar sobre una base que ya está en uso. Agrega solamente las tablas y columnas que faltan y puede volver a ejecutarse. |
| `BD/actualizacion_catalogo_alojamientos.sql` | Carga las múltiples unidades de cabañas, habitaciones y hoteles si todavía no existen. |
| `BD/actualizacion_actividades.sql` | Migración histórica de actividades. Ya quedó reemplazada por la actualización unificada. |
| `BD/actualizacion_cupos_inscripciones.sql` | Migración histórica del sistema de cupos. No hace falta ejecutarla si se usa la actualización unificada. |
| `BD/actualizacion_acompanantes_actividades.sql` | Migración histórica para acompañantes en actividades. |
| `BD/actualizacion_compras_alojamiento.sql` | Migración histórica de pedidos de alojamiento. |
| `BD/actualizacion_registro_acompanantes.sql` | Migración histórica del registro con acompañantes. |
| `BD/registro_pendiente.sql` | Crea la tabla temporal del registro si se instala esa parte por separado. |
| `BD/s27_El_Molino (1).sql` | Volcado original de la base. Se conserva como referencia, pero no es el archivo recomendado para una instalación actual. |
| `BD/sql` | Archivo antiguo sin contenido útil. No se ejecuta. |

## 13. Qué archivo SQL tenés que ejecutar

- Si ya tenés información cargada: hacé una copia de seguridad y ejecutá `BD/00_ACTUALIZAR_BASE_EXISTENTE.sql`.
- Si la base está completamente vacía: ejecutá `BD/s27_El_Molino_completa.sql`.
- No ejecutes todos los archivos históricos uno detrás de otro, porque repiten cambios que ya reúne la actualización unificada.
# Actualización de lógica de actividades

Antes de probar esta versión sobre una base que ya existe, ejecutar una vez:

`BD/04_ACTUALIZAR_LOGICA_ACTIVIDADES.sql`

Esta actualización agrega el historial de inscripciones, mantiene activos o
cancelados a los acompañantes sin borrar datos y crea `cargo_actividad`. La
tabla nueva conserva el precio aceptado, la cantidad de participantes y el
importe pendiente que deberá cobrarse al finalizar la estadía.

Después de ejecutar el SQL, tanto la web como Java usan esas columnas. No se
debe probar el código nuevo contra una base sin actualizar porque las consultas
esperan encontrar `estado`, `activo` y `cargo_actividad`.

