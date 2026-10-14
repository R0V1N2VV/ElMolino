<?php
declare(strict_types=1);

require_once __DIR__ . '/registro/inicio.php';
require_once __DIR__ . '/php/GestorCarritoAlojamiento.php';
require_once __DIR__ . '/php/Vista.php';

Sesion::iniciar();
$errores = [];
$mensaje = Sesion::tomarMensaje();
$elementos = [];
$totalCentavos = 0;
try {
    $gestor = new GestorCarritoAlojamiento(Conexion::obtener());
    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        if (!Sesion::validarCsrf($_POST['token_csrf'] ?? null)) {
            $errores[] = 'La sesión del formulario venció. Volvé a intentarlo.';
        } elseif ((string) ($_POST['accion'] ?? '') === 'quitar') {
            $gestor->quitar((string) ($_POST['clave'] ?? ''));
            Sesion::guardarMensaje('exito', 'Quitamos el alojamiento del carrito.');
            Utilidades::redirigir('carrito.php');
        }
    }
    $elementos = $gestor->elementos();
    $totalCentavos = GestorCarritoAlojamiento::totalEstimado($elementos);
} catch (PDOException $e) {
    error_log('No se pudo abrir el carrito de alojamientos: ' . $e->getMessage());
    $errores[] = 'No se pudo consultar el carrito. Revisá la conexión con la base de datos.';
}
$usuario = Sesion::usuario();
?>
<!DOCTYPE html>
<html lang="es-AR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Carrito | El Molino</title>
    <link rel="stylesheet" href="estilos/estilos.css?v=<?= (int) filemtime(__DIR__ . '/estilos/estilos.css') ?>">
</head>
<body class="pagina-compra">
    <header class="encabezado encabezado-compra"><div class="contenedor barra-navegacion"><a class="marca" href="index.php"><span class="simbolo-marca" aria-hidden="true">EM</span><span class="nombre-marca">El Molino<small>Complejo recreativo</small></span></a><nav class="navegacion-compra" aria-label="Navegación de compra"><a href="alojamientos.php">Alojamientos</a><a href="index.php">Inicio</a><?= Vista::accesoCuenta($usuario) ?></nav></div></header>
    <main class="contenedor contenido-compra">
        <p class="texto-destacado">TU CARRITO</p>
        <h1>Revisá tu estadía</h1>
        <?php if ($errores): ?><div class="mensaje mensaje-error" role="alert"><?= Utilidades::escapar(implode(' ', $errores)) ?></div><?php endif; ?>
        <?php if ($mensaje): ?><div class="mensaje mensaje-<?= Utilidades::escapar($mensaje['tipo']) ?>" role="status"><?= Utilidades::escapar($mensaje['texto']) ?></div><?php endif; ?>
        <?php if (!$elementos): ?>
            <section class="estado-vacio-compra"><h2>Tu carrito está vacío</h2><p>Explorá los alojamientos disponibles y agregá una estadía.</p><a class="boton boton-principal" href="alojamientos.php">Ver alojamientos</a></section>
        <?php else: ?>
            <div class="lista-carrito-alojamiento">
                <?php foreach ($elementos as $elemento): ?>
                    <article class="linea-carrito-alojamiento <?= !empty($elemento['error']) ? 'linea-con-error' : '' ?>">
                        <div><h2><?= Utilidades::escapar($elemento['tipo'] ?? 'Alojamiento') ?><?= isset($elemento['numero']) ? ' · ' . Utilidades::escapar($elemento['numero']) : '' ?></h2>
                            <?php if (empty($elemento['error'])): ?>
                                <p><?= Utilidades::escapar($elemento['fechaIngreso']) ?> al <?= Utilidades::escapar($elemento['fechaEgreso']) ?> · <?= (int) $elemento['noches'] ?> <?= (int) $elemento['noches'] === 1 ? 'noche' : 'noches' ?> · <?= (int) $elemento['personas'] ?> personas</p>
                                <p>$<?= number_format((float) $elemento['precioPorNoche'], 2, ',', '.') ?> por noche</p>
                                <strong>Subtotal: $<?= number_format((float) $elemento['subtotal'], 2, ',', '.') ?></strong>
                            <?php else: ?><p class="mensaje-linea-carrito"><?= Utilidades::escapar($elemento['error']) ?></p><?php endif; ?>
                        </div>
                        <form method="post"><input type="hidden" name="token_csrf" value="<?= Utilidades::escapar(Sesion::tokenCsrf()) ?>"><input type="hidden" name="accion" value="quitar"><input type="hidden" name="clave" value="<?= Utilidades::escapar($elemento['clave']) ?>"><button class="boton-enlace" type="submit">Quitar</button></form>
                    </article>
                <?php endforeach; ?>
            </div>
            <div class="resumen-carrito-alojamiento"><span>Total estimado</span><strong>$<?= number_format($totalCentavos / 100, 2, ',', '.') ?></strong><p>El total final se confirma con la disponibilidad al realizar el pedido.</p></div>
            <div class="acciones-carrito"><a class="boton boton-secundario" href="alojamientos.php">Seguir eligiendo</a><?php if ($totalCentavos > 0 && !array_filter($elementos, static fn(array $item): bool => !empty($item['error']))): ?><a class="boton boton-principal" href="compra.php">Continuar con la compra</a><?php endif; ?></div>
        <?php endif; ?>
    </main>
</body>
</html>
