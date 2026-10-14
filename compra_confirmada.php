<?php
declare(strict_types=1);

require_once __DIR__ . '/registro/inicio.php';
require_once __DIR__ . '/php/GestorCarritoAlojamiento.php';
require_once __DIR__ . '/php/Vista.php';

Sesion::iniciar();
$usuario = Sesion::usuario();
if (!$usuario) {
    Sesion::guardar('continuar_compra', true);
    Utilidades::redirigir('registro/login.php');
}
$idPedido = (int) ($_GET['id'] ?? 0);
$idCliente = (int) ($usuario['id_cliente'] ?? 0);
if ($idCliente <= 0) {
    $consultaCliente = Conexion::obtener()->prepare('SELECT idCliente FROM cliente WHERE email = :email LIMIT 1');
    $consultaCliente->execute(['email' => (string) $usuario['email']]);
    $idCliente = (int) ($consultaCliente->fetchColumn() ?: 0);
}
$gestor = new GestorCarritoAlojamiento(Conexion::obtener());
$pedido = $gestor->obtenerPedido($idPedido, $idCliente);
if (!$pedido) {
    http_response_code(404);
    exit('No encontramos ese pedido en tu cuenta.');
}

$errores = [];
$correoEnviado = (bool) Sesion::obtener('pedido_correo_' . $idPedido, true);
$mensaje = (string) Sesion::obtener('pedido_mensaje_' . $idPedido, 'El pedido quedó registrado.');
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if (!Sesion::validarCsrf($_POST['token_csrf'] ?? null)) {
        $errores[] = 'La sesión del formulario venció. Volvé a intentarlo.';
    } else {
        try {
            (new Correo())->enviarResumenCompra($pedido);
            $correoEnviado = true;
            $mensaje = 'Te reenviamos el resumen del pedido.';
            Sesion::guardar('pedido_correo_' . $idPedido, true);
            Sesion::guardar('pedido_mensaje_' . $idPedido, $mensaje);
        } catch (Throwable $e) {
            error_log('No se pudo reenviar el resumen del pedido ' . $idPedido . ': ' . $e->getMessage());
            $correoEnviado = false;
            $errores[] = 'No se pudo enviar el correo. Verificá la configuración del correo emisor e intentá nuevamente.';
        }
    }
}
?>
<!DOCTYPE html>
<html lang="es-AR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Pedido recibido | El Molino</title>
    <link rel="stylesheet" href="estilos/estilos.css?v=<?= (int) filemtime(__DIR__ . '/estilos/estilos.css') ?>">
</head>
<body class="pagina-compra">
    <header class="encabezado encabezado-compra"><div class="contenedor barra-navegacion"><?= Vista::marca() ?><nav class="navegacion-compra" aria-label="Navegación de compra"><a href="index.php">Inicio</a><a href="alojamientos.php">Alojamientos</a></nav></div></header>
    <main class="contenedor contenido-compra">
        <p class="texto-destacado">PEDIDO #<?= (int) $pedido['idPedido'] ?></p>
        <h1>Recibimos tu pedido</h1>
        <?php if ($errores): ?><div class="mensaje mensaje-error" role="alert"><?= Utilidades::escapar(implode(' ', $errores)) ?></div><?php endif; ?>
        <div class="mensaje <?= $correoEnviado ? 'mensaje-exito' : 'mensaje-error' ?>" role="status"><?= Utilidades::escapar($mensaje) ?></div>
        <p>Estado del pedido: <strong><?= Utilidades::escapar($pedido['estadoPedido']) ?></strong> · Estado del pago: <strong><?= Utilidades::escapar($pedido['estadoPago']) ?></strong></p>
        <p>Forma de pago elegida: <strong><?= Utilidades::escapar($pedido['metodoPago']) ?></strong>. No se realizó ningún cobro en línea.</p>
        <section class="resumen-compra">
            <?php foreach ($pedido['lineas'] as $linea): ?>
                <article class="linea-resumen-compra"><div><h2><?= Utilidades::escapar($linea['alojamientoTipo']) ?> · <?= Utilidades::escapar($linea['alojamientoNumero']) ?></h2><p><?= Utilidades::escapar($linea['fechaIngreso']) ?> al <?= Utilidades::escapar($linea['fechaEgreso']) ?> · <?= (int) $linea['noches'] ?> noches · <?= (int) $linea['cantidadPersonas'] ?> personas</p><p>$<?= number_format((float) $linea['precioPorNoche'], 2, ',', '.') ?> por noche</p><strong>Subtotal: $<?= number_format((float) $linea['subtotal'], 2, ',', '.') ?></strong></div></article>
            <?php endforeach; ?>
            <div class="resumen-carrito-alojamiento"><span>Total</span><strong>$<?= number_format((float) $pedido['total'], 2, ',', '.') ?></strong></div>
        </section>
        <?php if (!$correoEnviado): ?><form method="post" class="formulario-reenvio"><input type="hidden" name="token_csrf" value="<?= Utilidades::escapar(Sesion::tokenCsrf()) ?>"><button class="boton boton-secundario" type="submit">Reenviar resumen por correo</button></form><?php endif; ?>
        <a class="boton boton-principal" href="index.php">Volver al inicio</a>
    </main>
</body>
</html>
