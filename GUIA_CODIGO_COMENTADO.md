# Guía del código comentado — El Molino

Esta versión conserva la lógica del proyecto y agrega explicaciones en español para que sea más fácil estudiarlo. Antes de cada función propia hay un comentario que explica su responsabilidad. Las dependencias externas de `vendor/` y los archivos compilados de `target/` no se modificaron.

## Por dónde conviene empezar

1. Abrí `index.php` para ver cómo comienza la página web.
2. Seguí los archivos incluidos con `require_once`: esos archivos contienen la lógica reutilizable.
3. Mirá `php/Conexion.php` para entender cómo PHP se conecta a MySQL.
4. Revisá los archivos `Repositorio...php`: allí están la mayoría de las consultas a la base.
5. Abrí `script.js` para estudiar las interacciones que ocurren dentro del navegador.
6. Para la aplicación de escritorio, empezá por `ElMolino_Aplicacion/el-molino/src/main/java/com/elmolino/Main.java`.

## Mapa de la página web

| Ubicación | Responsabilidad |
|---|---|
| `index.php` | Portada pública del sitio. |
| `actividades.php`, `actividad.php` | Catálogo, detalle e inscripción a actividades. |
| `alojamientos.php`, `carrito.php`, `compra.php` | Disponibilidad, selección y reserva de estadías. |
| `administrar-actividades.php` | Gestión disponible para el rol coordinador. |
| `registro/` | Inicio de sesión, alta, verificación, perfil y correo. |
| `php/` | Modelos, controladores, repositorios, seguridad y utilidades. |
| `script.js` | Menús, accesibilidad, idioma, animaciones y formularios dinámicos. |
| `estilos/` | Diseño visual del sitio. |

## Cómo quedó ordenado el CSS

`estilos/estilos.css` ahora es solamente el índice de carga. Los estilos reales están en `estilos/modulos/`, numerados del 01 al 26. La numeración es importante: mantiene el orden de la cascada que tenía el archivo original.

Por ejemplo:

- `01-base-y-paleta.css`: fuente, colores y reglas generales.
- `02-navegacion.css`: cabecera y navegación.
- `13-acceso-y-registro.css`: inicio de sesión y registro.
- `14-pagina-actividades.css`: catálogo de actividades.
- `16-administracion-actividades.css`: pantalla de gestión.
- `19-catalogo-alojamientos.css` y `20-reserva-alojamientos.css`: estadías y reservas.
- `22-accesibilidad-y-animaciones.css`: idioma, modo dislexia y animaciones.
- `26-perfil-cliente.css`: perfil y sus ajustes responsive.

Si dos reglas usan el mismo selector, se aplica la que se carga más tarde o la que tiene mayor especificidad. Por eso no conviene cambiar el orden de los `@import` sin revisar primero el resultado visual.

## Mapa de la aplicación Java

| Paquete | Responsabilidad |
|---|---|
| `com.elmolino.Main` | Punto de entrada de la aplicación. |
| `com.elmolino.ui` | Ventanas, botones, tablas y eventos de la interfaz Swing. |
| `com.elmolino.dao` | Consultas SQL y operaciones de lectura, alta, modificación y baja. |
| `com.elmolino.model` | Objetos simples que representan usuarios y clientes. |
| `com.elmolino.util` | Conexión, configuración, sesión y seguridad de contraseñas. |

## Cómo leer una función comentada

El comentario que aparece inmediatamente antes de una función explica qué hace. En PHP también aparecen estas etiquetas:

- `@param`: describe un valor que entra a la función.
- `@return`: describe el valor que la función devuelve.

En Java, un método `get...` devuelve un dato del objeto y un método `set...` lo cambia. Un constructor tiene el mismo nombre que la clase y se ejecuta al crear el objeto con `new`.

## Flujo de datos básico

En la web, una página PHP recibe la solicitud, llama a un controlador o gestor y este usa un repositorio para consultar MySQL. El resultado vuelve a la página, que genera el HTML enviado al navegador.

En Java, una ventana recibe la acción del usuario, llama a un DAO y el DAO ejecuta una consulta usando `Conexion`. Luego la ventana refresca su tabla o muestra el resultado.

## Carpetas que no se comentaron

- `vendor/`: contiene PHPMailer y archivos generados por Composer. Son dependencias de terceros.
- `ElMolino_Aplicacion/el-molino/target/`: contiene resultados compilados. Se regeneran desde `src/main/java`.

Modificar esos archivos haría más difícil actualizar las dependencias y los cambios se perderían al volver a compilar.
