<?php
/**
 * ARCHIVO EXPLICADO: alojamientos.php
 *
 * Muestra tipos de alojamiento, disponibilidad por fechas y acciones de reserva.
 * Las funciones de este archivo están documentadas junto a su declaración.
 */

declare(strict_types=1);

require_once __DIR__ . '/registro/inicio.php';
require_once __DIR__ . '/php/GestorCarritoAlojamiento.php';
require_once __DIR__ . '/php/RelojDemostracion.php';
require_once __DIR__ . '/php/Vista.php';

Sesion::iniciar();
$zona = new DateTimeZone('America/Argentina/Buenos_Aires');
$manana = new DateTimeImmutable('tomorrow', $zona);
$entrada = (string) ($_GET['entrada'] ?? $manana->format('Y-m-d'));
$salida = (string) ($_GET['salida'] ?? $manana->modify('+1 day')->format('Y-m-d'));
$tiposAlojamiento = [
    'cabanas' => ['nombre' => 'Cabañas', 'descripcion' => 'Espacios independientes para 2, 4 o 6 personas.', 'imagen' => 'imgs/alojamiento-cabana.webp'],
    'habitaciones' => ['nombre' => 'Habitaciones y departamentos', 'descripcion' => 'Opciones dobles, familiares y grupales dentro del complejo.', 'imagen' => 'imgs/alojamiento-departamento.webp'],
    'hoteles' => ['nombre' => 'Hoteles', 'descripcion' => 'Habitaciones con servicios diarios y espacios comunes.', 'imagen' => 'imgs/alojamiento-hotel.webp'],
];
$tipoSeleccionado = (string) ($_POST['tipoSeleccionado'] ?? $_GET['tipo'] ?? '');
if (!isset($tiposAlojamiento[$tipoSeleccionado])) $tipoSeleccionado = '';
$errores = [];
$grupoConError = '';
Sesion::tomarMensaje();
$alojamientos = [];

try {
    $conexionAlojamientos = Conexion::obtener();
    $manana = RelojDemostracion::ahora($conexionAlojamientos)->setTime(0, 0)->modify('+1 day');
    $entrada = (string) ($_GET['entrada'] ?? $manana->format('Y-m-d'));
    $salida = (string) ($_GET['salida'] ?? $manana->modify('+1 day')->format('Y-m-d'));
    $gestor = new GestorCarritoAlojamiento($conexionAlojamientos);
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
                    1
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
} catch (Throwable $e) {
    error_log('No se pudo cargar el catálogo de alojamientos: ' . $e->getMessage());
    $errores[] = 'No pudimos cargar los alojamientos en este momento. Volvé a intentarlo.';
}
$alojamientosVisibles = $tipoSeleccionado === '' ? [] : array_values(array_filter(
    $alojamientos,
    // Esta función anónima procesa cada elemento recibido sin crear un método público adicional.
    static fn(array $alojamiento): bool => (string) $alojamiento['tipoClave'] === $tipoSeleccionado
));
if ($grupoConError !== '' && !array_filter(
    $alojamientosVisibles,
    // Esta función anónima procesa cada elemento recibido sin crear un método público adicional.
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
    <?= Vista::encabezadoPrincipal($usuario, '', 'alojamientos') ?>
    <main class="contenedor contenido-compra">
        <section class="portada-alojamientos">
            <p class="texto-destacado">ALOJAMIENTOS</p>
            <h1>Elegí cómo querés alojarte</h1>
            <p class="introduccion-compra">Primero elegí el tipo de alojamiento. Después vas a poder comparar capacidades, servicios y fechas. El ingreso es desde las 14:00 y el egreso hasta las 10:00.</p>
        </section>
        <?php if ($errores && $grupoConError === ''): ?><p class="aviso-estadia" role="alert"><?= Utilidades::escapar(implode(' ', $errores)) ?></p><?php endif; ?>

        <?php if ($tipoSeleccionado === ''): ?>
            <section class="selector-tipo-alojamiento" aria-labelledby="titulo-tipos-alojamiento">
                <div class="encabezado-packs"><div><h2 id="titulo-tipos-alojamiento">¿Qué estás buscando?</h2></div><p>Elegí una opción para ver los alojamientos</p></div>
                <div class="grilla-tipos-alojamiento">
                    <?php foreach ($tiposAlojamiento as $clave => $tipo): ?>
                        <a class="tarjeta-tipo-alojamiento" href="alojamientos.php?tipo=<?= rawurlencode($clave) ?>">
                            <figure class="imagen-tipo-alojamiento"><img src="<?= Utilidades::escapar($tipo['imagen']) ?>" alt="<?= Utilidades::escapar($tipo['nombre']) ?> en El Molino" loading="lazy"></figure>
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
                        <figure class="imagen-producto-alojamiento"><img src="<?= Utilidades::escapar($tiposAlojamiento[$tipoSeleccionado]['imagen']) ?>" alt="<?= Utilidades::escapar($tiposAlojamiento[$tipoSeleccionado]['nombre']) ?> en El Molino" loading="lazy"></figure>
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
                                <p class="horarios-estadia-formulario">Ingreso desde las 14:00 · egreso hasta las 10:00.</p>
                                <input type="hidden" name="cantidadPersonas" value="1">
                                <p class="ayuda-formulario">Hasta <?= (int) $alojamiento['capacidad'] ?> personas. Elegís a tus acompañantes al confirmar la reserva.</p>
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
    <footer class="pie-pagina pie-pagina-simple"><div class="contenedor pie-inferior"><span>El Molino · Complejo recreativo</span><?= Vista::contactoPie() ?></div></footer>
</body>
</html>
