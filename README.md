# El Molino

Sitio del Complejo Recreativo El Molino.

## Páginas principales

- `index.php`: inicio del sitio.
- `actividades.php`: portada con carrusel, tendencias y todas las categorías.
- `categoria-actividades.php?categoria=deportes`: menú de una categoría con sus actividades y filtros.
- `actividad.php?id=futbol-recreativo`: ficha de una actividad.
- `administrar-actividades.php`: panel protegido donde el coordinador crea categorías y administra actividades.

## Actividades

La sección está desarrollada en PHP orientado a objetos. Las entidades, las fuentes de datos, el repositorio PDO, el controlador, la autorización y el servicio de imágenes se encuentran en `php/`. Las altas y modificaciones se guardan en MySQL.

El sitio incluye 6 categorías y 10 actividades predeterminadas. Si la conexión o la estructura de actividades todavía no están disponibles, el catálogo público usa automáticamente esos datos en modo de solo lectura, por lo que la página no queda vacía. El contenido es el mismo que se carga en MySQL mediante el archivo de actualización.

El archivo `BD/actualizacion_actividades.sql` amplía la base, carga las categorías y actividades existentes y permite conectarlas con PHP y MySQL. El panel valida la sesión y exige el rol `coordinador_actividades`.

## Cupos e inscripciones

Cada actividad tiene un cupo máximo y muestra cuántos lugares siguen disponibles. Un usuario con sesión iniciada puede inscribirse desde la ficha de la actividad y también cancelar su inscripción. La operación se realiza dentro de una transacción y bloquea momentáneamente la actividad para impedir que se supere el cupo si llegan varias solicitudes al mismo tiempo.

Las actividades pueden estar incluidas o ser pagas. En ambos casos se permite la inscripción; cuando es paga se muestra el precio definido por el coordinador. Esta versión no incorpora una pasarela de pago porque ese sistema no forma parte de los archivos actuales.

El inicio de sesión conserva la actividad elegida y devuelve al usuario a su ficha para que pueda completar la inscripción. La tabla utilizada es `inscripcionActividad`, incluida en la base completa y asegurada también por `BD/actualizacion_actividades.sql`.

Si `BD/actualizacion_actividades.sql` ya se ejecutó en una instalación anterior, no hay que repetirlo. Para esta entrega se debe ejecutar únicamente `BD/actualizacion_cupos_inscripciones.sql`; es una actualización separada que crea la tabla de inscripciones solo cuando hace falta.

Cuando el coordinador inicia sesión, encuentra el botón `Agregar categoría` en la sección pública de categorías y en su panel. Cada categoría también ofrece `Agregar actividad`, que abre el formulario con esa categoría ya seleccionada. Las categorías nuevas solicitan nombre, descripción e imagen.

En las páginas públicas de actividades, el coordinador ve un botón destacado `Modo edición`. En la ficha individual, ese acceso abre directamente la edición de la actividad que está mirando.

El panel permite eliminar categorías vacías. Si una categoría todavía contiene actividades, el botón queda deshabilitado hasta que esas actividades se eliminen o se muevan a otra categoría; así no se oculta contenido por accidente.

El formulario de actividades utiliza dos desplegables `Desde` / `Hasta` con intervalos de 15 minutos. La duración y el momento del día se calculan a partir del horario y no se solicitan por separado. El precio solo aparece cuando el coordinador indica que la actividad es paga; si está incluida, se guarda automáticamente en cero. Los textos "Qué llevar" e "Información importante" son opcionales y permanecen dentro de un bloque desplegable.

Después de guardar, el sistema comprueba que la actividad esté publicada y abre directamente su ficha para que el coordinador pueda verla inmediatamente. Si la base rechaza el alta, el formulario conserva los datos y muestra el problema dentro de la página.

Para que el coordinador pueda agregar, editar o eliminar actividades, es obligatorio ejecutar `BD/actualizacion_actividades.sql` una sola vez. Mientras esa actualización no esté instalada, el panel informa que el catálogo predeterminado es de solo lectura.

Para habilitar el panel en una cuenta ya verificada, asignale el rol desde MySQL:

```sql
UPDATE usuarios SET rol = 'coordinador_actividades' WHERE email = 'correo@ejemplo.com';
```

Las categorías utilizan fotografías guardadas dentro de `imgs/`. Los autores y enlaces de las imágenes incorporadas están detallados en `imgs/CREDITOS-FOTOS.txt`.

## Conexión con la aplicación Java

La página PHP y la aplicación de escritorio Java trabajan sobre la misma base MySQL. No se copian actividades entre dos sistemas: categorías, actividades, cupos e inscripciones se guardan una sola vez en las tablas `CategoriaActividad`, `Actividad` e `inscripcionActividad`.

La aplicación se configura desde `ElMolino_Aplicacion/el-molino/config.properties`. Los valores `db.host`, `db.port`, `db.name`, `db.user` y `db.password` deben identificar exactamente la misma base que utiliza `php/Conexion.php`.

En la aplicación, los módulos **Categorías**, **Actividades** e **Inscripciones** se vuelven a consultar automáticamente cada 10 segundos y también al regresar a la ventana. El botón **Actualizar** permite forzar la consulta en cualquier momento. Una actividad agregada o modificada desde la página aparece en la app sin volver a cargarla manualmente ni reiniciar el programa.

La página y la aplicación reconocen tanto el rol Java `COORDINADOR` como el rol web `coordinador_actividades` para mostrar las opciones relacionadas con actividades.

Las cuentas nuevas usan el mismo formato de contraseña PBKDF2 en PHP y Java. Las cuentas web anteriores, protegidas con el formato de PHP, se convierten automáticamente al formato compartido la próxima vez que ingresan correctamente en la página; después de ese ingreso también pueden usarse en la app Java.

## Registro e inicio de sesión

1. La base completa (`BD/s27_El_Molino_completa.sql`) ya incluye `registro_pendiente`. Si importás otra base, ejecutá `BD/registro_pendiente.sql` en `s27_El Molino`; también actualiza instalaciones anteriores de esa tabla.
2. Completá `registro/mail_config.php` con los datos del correo emisor.
3. Si tu MySQL no usa `root` sin contraseña, definí las variables de entorno `EL_MOLINO_DB_HOST`, `EL_MOLINO_DB_PUERTO`, `EL_MOLINO_DB_NOMBRE`, `EL_MOLINO_DB_USUARIO` y `EL_MOLINO_DB_CONTRASENA`.

El alta ocurre en tres pasos: datos personales, contraseña y verificación por correo. Al confirmar el código, se crean los registros de `cliente` y `usuarios` dentro de una misma transacción.
