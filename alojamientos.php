<?php
declare(strict_types=1);

require_once __DIR__ . '/registro/inicio.php';
require_once __DIR__ . '/php/GestorCarritoAlojamiento.php';
require_once __DIR__ . '/php/Vista.php';

Sesion::iniciar();
$zona = new DateTimeZone('America/Argentina/Buenos_Aires');
$manana = new DateTimeImmutable('tomorrow', $zona);
$entrada = (string) ($_GET['entrada'] ?? $manana->format('Y-m-d'));
$salida = (string) ($_GET['salida'] ?? $manana->modify('+1 day')->format('Y-m-d'));
$errores = [];
$mensaje = Sesion::tomarMensaje();
$alojamientos = [];

try {
    $gestor = new GestorCarritoAlojamiento(Conexion::obtener());
    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        if (!Sesion::validarCsrf($_POST['token_csrf'] ?? null)) {
            $errores[] = 'La sesión del formulario venció. Volvé a intentarlo.';
        } elseif ((string) ($_POST['accion'] ?? '') === 'agregar') {
            try {
                $gestor->agregar(
                    (int) ($_POST['idAlojamiento'] ?? 0),
                    (string) ($_POST['fechaIngreso'] ?? ''),
                    (string) ($_POST['fechaEgreso'] ?? ''),
                    (int) ($_POST['cantidadPersonas'] ?? 0)
                );
                Sesion::guardarMensaje('exito', 'La estadía se agregó correctamente.');
                Utilidades::redirigir('carrito.php');
            } catch (RuntimeException $e) {
                $errores[] = $e->getMessage();
                $entrada = (string) ($_POST['fechaIngreso'] ?? $entrada);
                $salida = (string) ($_POST['fechaEgreso'] ?? $salida);
            }
        }
    }
    $alojamientos = $gestor->catalogo();
} catch (PDOException $e) {
    error_log('No se pudo cargar el catálogo de alojamientos: ' . $e->getMessage());
    $errores[] = 'No se pudieron cargar los alojamientos. Revisá la conexión con la base de datos.';
}
$usuario = Sesion::usuario();
$cantidadCarrito = isset($gestor) ? $gestor->cantidad() : 0;
?>
<!DOCTYPE html>
<html lang="es-AR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Alojamientos | El Molino</title>
    <link rel="stylesheet" href="estilos/estilos.css?v=<?= (int) filemtime(__DIR__ . '/estilos/estilos.css') ?>">
    <script src="script.js?v=<?= (int) filemtime(__DIR__ . '/script.js') ?>" defer></script>
</head>
<body class="pagina-compra">
    <header class="encabezado encabezado-compra">
        <div class="contenedor barra-navegacion">
            <?= Vista::marca() ?>
            <nav class="navegacion-compra" aria-label="Navegación de compra">
                <a href="index.php">Inicio</a>
                <a class="enlace-activo" href="alojamientos.php">Alojamientos</a>
                <a class="enlace-estadia" href="carrito.php">Mi estadía <span><?= $cantidadCarrito ?></span></a>
            </nav>
            <div class="acciones-encabezado">
                <?= Vista::accesoCuenta($usuario) ?>
            </div>
        </div>
    </header>
    <main class="contenedor contenido-compra">
        <section class="portada-alojamientos">
            <p class="texto-destacado">ALOJAMIENTOS</p>
            <h1>Elegí dónde quedarte</h1>
            <p class="introduccion-compra">Consultá las opciones disponibles y armá tu estadía con las fechas y la cantidad de personas.</p>
        </section>
        <?php if ($errores): ?><div class="mensaje mensaje-error" role="alert"><?= Utilidades::escapar(implode(' ', $errores)) ?></div><?php endif; ?>
        <?php if ($mensaje): ?><div class="mensaje mensaje-<?= Utilidades::escapar($mensaje['tipo']) ?>" role="status"><?= Utilidades::escapar($mensaje['texto']) ?></div><?php endif; ?>

        <?php if (!$alojamientos): ?>
            <section class="estado-vacio-compra" id="alojamientos-disponibles"><h2>Todavía no hay alojamientos cargados</h2><p>Cuando haya habitaciones, cabañas o departamentos disponibles, van a aparecer acá.</p></section>
        <?php else: ?>
            <div class="encabezado-packs" id="alojamientos-disponibles">
                <div><h2>Opciones disponibles</h2></div>
                <p><?= count($alojamientos) ?> <?= count($alojamientos) === 1 ? 'opción disponible' : 'opciones disponibles' ?></p>
            </div>
            <div class="grilla-productos-alojamiento">
                <?php foreach ($alojamientos as $alojamiento): ?>
                    <?php $disponible = strtolower(trim((string) $alojamiento['estado'])) === 'disponible'; ?>
                    <article class="producto-alojamiento">
                        <div class="contenido-pack-alojamiento">
                        <div class="etiquetas-pack"><span class="estado-alojamiento <?= $disponible ? 'estado-disponible' : 'estado-no-disponible' ?>"><?= $disponible ? 'Disponible' : 'No disponible' ?></span></div>
                        <h2><?= Utilidades::escapar($alojamiento['tipo']) ?> <?= Utilidades::escapar($alojamiento['numero']) ?></h2>
                        <p class="capacidad-pack">Capacidad: hasta <strong><?= (int) $alojamiento['capacidad'] ?> personas</strong></p>
                        <p class="precio-alojamiento">$<?= number_format((float) $alojamiento['precioPorNoche'], 2, ',', '.') ?> <span>por noche</span></p>
                        <?php if ($disponible): ?>
                            <form method="post" class="formulario-alojamiento">
                                <input type="hidden" name="token_csrf" value="<?= Utilidades::escapar(Sesion::tokenCsrf()) ?>">
                                <input type="hidden" name="accion" value="agregar">
                                <input type="hidden" name="idAlojamiento" value="<?= (int) $alojamiento['idAlojamiento'] ?>">
                                <label>Ingreso<input type="date" name="fechaIngreso" min="<?= $manana->format('Y-m-d') ?>" value="<?= Utilidades::escapar($entrada) ?>" required></label>
                                <label>Egreso<input type="date" name="fechaEgreso" min="<?= $manana->modify('+1 day')->format('Y-m-d') ?>" value="<?= Utilidades::escapar($salida) ?>" required></label>
                                <label>Personas<input type="number" name="cantidadPersonas" min="1" max="<?= (int) $alojamiento['capacidad'] ?>" value="1" required></label>
                                <button class="boton boton-principal" type="submit">Agregar a mi estadía</button>
                            </form>
                        <?php endif; ?>
                        </div>
                    </article>
                <?php endforeach; ?>
            </div>
        <?php endif; ?>
    </main>
</body>
</html>
