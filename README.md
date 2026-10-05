# El Molino

Sitio del Complejo Recreativo El Molino.

## Páginas principales

- `index.php`: inicio del sitio.
- `actividades.php`: portada con carrusel, tendencias y todas las categorías.
- `categoria-actividades.php?categoria=deportes`: menú de una categoría con sus actividades y filtros.
- `actividad.php?id=futbol-recreativo`: ficha de una actividad.
- `administrar-actividades.php`: panel protegido del coordinador de actividades.

## Actividades

La sección está desarrollada en PHP orientado a objetos. Las entidades, las fuentes de datos, el repositorio PDO, el controlador, la autorización y el servicio de imágenes se encuentran en `php/`. Las altas y modificaciones se guardan en MySQL.

El sitio incluye 6 categorías y 10 actividades predeterminadas. Si la conexión o la estructura de actividades todavía no están disponibles, el catálogo público usa automáticamente esos datos en modo de solo lectura, por lo que la página no queda vacía. El contenido es el mismo que se carga en MySQL mediante el archivo de actualización.

El archivo `BD/actualizacion_actividades.sql` amplía la base, carga las categorías y actividades existentes y permite conectarlas con PHP y MySQL. El panel valida la sesión y exige el rol `coordinador_actividades`.

Para que el coordinador pueda agregar, editar o eliminar actividades, es obligatorio ejecutar `BD/actualizacion_actividades.sql` una sola vez. Mientras esa actualización no esté instalada, el panel informa que el catálogo predeterminado es de solo lectura.

Para habilitar el panel en una cuenta ya verificada, asignale el rol desde MySQL:

```sql
UPDATE usuarios SET rol = 'coordinador_actividades' WHERE email = 'correo@ejemplo.com';
```

Las categorías utilizan fotografías guardadas dentro de `imgs/`. Los autores y enlaces de las imágenes incorporadas están detallados en `imgs/CREDITOS-FOTOS.txt`.

## Registro e inicio de sesión

1. La base completa (`BD/s27_El_Molino_completa.sql`) ya incluye `registro_pendiente`. Si importás otra base, ejecutá `BD/registro_pendiente.sql` en `s27_El Molino`; también actualiza instalaciones anteriores de esa tabla.
2. Completá `registro/mail_config.php` con los datos del correo emisor.
3. Si tu MySQL no usa `root` sin contraseña, definí las variables de entorno `EL_MOLINO_DB_HOST`, `EL_MOLINO_DB_PUERTO`, `EL_MOLINO_DB_NOMBRE`, `EL_MOLINO_DB_USUARIO` y `EL_MOLINO_DB_CONTRASENA`.

El alta ocurre en tres pasos: datos personales, contraseña y verificación por correo. Al confirmar el código, se crean los registros de `cliente` y `usuarios` dentro de una misma transacción.
