<?php
/**
 * ARCHIVO EXPLICADO: registro/estadia.php
 *
 * Muestra al cliente el detalle de una reserva propia: fechas contratadas,
 * situación de llegada, acompañantes, actividades y estado del dinero.
 */

declare(strict_types=1);

require_once __DIR__ . '/inicio.php';
require_once __DIR__ . '/../php/Vista.php';
require_once __DIR__ . '/../php/GestorEstadoFinanciero.php';

Sesion::iniciar();
$usuario = Sesion::usuario();
if (!$usuario) {
    Sesion::guardarMensaje('error', 'Iniciá sesión para ver tu estadía.');
    Utilidades::redirigir('login.php');
}

$idCliente = (int) ($usuario['id_cliente'] ?? 0);
$idReserva = (int) ($_GET['id'] ?? 0);
$estadia = null;
$acompanantes = [];
$actividades = [];
$error = null;
$fecha = static function (?string $valor, bool $hora = false): string {
    if (!$valor) return 'Sin registrar';
    try {
        return (new DateTimeImmutable($valor))->format($hora ? 'd/m/Y H:i' : 'd/m/Y');
    } catch (Exception) {
        return (string) $valor;
    }
};

if ($idCliente <= 0) {
    $error = 'No encontramos la estadía solicitada.';
} else {
    try {
        $conexion = Conexion::obtener();
        GestorEstadoFinanciero::vencerPedidosPendientes($conexion);
        // El acceso desde el menú no necesita un identificador: abre primero la
        // estadía vigente o próxima del cliente.
        if ($idReserva <= 0) {
            $buscarPrincipal = $conexion->prepare(
                "SELECT idReserva FROM reservas WHERE idCliente=:cliente
                   AND UPPER(estadoReserva) NOT IN ('CANCELADA','CANCELADO')
                 ORDER BY (fechaEgreso >= CURRENT_DATE) DESC, fechaIngreso ASC LIMIT 1"
            );
            $buscarPrincipal->execute(['cliente' => $idCliente]);
            $idReserva = (int) ($buscarPrincipal->fetchColumn() ?: 0);
        }
        if ($idReserva <= 0) throw new RuntimeException('Todavía no tenés una estadía reservada.');
        $consulta = $conexion->prepare(
            "SELECT r.idReserva, r.fechaIngreso, r.fechaEgreso, r.checkInReal, r.checkOutReal,
                    r.cantidadPersonas, r.estadoReserva, r.estadoLlegada, r.precioTotal,
                    a.tipo, a.capacidad, a.estado AS estadoAlojamiento,
                    p.idPedido, p.total AS totalPedido, p.montoSena, p.estadoPago, p.estadoPedido,
                    p.fechaLimiteSena, p.fechaLimiteRevisionSena,
                    (SELECT COALESCE(SUM(CASE WHEN pg.estado='CONFIRMADO' AND pg.tipoPago IN ('SENA','SALDO') THEN pg.montoTotal ELSE 0 END), 0)
                       FROM pago pg
                       JOIN detalle_pedido_alojamiento dp ON dp.idReserva=pg.idReserva
                      WHERE dp.idPedido=p.idPedido) AS montoPagado,
                    (SELECT COALESCE(SUM(CASE WHEN ca.estado='PENDIENTE' THEN ca.montoTotal ELSE 0 END), 0)
                       FROM cargo_actividad ca WHERE ca.idPedido=p.idPedido) AS actividadesPendientes
               FROM reservas r
               JOIN alojamientos a ON a.idAlojamiento=r.idAlojamiento
          LEFT JOIN detalle_pedido_alojamiento d ON d.idReserva=r.idReserva
          LEFT JOIN pedido_alojamiento p ON p.idPedido=d.idPedido
              WHERE r.idReserva=:reserva AND r.idCliente=:cliente
              LIMIT 1"
        );
        $consulta->execute(['reserva' => $idReserva, 'cliente' => $idCliente]);
        $estadia = $consulta->fetch();
        if (!$estadia) {
            $error = 'No encontramos esa estadía en tu cuenta.';
        } else {
            $consultaAcompanantes = $conexion->prepare(
                'SELECT ac.nombre, ac.apellido, ac.parentesco
                   FROM `reservaAcompañante` ra
                   JOIN `acompañante` ac ON ac.idAcompañante=ra.idAcompañante
                  WHERE ra.idReserva=:reserva ORDER BY ac.nombre, ac.apellido'
            );
            $consultaAcompanantes->execute(['reserva' => $idReserva]);
            $acompanantes = $consultaAcompanantes->fetchAll();

            $consultaActividades = $conexion->prepare(
                "SELECT a.nombre, ia.fechaActividad, a.horario, ca.montoTotal, ca.estado AS estadoCargo
                   FROM inscripcionActividad ia
                   JOIN Actividad a ON a.idActividad=ia.idActividad
              LEFT JOIN cargo_actividad ca ON ca.idInscripcion=ia.idInscripcion
                  WHERE ia.idCliente=:cliente
                    AND ia.fechaActividad>=:ingreso AND ia.fechaActividad<:egreso
                    AND UPPER(ia.estado)='ACTIVA'
                  ORDER BY ia.fechaActividad, a.horario, a.nombre"
            );
            $consultaActividades->execute([
                'cliente' => $idCliente,
                'ingreso' => $estadia['fechaIngreso'],
                'egreso' => $estadia['fechaEgreso'],
            ]);
            $actividades = $consultaActividades->fetchAll();
        }
    } catch (PDOException|RuntimeException $excepcion) {
        error_log('No se pudo cargar el detalle de estadía: ' . $excepcion->getMessage());
        $error = $excepcion instanceof RuntimeException
            ? $excepcion->getMessage()
            : 'No pudimos cargar el detalle. Verificá que la actualización de base de datos esté aplicada.';
    }
}

$situacion = strtoupper((string) ($estadia['estadoLlegada'] ?? 'PENDIENTE_LLEGADA'));
$estadoPagoActual = strtoupper((string) ($estadia['estadoPago'] ?? 'SENA_PENDIENTE'));
$senaAcreditada = in_array($estadoPagoActual, ['SENA_PAGADA', 'PAGADO'], true);
// La llegada solo se puede mostrar como pendiente después de que recepción acreditó la seña.
// Una transferencia informada sigue siendo una pre-reserva: todavía no confirma la estadía.
if ($estadoPagoActual === 'SENA_INFORMADA') {
    $estadoVisual = ['Seña en revisión', 'Informaste la transferencia. Recepción debe validarla antes de confirmar la estadía.'];
} elseif ($estadoPagoActual === 'PAGO_RECHAZADO') {
    $estadoVisual = ['Seña no confirmada', 'La transferencia no fue acreditada. Consultá con recepción para resolver la reserva.'];
} elseif (!$senaAcreditada) {
    $estadoVisual = ['Pre-reserva pendiente', 'La fecha está retenida temporalmente. Falta pagar e informar la seña para confirmarla.'];
} else {
    $estadoVisual = match ($situacion) {
        'EN_ESTADIA' => ['En el complejo', 'Llegada registrada por recepción.'],
        'NO_SE_PRESENTO' => ['No se presentó', 'La estadía mantiene las fechas y el saldo contratados.'],
        'FINALIZADA' => ['Finalizada', 'Recepción registró la salida y cerró la estadía.'],
        default => ['Pendiente de llegada', 'La seña fue acreditada. Al llegar, recepción registrará el check-in.'],
    };
}
$total = (float) ($estadia['totalPedido'] ?? $estadia['precioTotal'] ?? 0);
$pagado = (float) ($estadia['montoPagado'] ?? 0);
$cargos = (float) ($estadia['actividadesPendientes'] ?? 0);
$saldoAlojamiento = max(0, $total - $pagado);
?>
<!DOCTYPE html>
<html lang="es-AR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="icon" type="image/svg+xml" href="../imgs/favicon-molino.svg">
    <title>Mi estadía | El Molino</title>
    <link rel="stylesheet" href="../estilos/estilos.css?v=<?= (int) filemtime(__DIR__ . '/../estilos/estilos.css') ?>">
    <script src="../script.js?v=<?= (int) filemtime(__DIR__ . '/../script.js') ?>" defer></script>
</head>
<body class="pagina-estadia-cliente">
    <a class="enlace-saltar" href="#contenido">Saltar al contenido</a>
    <?= Vista::encabezadoPrincipal($usuario, '../', 'estadía') ?>
    <main class="contenedor contenido-estadia" id="contenido">
        <a class="volver-cuenta" href="cuenta.php">Volver a mi cuenta</a>
        <?php if ($error || !$estadia): ?>
            <section class="estado-vacio-estadia"><p class="texto-destacado">MI ESTADÍA</p><h1>No hay detalle para mostrar</h1><p><?= Utilidades::escapar((string) $error) ?></p><a class="boton boton-principal" href="cuenta.php">Ir a mi cuenta</a></section>
        <?php else: ?>
            <section class="cabecera-estadia">
                <div>
                    <p class="texto-destacado">MI ESTADÍA</p>
                    <h1><?= Utilidades::escapar((string) $estadia['tipo']) ?> para <?= (int) $estadia['capacidad'] ?> personas</h1>
                    <p class="rango-estadia"><?= $fecha($estadia['fechaIngreso']) ?> al <?= $fecha($estadia['fechaEgreso']) ?></p>
                </div>
                <div class="estado-estadia-pill estado-<?= strtolower($situacion) ?>"><strong><?= Utilidades::escapar($estadoVisual[0]) ?></strong><span><?= Utilidades::escapar($estadoVisual[1]) ?></span></div>
            </section>

            <section class="metricas-estadia" aria-label="Resumen de la estadía">
                <article><span>Ingreso contratado</span><strong><?= $fecha($estadia['fechaIngreso']) ?> · desde las 14:00</strong></article>
                <article><span>Llegada real</span><strong><?= $fecha($estadia['checkInReal'] ?? null, true) ?></strong></article>
                <article><span>Egreso contratado</span><strong><?= $fecha($estadia['fechaEgreso']) ?> · hasta las 10:00</strong></article>
                <article><span>Salida real</span><strong><?= $fecha($estadia['checkOutReal'] ?? null, true) ?></strong></article>
            </section>

            <section class="linea-tiempo-estadia" aria-labelledby="titulo-recorrido">
                <p class="texto-destacado">RECORRIDO</p><h2 id="titulo-recorrido">Así está tu estadía</h2>
                <ol>
                    <li class="<?= $senaAcreditada ? 'completo' : '' ?>"><strong><?= $senaAcreditada ? 'Reserva confirmada' : 'Confirmación de la seña' ?></strong><span><?= $senaAcreditada ? 'La seña fue validada y las fechas quedaron reservadas.' : 'La reserva se confirma solamente cuando recepción acredita la seña.' ?></span></li>
                    <li class="<?= $senaAcreditada && in_array($situacion, ['EN_ESTADIA', 'FINALIZADA'], true) ? 'completo' : ($situacion === 'NO_SE_PRESENTO' ? 'detenido' : '') ?>"><strong>Llegada en recepción</strong><span><?= !$senaAcreditada ? 'Se habilita cuando la seña esté confirmada.' : ($situacion === 'NO_SE_PRESENTO' ? 'No se registró llegada.' : 'Recepción confirma que llegaste al complejo.') ?></span></li>
                    <li class="<?= $situacion === 'FINALIZADA' ? 'completo' : '' ?>"><strong>Salida y saldo final</strong><span>Al retirarte, recepción registra la salida y cobra el saldo que corresponda.</span></li>
                </ol>
            </section>

            <div class="grilla-detalle-estadia">
                <section class="detalle-estadia-card"><p class="texto-destacado">ALOJAMIENTO</p><h2>Tu grupo</h2><dl>
                    <div><dt>Huéspedes</dt><dd><?= (int) $estadia['cantidadPersonas'] ?> en total</dd></div>
                    <div><dt>Titular</dt><dd><?= Utilidades::escapar((string) $usuario['nombre']) ?></dd></div>
                    <div><dt>Estado de la reserva</dt><dd><?= Utilidades::escapar(Vista::estadoPedidoCliente((string) $estadia['estadoReserva'])) ?></dd></div>
                </dl>
                <?php if ($acompanantes): ?><ul class="lista-huespedes-estadia"><?php foreach ($acompanantes as $acompanante): ?><li><?= Utilidades::escapar($acompanante['nombre'] . ' ' . $acompanante['apellido']) ?> <span><?= Utilidades::escapar((string) $acompanante['parentesco']) ?></span></li><?php endforeach; ?></ul><?php else: ?><p class="texto-suave">No agregaste acompañantes a esta reserva.</p><?php endif; ?>
                </section>

                <section class="detalle-estadia-card"><p class="texto-destacado">PAGOS</p><h2>Estado del saldo</h2><dl class="resumen-dinero-estadia">
                    <div><dt>Total del alojamiento</dt><dd>$<?= number_format($total, 2, ',', '.') ?></dd></div>
                    <div><dt>Seña y pagos confirmados</dt><dd>$<?= number_format($pagado, 2, ',', '.') ?></dd></div>
                    <div><dt>Saldo del alojamiento</dt><dd>$<?= number_format($saldoAlojamiento, 2, ',', '.') ?></dd></div>
                    <div><dt>Actividades pendientes</dt><dd>$<?= number_format($cargos, 2, ',', '.') ?></dd></div>
                </dl><p class="texto-suave">Estado del pago: <?= Utilidades::escapar(Vista::estadoPagoCliente((string) $estadia['estadoPago'])) ?>. El saldo se registra en recepción al cerrar la estadía.</p>
                </section>
            </div>

            <section class="detalle-estadia-card actividades-estadia"><p class="texto-destacado">ACTIVIDADES</p><h2>Durante esta estadía</h2>
                <?php if ($actividades): ?><ul><?php foreach ($actividades as $actividad): ?><li><div><strong><?= Utilidades::escapar($actividad['nombre']) ?></strong><span><?= $fecha($actividad['fechaActividad']) ?> · <?= Utilidades::escapar((string) $actividad['horario']) ?></span></div><span><?= strtoupper((string) $actividad['estadoCargo']) === 'PENDIENTE' ? 'Pendiente: $' . number_format((float) $actividad['montoTotal'], 2, ',', '.') : 'Abonada' ?></span></li><?php endforeach; ?></ul><?php else: ?><p class="texto-suave">No hay actividades registradas dentro de estas fechas.</p><?php endif; ?>
            </section>
        <?php endif; ?>
    </main>
    <footer class="pie-pagina pie-pagina-simple"><div class="contenedor pie-inferior"><span>El Molino · Complejo recreativo</span><?= Vista::contactoPie() ?></div></footer>
</body>
</html>
