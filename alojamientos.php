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
$tiposAlojamiento = [
    'cabanas' => ['nombre' => 'Cabañas', 'descripcion' => 'Espacios independientes para 2, 4 o 6 personas.'],
    'habitaciones' => ['nombre' => 'Habitaciones', 'descripcion' => 'Opciones dobles, familiares y grupales dentro del complejo.'],
    'hoteles' => ['nombre' => 'Hoteles', 'descripcion' => 'Habitaciones con servicios diarios y espacios comunes.'],
];
$tipoSeleccionado = (string) ($_POST['tipoSeleccionado'] ?? $_GET['tipo'] ?? '');
if (!isset($tiposAlojamiento[$tipoSeleccionado])) $tipoSeleccionado = '';
$errores = [];
$grupoConError = '';
Sesion::tomarMensaje();
$alojamientos = [];

try {
    $gestor = new GestorCarritoAlojamiento(Conexion::obtener());
    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        if (!Sesion::validarCsrf($_POST['token_csrf'] ?? null)) {
            $errores[] = 'La sesión del formulario venció. Volvé a intentarlo.';
        } elseif ((string) ($_POST['accion'] ?? '') === 'agregar') {
            $tipoReserva = (string) ($_POST['tipoAlojamiento'] ?? '');
            $capacidadReserva = (int) ($_POST['capacidadAlojamiento'] ?? 0);
            $grupoConError = $tipoReserva . '-' . $capacidadReserva;
            try {
                $gestor->agregarPorTipoCapacidad(
                    $tipoReserva,
                    $capacidadReserva,
                    (string) ($_POST['fechaIngreso'] ?? ''),
                    (string) ($_POST['fechaEgreso'] ?? ''),
                    (int) ($_POST['cantidadPersonas'] ?? 0)
                );
                Utilidades::redirigir('carrito.php');
            } catch (RuntimeException $e) {
                $errores[] = $e->getMessage();
                $entrada = (string) ($_POST['fechaIngreso'] ?? $entrada);
                $salida = (string) ($_POST['fechaEgreso'] ?? $salida);
            }
        }
    }
    $alojamientos = $gestor->catalogoAgrupado($entrada, $salida);
} catch (PDOException $e) {
    error_log('No se pudo cargar el catálogo de alojamientos: ' . $e->getMessage());
    $errores[] = 'No se pudieron cargar los alojamientos. Revisá la conexión con la base de datos.';
}
$alojamientosVisibles = $tipoSeleccionado === '' ? [] : array_values(array_filter(
    $alojamientos,
    static fn(array $alojamiento): bool => (string) $alojamiento['tipoClave'] === $tipoSeleccionado
));
if ($grupoConError !== '' && !array_filter(
    $alojamientosVisibles,
    static fn(array $alojamiento): bool => (string) $alojamiento['tipoClave'] . '-' . (int) $alojamiento['capacidad'] === $grupoConError
)) {
    $grupoConError = '';
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
            <h1>Elegí cómo querés alojarte</h1>
            <p class="introduccion-compra">Primero elegí el tipo de alojamiento. Después vas a poder comparar capacidades, servicios y fechas.</p>
        </section>
        <?php if ($errores && $grupoConError === ''): ?><p class="aviso-estadia" role="alert"><?= Utilidades::escapar(implode(' ', $errores)) ?></p><?php endif; ?>

        <?php if ($tipoSeleccionado === ''): ?>
            <section class="selector-tipo-alojamiento" aria-labelledby="titulo-tipos-alojamiento">
                <div class="encabezado-packs"><div><h2 id="titulo-tipos-alojamiento">¿Qué estás buscando?</h2></div><p>Elegí una opción para ver los alojamientos</p></div>
                <div class="grilla-tipos-alojamiento">
                    <?php foreach ($tiposAlojamiento as $clave => $tipo): ?>
                        <a class="tarjeta-tipo-alojamiento" href="alojamientos.php?tipo=<?= rawurlencode($clave) ?>">
                            <div><h2><?= Utilidades::escapar($tipo['nombre']) ?></h2><p><?= Utilidades::escapar($tipo['descripcion']) ?></p></div>
                            <span class="accion-tipo-alojamiento">Ver opciones <span aria-hidden="true">→</span></span>
                        </a>
                    <?php endforeach; ?>
                </div>
            </section>
        <?php else: ?>
            <nav class="navegacion-tipos-alojamiento" aria-label="Tipos de alojamiento">
                <a href="alojamientos.php">Todos los tipos</a>
                <?php foreach ($tiposAlojamiento as $clave => $tipo): ?><a href="alojamientos.php?tipo=<?= rawurlencode($clave) ?>"<?= $tipoSeleccionado === $clave ? ' class="activo" aria-current="page"' : '' ?>><?= Utilidades::escapar($tipo['nombre']) ?></a><?php endforeach; ?>
            </nav>
            <div class="encabezado-packs" id="alojamientos-disponibles">
                <div><p class="texto-destacado">TIPO DE ALOJAMIENTO</p><h2><?= Utilidades::escapar($tiposAlojamiento[$tipoSeleccionado]['nombre']) ?></h2></div>
                <p><?= count($alojamientosVisibles) ?> <?= count($alojamientosVisibles) === 1 ? 'opción' : 'opciones' ?></p>
            </div>
            <?php if (!$alojamientosVisibles): ?>
                <section class="estado-vacio-compra estado-vacio-catalogo"><h2>No hay <?= Utilidades::escapar(strtolower($tiposAlojamiento[$tipoSeleccionado]['nombre'])) ?> disponibles en este momento</h2><p>Podés volver y elegir otro tipo de alojamiento o consultar nuevamente más adelante.</p><a class="boton boton-secundario" href="alojamientos.php">Elegir otro tipo</a></section>
            <?php else: ?>
                <div class="grilla-productos-alojamiento">
                <?php foreach ($alojamientosVisibles as $alojamiento): ?>
                    <?php $detalle = GestorCarritoAlojamiento::detallesPublicos($alojamiento); ?>
                    <?php $claveGrupo = (string) $alojamiento['tipoClave'] . '-' . (int) $alojamiento['capacidad']; ?>
                    <article class="producto-alojamiento">
                        <div class="contenido-pack-alojamiento">
                        <h2><?= Utilidades::escapar($detalle['titulo']) ?></h2>
                        <p class="descripcion-alojamiento"><?= Utilidades::escapar($detalle['descripcion']) ?></p>
                        <p class="precio-alojamiento">$<?= number_format((float) $alojamiento['precioPorNoche'], 2, ',', '.') ?> <span>por noche</span></p>
                        <div class="servicios-alojamiento"><h3>Incluye</h3><ul><?php foreach ($detalle['servicios'] as $servicio): ?><li><?= Utilidades::escapar($servicio) ?></li><?php endforeach; ?></ul></div>
                        <details class="reserva-alojamiento"<?= $grupoConError === $claveGrupo ? ' open' : '' ?>>
                            <summary>Reservar</summary>
                            <form method="post" class="formulario-alojamiento">
                                <input type="hidden" name="token_csrf" value="<?= Utilidades::escapar(Sesion::tokenCsrf()) ?>">
                                <input type="hidden" name="accion" value="agregar">
                                <input type="hidden" name="tipoAlojamiento" value="<?= Utilidades::escapar($alojamiento['tipoClave']) ?>">
                                <input type="hidden" name="capacidadAlojamiento" value="<?= (int) $alojamiento['capacidad'] ?>">
                                <input type="hidden" name="tipoSeleccionado" value="<?= Utilidades::escapar($tipoSeleccionado) ?>">
                                <label>Ingreso<input type="date" name="fechaIngreso" min="<?= $manana->format('Y-m-d') ?>" value="<?= Utilidades::escapar($entrada) ?>" required></label>
                                <label>Egreso<input type="date" name="fechaEgreso" min="<?= $manana->modify('+1 day')->format('Y-m-d') ?>" value="<?= Utilidades::escapar($salida) ?>" required></label>
                                <label>Personas<input type="number" name="cantidadPersonas" min="1" max="<?= (int) $alojamiento['capacidad'] ?>" value="1" required></label>
                                <?php if ($errores && $grupoConError === $claveGrupo): ?><p class="aviso-estadia aviso-estadia-formulario" role="alert"><?= Utilidades::escapar(implode(' ', $errores)) ?></p><?php endif; ?>
                                <button class="boton boton-principal" type="submit">Agregar a mi estadía</button>
                            </form>
                        </details>
                        </div>
                    </article>
                <?php endforeach; ?>
                </div>
            <?php endif; ?>
        <?php endif; ?>
    </main>
</body>
</html>
