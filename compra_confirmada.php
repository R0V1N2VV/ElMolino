<?php
/**
 * ARCHIVO EXPLICADO: compra_confirmada.php
 *
 * Muestra el resumen final de una reserva confirmada.
 * Las funciones de este archivo están documentadas junto a su declaración.
 */

declare(strict_types=1);

require_once __DIR__ . '/registro/inicio.php';
require_once __DIR__ . '/php/GestorCarritoAlojamiento.php';
require_once __DIR__ . '/php/GestorPagosSena.php';
require_once __DIR__ . '/php/ConfiguracionPagos.php';
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
$configuracionPago = ConfiguracionPagos::transferencia();
$correoEnviado = (bool) Sesion::obtener('pedido_correo_' . $idPedido, true);
$mensaje = (string) Sesion::obtener('pedido_mensaje_' . $idPedido, 'El pedido quedó registrado.');
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if (!Sesion::validarCsrf($_POST['token_csrf'] ?? null)) {
        $errores[] = 'La sesión del formulario venció. Volvé a intentarlo.';
    } else {
        try {
            $accion = (string) ($_POST['accion'] ?? '');
            if ($accion === 'informar_pago_sena') {
                if (!$configuracionPago['configurada']) {
                    throw new RuntimeException('Los datos bancarios todavía no están configurados. Contactá a recepción.');
                }
                (new GestorPagosSena(Conexion::obtener()))->informarTransferencia(
                    $idPedido,
                    $idCliente,
                    (string) ($_POST['referencia_pago'] ?? ''),
                    (string) ($_POST['monto_transferido'] ?? ''),
                    is_array($_FILES['comprobante_archivo'] ?? null) ? $_FILES['comprobante_archivo'] : []
                );
                $pedido = $gestor->obtenerPedido($idPedido, $idCliente);
                $mensaje = 'Registramos la transferencia. Administración debe verificarla para confirmar la estadía.';
            } elseif ($accion === 'reenviar_correo') {
                (new Correo())->enviarResumenCompra($pedido);
                $correoEnviado = true;
                $mensaje = 'Te reenviamos el resumen del pedido.';
                Sesion::guardar('pedido_correo_' . $idPedido, true);
                Sesion::guardar('pedido_mensaje_' . $idPedido, $mensaje);
            } else {
                throw new RuntimeException('La acción solicitada no es válida.');
            }
        } catch (Throwable $e) {
            error_log('No se pudo procesar el pedido ' . $idPedido . ': ' . $e->getMessage());
            $errores[] = $e instanceof PDOException
                ? 'No se pudo registrar el pago. Ejecutá la actualización SQL y volvé a intentarlo.'
                : (($e instanceof RuntimeException || $e instanceof InvalidArgumentException)
                    ? $e->getMessage()
                    : 'No se pudo procesar la solicitud. Volvé a intentarlo.');
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
    <?= Vista::encabezadoPrincipal($usuario, '', 'estadía') ?>
    <main class="contenedor contenido-compra">
        <p class="texto-destacado">PEDIDO #<?= (int) $pedido['idPedido'] ?></p>
        <h1>Recibimos tu solicitud de reserva</h1>
        <?php if ($errores): ?><p class="aviso-estadia" role="alert"><?= Utilidades::escapar(implode(' ', $errores)) ?></p><?php endif; ?>
        <p class="estado-pedido-correo" role="status"><?= Utilidades::escapar($mensaje) ?></p>
        <p>Estado del pedido: <strong><?= Utilidades::escapar($pedido['estadoPedido']) ?></strong> · Estado del pago: <strong><?= Utilidades::escapar($pedido['estadoPago']) ?></strong></p>
        <?php if ((float) ($pedido['montoPagado'] ?? 0) > 0): ?><p>Pagado: <strong>$<?= number_format((float) $pedido['montoPagado'], 2, ',', '.') ?></strong> · Saldo pendiente: <strong>$<?= number_format(max(0, (float) $pedido['total'] - (float) $pedido['montoPagado']), 2, ',', '.') ?></strong></p><?php endif; ?>
        <p>Forma elegida para la seña: <strong><?= Utilidades::escapar($pedido['metodoPago']) ?></strong>. La reserva se confirma cuando se acredita el pago.</p>
        <section class="resumen-sena resumen-sena-confirmada">
            <div><span>Seña requerida (<?= (int) $pedido['porcentajeSena'] ?>%)</span><small>Tu pre-reserva se retiene hasta el <?= Utilidades::escapar((new DateTimeImmutable((string) $pedido['fechaLimiteSena']))->format('d/m/Y H:i')) ?>. Si no informás la seña, se libera automáticamente.</small></div>
            <strong>$<?= number_format((float) $pedido['montoSena'], 2, ',', '.') ?></strong>
        </section>
        <?php $estadoPago = strtoupper((string) $pedido['estadoPago']); ?>
        <?php if (!in_array($estadoPago, ['SENA_PAGADA', 'PAGADO'], true)): ?>
            <section class="pasos-pago-sena" aria-labelledby="titulo-pago-sena">
                <p class="texto-destacado">CONFIRMAR ESTADÍA</p>
                <h2 id="titulo-pago-sena">Pagá la seña por transferencia</h2>
                <?php if ($estadoPago === 'SENA_INFORMADA'): ?>
                    <p>Ya informaste la transferencia y el alojamiento quedó retenido mientras Recepción la revisa. La revisión vence el <?= Utilidades::escapar((new DateTimeImmutable((string) $pedido['fechaLimiteRevisionSena']))->format('d/m/Y H:i')) ?>.</p>
                <?php elseif ($configuracionPago['configurada']): ?>
                    <ol><li>Transferí exactamente <strong>$<?= number_format((float) $pedido['montoSena'], 2, ',', '.') ?></strong>.</li><li>Guardá el número de operación.</li><li>Informalo debajo para que administración pueda comprobarlo.</li></ol>
                    <dl class="datos-transferencia">
                        <div><dt>Titular</dt><dd><?= Utilidades::escapar($configuracionPago['titular']) ?></dd></div>
                        <?php if ($configuracionPago['alias'] !== ''): ?><div><dt>Alias</dt><dd><?= Utilidades::escapar($configuracionPago['alias']) ?></dd></div><?php endif; ?>
                        <?php if ($configuracionPago['cbu'] !== ''): ?><div><dt>CBU</dt><dd><?= Utilidades::escapar($configuracionPago['cbu']) ?></dd></div><?php endif; ?>
                        <?php if ($configuracionPago['cuit'] !== ''): ?><div><dt>CUIT</dt><dd><?= Utilidades::escapar($configuracionPago['cuit']) ?></dd></div><?php endif; ?>
                    </dl>
                    <form method="post" enctype="multipart/form-data" class="formulario-informar-sena">
                        <input type="hidden" name="token_csrf" value="<?= Utilidades::escapar(Sesion::tokenCsrf()) ?>">
                        <input type="hidden" name="accion" value="informar_pago_sena">
                        <label>Importe transferido<input type="number" name="monto_transferido" min="<?= number_format(max(0.01, (float) $pedido['montoSena'] - (float) ($pedido['montoPagado'] ?? 0)), 2, '.', '') ?>" step="0.01" value="<?= number_format(max(0.01, (float) $pedido['montoSena'] - (float) ($pedido['montoPagado'] ?? 0)), 2, '.', '') ?>" required></label>
                        <label>Número de operación o referencia<input type="text" name="referencia_pago" minlength="4" maxlength="150" required></label>
                        <label>Comprobante (imagen o PDF, hasta 5 MB)<input type="file" name="comprobante_archivo" accept="image/jpeg,image/png,image/webp,application/pdf" required></label>
                        <button class="boton boton-principal" type="submit">Ya transferí la seña</button>
                    </form>
                <?php else: ?>
                    <p>Los datos bancarios todavía no están configurados. Contactá a recepción antes de realizar el pago.</p>
                <?php endif; ?>
            </section>
        <?php else: ?>
            <p class="estado-pago-acreditado"><strong>Seña acreditada.</strong> Tu estadía está confirmada y ya podés elegir actividades para esas fechas.</p>
        <?php endif; ?>
        <section class="resumen-compra">
            <?php foreach ($pedido['lineas'] as $linea): ?>
                <article class="linea-resumen-compra"><div><h2><?= Utilidades::escapar($linea['alojamientoTipo']) ?></h2><p><?= Utilidades::escapar($linea['fechaIngreso']) ?> al <?= Utilidades::escapar($linea['fechaEgreso']) ?> · <?= (int) $linea['noches'] ?> noches · <?= (int) $linea['cantidadPersonas'] ?> personas</p><?php if ($linea['acompanantes']): ?><p><strong>Acompañantes:</strong> <?= Utilidades::escapar(implode(', ', $linea['acompanantes'])) ?></p><?php endif; ?><p>$<?= number_format((float) $linea['precioPorNoche'], 2, ',', '.') ?> por noche</p><strong>Subtotal: $<?= number_format((float) $linea['subtotal'], 2, ',', '.') ?></strong></div></article>
            <?php endforeach; ?>
            <div class="resumen-carrito-alojamiento"><span>Total</span><strong>$<?= number_format((float) $pedido['total'], 2, ',', '.') ?></strong></div>
        </section>
        <?php if (!$correoEnviado): ?><form method="post" class="formulario-reenvio"><input type="hidden" name="token_csrf" value="<?= Utilidades::escapar(Sesion::tokenCsrf()) ?>"><input type="hidden" name="accion" value="reenviar_correo"><button class="boton boton-secundario" type="submit">Reenviar resumen por correo</button></form><?php endif; ?>
        <a class="boton boton-principal" href="index.php">Volver al inicio</a>
    </main>
    <footer class="pie-pagina pie-pagina-simple"><div class="contenedor pie-inferior"><span>El Molino · Complejo recreativo</span><?= Vista::contactoPie() ?></div></footer>
</body>
</html>
