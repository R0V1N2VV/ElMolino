<?php
/**
 * ARCHIVO EXPLICADO: compra.php
 *
 * Valida la compra de estadías y crea el pedido correspondiente.
 * Las funciones de este archivo están documentadas junto a su declaración.
 */

declare(strict_types=1);

require_once __DIR__ . '/registro/inicio.php';
require_once __DIR__ . '/php/GestorCarritoAlojamiento.php';
require_once __DIR__ . '/php/Vista.php';

Sesion::iniciar();
$usuario = Sesion::usuario();
$errores = [];
$gestor = null;
$elementos = [];
$acompanantesCliente = [];
$idCliente = 0;
try {
    $gestor = new GestorCarritoAlojamiento(Conexion::obtener());
    $elementos = $gestor->elementos();
} catch (PDOException $e) {
    error_log('No se pudo cargar el resumen de compra: ' . $e->getMessage());
    $errores[] = 'No se pudo consultar el resumen de la estadía. Revisá la conexión con la base de datos.';
}
if ($gestor && !$elementos) Utilidades::redirigir('carrito.php');

// Esta función anónima procesa cada elemento recibido sin crear un método público adicional.
$elementosConError = array_filter($elementos, static fn(array $item): bool => !empty($item['error']));
$totalCentavos = GestorCarritoAlojamiento::totalEstimado($elementos);

if (!$usuario) {
    Sesion::guardar('continuar_compra', true);
} elseif ($gestor) {
    $idCliente = (int) ($usuario['id_cliente'] ?? 0);
    if ($idCliente <= 0) {
        $consultaCliente = Conexion::obtener()->prepare('SELECT idCliente FROM cliente WHERE email = :email LIMIT 1');
        $consultaCliente->execute(['email' => (string) $usuario['email']]);
        $idCliente = (int) ($consultaCliente->fetchColumn() ?: 0);
    }
    if ($idCliente > 0) $acompanantesCliente = $gestor->listarAcompanantesCliente($idCliente);
}

if ($usuario && $gestor && $_SERVER['REQUEST_METHOD'] === 'POST') {
    if (!Sesion::validarCsrf($_POST['token_csrf'] ?? null)) {
        $errores[] = 'La sesión del formulario venció. Volvé a intentarlo.';
    } elseif ($elementosConError) {
        $errores[] = 'Hay alojamientos de tu estadía que ya no están disponibles. Revisalos antes de continuar.';
    } else {
        try {
            $acompanantesPorItem = is_array($_POST['acompanantes'] ?? null) ? $_POST['acompanantes'] : [];
            $idPedido = $gestor->crearPedido($usuario, (string) ($_POST['metodoPago'] ?? ''), $acompanantesPorItem);
            $pedido = $gestor->obtenerPedido($idPedido, $idCliente);
            $correoEnviado = false;
            try {
                if (!$pedido) throw new RuntimeException('No se pudo recuperar el resumen del pedido.');
                (new Correo())->enviarResumenCompra($pedido);
                $correoEnviado = true;
            } catch (Throwable $e) {
                error_log('Pedido ' . $idPedido . ' guardado; no se pudo enviar su resumen: ' . $e->getMessage());
            }
            Sesion::guardar('pedido_correo_' . $idPedido, $correoEnviado);
            Sesion::guardar('pedido_mensaje_' . $idPedido, $correoEnviado
                ? 'El pedido quedó registrado y enviamos el resumen a tu correo.'
                : 'El pedido quedó registrado, pero no pudimos enviar el resumen. Podés intentar reenviarlo desde esta página.');
            if (Sesion::obtener('continuar_compra', false)) Sesion::eliminar('continuar_compra');
            Utilidades::redirigir('compra_confirmada.php?id=' . $idPedido);
        } catch (RuntimeException $e) {
            $errores[] = $e->getMessage();
        } catch (PDOException $e) {
            error_log('No se pudo registrar el pedido de alojamiento: ' . $e->getMessage());
            $errores[] = 'No se pudo registrar el pedido. Revisá que esté instalada la actualización de compras de alojamiento.';
        } catch (Throwable $e) {
            error_log('Error inesperado al registrar el pedido de alojamiento: ' . $e->getMessage());
            $errores[] = 'Ocurrió un error inesperado al registrar el pedido. Volvé a intentarlo.';
        }
    }
}
Sesion::tomarMensaje();
?>
<!DOCTYPE html>
<html lang="es-AR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Finalizar compra | El Molino</title>
    <link rel="stylesheet" href="estilos/estilos.css?v=<?= (int) filemtime(__DIR__ . '/estilos/estilos.css') ?>">
    <script src="script.js?v=<?= (int) filemtime(__DIR__ . '/script.js') ?>" defer></script>
</head>
<body class="pagina-compra">
    <header class="encabezado encabezado-compra"><div class="contenedor barra-navegacion"><?= Vista::marca() ?><nav class="navegacion-compra" aria-label="Navegación de compra"><a href="carrito.php">Volver a mi estadía</a><a href="index.php">Inicio</a></nav></div></header>
    <main class="contenedor contenido-compra">
        <p class="texto-destacado">FINALIZAR COMPRA</p>
        <h1>Resumen del pedido</h1>
        <?php if ($errores): ?><p class="aviso-estadia" role="alert"><?= Utilidades::escapar(implode(' ', $errores)) ?></p><?php endif; ?>
        <?php if (!$usuario): ?>
            <section class="bloque-cuenta-compra">
                <h2>Para confirmar necesitás una cuenta</h2>
                <p>Tu selección se conserva mientras iniciás sesión o creás una cuenta. Como invitado podés elegir alojamientos y preparar el pedido.</p>
                <div class="acciones-carrito"><a class="boton boton-principal" href="registro/login.php">Ya tengo cuenta</a><a class="boton boton-secundario" href="registro/registro.php">Crear cuenta</a></div>
            </section>
        <?php else: ?>
            <p>Cuenta: <strong><?= Utilidades::escapar($usuario['email']) ?></strong></p>
        <?php endif; ?>
        <section class="resumen-compra">
            <?php foreach ($elementos as $elemento): ?>
                <article class="linea-resumen-compra <?= !empty($elemento['error']) ? 'linea-con-error' : '' ?>">
                    <div><h2><?= Utilidades::escapar($elemento['tipo'] ?? 'Alojamiento') ?><?= isset($elemento['numero']) ? ' · ' . Utilidades::escapar($elemento['numero']) : '' ?></h2>
                        <?php if (empty($elemento['error'])): ?><p><?= Utilidades::escapar($elemento['fechaIngreso']) ?> al <?= Utilidades::escapar($elemento['fechaEgreso']) ?> · <?= (int) $elemento['noches'] ?> noches · <?= (int) $elemento['personas'] ?> personas</p><strong>$<?= number_format((float) $elemento['subtotal'], 2, ',', '.') ?></strong>
                        <?php else: ?><p><?= Utilidades::escapar($elemento['error']) ?></p><?php endif; ?>
                    </div>
                </article>
            <?php endforeach; ?>
            <div class="resumen-carrito-alojamiento"><span>Total estimado</span><strong>$<?= number_format($totalCentavos / 100, 2, ',', '.') ?></strong></div>
        </section>
        <?php if ($usuario): ?>
            <form method="post" class="formulario-metodo-pago formulario-confirmacion-estadia">
                <input type="hidden" name="token_csrf" value="<?= Utilidades::escapar(Sesion::tokenCsrf()) ?>">
                <section class="asignacion-acompanantes-reserva" aria-labelledby="titulo-acompanantes-reserva">
                    <h2 id="titulo-acompanantes-reserva">¿Quiénes se hospedan con vos?</h2>
                    <p>Asigná los acompañantes ahora. Después vas a poder agregarlos o quitarlos desde Mi cuenta.</p>
                    <?php foreach ($elementos as $elemento): ?>
                        <?php if (!empty($elemento['error'])) continue; ?>
                        <?php $maximoAcompanantes = max(0, (int) $elemento['personas'] - 1); ?>
                        <fieldset>
                            <legend><?= Utilidades::escapar($elemento['tipo']) ?> · hasta <?= $maximoAcompanantes ?> acompañantes</legend>
                            <?php if ($maximoAcompanantes === 0): ?>
                                <p class="ayuda-formulario">Esta reserva fue indicada para una sola persona.</p>
                            <?php elseif (!$acompanantesCliente): ?>
                                <p class="ayuda-formulario">No tenés acompañantes registrados. La reserva se guardará a nombre del titular.</p>
                            <?php else: ?>
                                <div class="opciones-acompanantes-reserva">
                                    <?php foreach ($acompanantesCliente as $acompanante): ?>
                                        <?php $marcado = in_array((string) $acompanante['id'], array_map('strval', (array) ($_POST['acompanantes'][$elemento['clave']] ?? [])), true); ?>
                                        <label><input type="checkbox" name="acompanantes[<?= Utilidades::escapar($elemento['clave']) ?>][]" value="<?= (int) $acompanante['id'] ?>"<?= $marcado ? ' checked' : '' ?>><span><strong><?= Utilidades::escapar($acompanante['nombre'] . ' ' . $acompanante['apellido']) ?></strong><small><?= Utilidades::escapar($acompanante['parentesco']) ?></small></span></label>
                                    <?php endforeach; ?>
                                </div>
                            <?php endif; ?>
                        </fieldset>
                    <?php endforeach; ?>
                </section>
                <label for="metodoPago">Método de pago</label>
                <select id="metodoPago" name="metodoPago" required>
                    <option value="">Elegí cómo preferís pagar</option>
                    <?php foreach (GestorCarritoAlojamiento::METODOS_PAGO as $metodo): ?><option value="<?= Utilidades::escapar($metodo) ?>"<?= (string) ($_POST['metodoPago'] ?? '') === $metodo ? ' selected' : '' ?>><?= Utilidades::escapar($metodo) ?></option><?php endforeach; ?>
                </select>
                <div class="resumen-sena"><span>Seña para confirmar (<?= GestorCarritoAlojamiento::PORCENTAJE_SENA ?>%)</span><strong>$<?= number_format(($totalCentavos * GestorCarritoAlojamiento::PORCENTAJE_SENA / 100) / 100, 2, ',', '.') ?></strong></div>
                <p>La reserva queda pendiente hasta acreditar la seña. El saldo se abona según el medio coordinado con El Molino.</p>
                <button class="boton boton-principal" type="submit" <?= (!$gestor || $elementosConError || $totalCentavos <= 0) ? 'disabled' : '' ?>>Solicitar reserva y seña</button>
            </form>
        <?php endif; ?>
    </main>
</body>
</html>
