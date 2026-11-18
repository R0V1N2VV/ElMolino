<?php
declare(strict_types=1);

require_once __DIR__ . '/GestorCoherenciaActividades.php';
require_once __DIR__ . '/RelojDemostracion.php';

/**
 * Mantiene sincronizados el pedido, sus reservas, pagos y reintegros.
 * Todas las funciones reciben una conexión existente para poder participar
 * de la misma transacción que originó el cambio.
 */
final class GestorEstadoFinanciero
{
    /** Libera reservas cuya seña venció y nunca llegó a completarse. */
    public static function vencerPedidosPendientes(PDO $conexion): int
    {
        $momentoOperativo = RelojDemostracion::ahora($conexion)->format('Y-m-d H:i:s');
        $iniciarTransaccion = !$conexion->inTransaction();
        if ($iniciarTransaccion) $conexion->beginTransaction();
        try {
            $consulta = $conexion->prepare(
                "SELECT pa.idPedido, pa.idCliente, UPPER(pa.estadoPago) AS estadoPago
                   FROM pedido_alojamiento pa
                  WHERE ((UPPER(pa.estadoPedido) IN ('ESPERANDO_SENA','PENDIENTE')
                         AND UPPER(pa.estadoPago)='SENA_PENDIENTE'
                         AND pa.fechaLimiteSena IS NOT NULL AND pa.fechaLimiteSena < :momentoSena
                         AND (SELECT COALESCE(SUM(pg.montoTotal),0)
                                FROM pago pg JOIN detalle_pedido_alojamiento dpg ON dpg.idReserva=pg.idReserva
                               WHERE dpg.idPedido=pa.idPedido AND UPPER(pg.estado)='CONFIRMADO'
                                 AND UPPER(pg.tipoPago) IN ('SENA','SALDO')) < pa.montoSena)
                      OR (UPPER(pa.estadoPago)='SENA_INFORMADA'
                          AND pa.fechaLimiteRevisionSena IS NOT NULL
                          AND pa.fechaLimiteRevisionSena < :momentoRevision))
                  FOR UPDATE"
            );
            $consulta->execute([
                'momentoSena' => $momentoOperativo,
                'momentoRevision' => $momentoOperativo,
            ]);
            $pedidos = $consulta->fetchAll();
            foreach ($pedidos as $pedido) {
                $idPedido = (int) $pedido['idPedido'];
                $idCliente = (int) $pedido['idCliente'];
                $enRevision = strtoupper((string) $pedido['estadoPago']) === 'SENA_INFORMADA';
                $conexion->prepare(
                    "UPDATE pedido_alojamiento
                        SET estadoPedido='VENCIDO', estadoPago=:estadoPago,
                            fechaLimiteRevisionSena=NULL, canceladoEn=:momentoCancelado
                      WHERE idPedido=:pedido"
                )->execute([
                    'pedido' => $idPedido,
                    'estadoPago' => $enRevision ? 'PAGO_RECHAZADO' : 'VENCIDO',
                    'momentoCancelado' => $momentoOperativo,
                ]);
                $conexion->prepare(
                    "UPDATE reservas r JOIN detalle_pedido_alojamiento d ON d.idReserva=r.idReserva
                        SET r.estadoReserva='CANCELADA'
                      WHERE d.idPedido=:pedido
                        AND UPPER(r.estadoReserva) NOT IN ('CANCELADA','CANCELADO','FINALIZADA')"
                )->execute(['pedido' => $idPedido]);
                $conexion->prepare(
                    "UPDATE pago pg JOIN detalle_pedido_alojamiento d ON d.idReserva=pg.idReserva
                        SET pg.estado='ANULADO', pg.observacion=:motivo
                      WHERE d.idPedido=:pedido AND UPPER(pg.estado)='PENDIENTE'"
                )->execute([
                    'pedido' => $idPedido,
                    'motivo' => $enRevision
                        ? 'La recepción no pudo confirmar la transferencia dentro de las 24 horas.'
                        : 'Venció el plazo de 30 minutos para informar la seña.',
                ]);

                // No puede sobrevivir una inscripción a una actividad si la
                // estadía que la habilitaba venció. Este paso también anula el
                // cargo pendiente de la actividad y libera sus cupos.
                if ($idCliente > 0) {
                    GestorCoherenciaActividades::sincronizarCliente(
                        $conexion,
                        $idCliente,
                        'ESTADIA_VENCIDA_SIN_SENA'
                    );
                }
            }
            if ($iniciarTransaccion) $conexion->commit();
            return count($pedidos);
        } catch (Throwable $error) {
            if ($iniciarTransaccion && $conexion->inTransaction()) $conexion->rollBack();
            throw $error;
        }
    }

    /** Recalcula importes y estados usando únicamente reservas vigentes y dinero confirmado. */
    public static function sincronizarPedido(PDO $conexion, int $idPedido): array
    {
        $momentoOperativo = RelojDemostracion::ahora($conexion)->format('Y-m-d H:i:s');
        $consulta = $conexion->prepare('SELECT * FROM pedido_alojamiento WHERE idPedido=:pedido FOR UPDATE');
        $consulta->execute(['pedido' => $idPedido]);
        $pedido = $consulta->fetch();
        if (!$pedido) throw new RuntimeException('No se encontró el pedido que se debe actualizar.');

        $totales = $conexion->prepare(
            "SELECT COALESCE(SUM(CASE WHEN UPPER(r.estadoReserva) NOT IN ('CANCELADA','CANCELADO') THEN d.subtotal ELSE 0 END),0) total,
                    SUM(CASE WHEN UPPER(r.estadoReserva) NOT IN ('CANCELADA','CANCELADO') THEN 1 ELSE 0 END) activas
               FROM detalle_pedido_alojamiento d JOIN reservas r ON r.idReserva=d.idReserva
              WHERE d.idPedido=:pedido"
        );
        $totales->execute(['pedido' => $idPedido]);
        $resumen = $totales->fetch();
        $total = round((float) $resumen['total'], 2);
        $activas = (int) $resumen['activas'];
        $montoSena = round($total * (float) $pedido['porcentajeSena'] / 100, 2);

        $movimientos = $conexion->prepare(
            "SELECT COALESCE(SUM(CASE WHEN UPPER(pg.estado)='CONFIRMADO' AND UPPER(pg.tipoPago) IN ('SENA','SALDO') THEN pg.montoTotal ELSE 0 END),0) ingresos,
                    COALESCE((SELECT SUM(rf.monto) FROM reembolso rf WHERE rf.idPedido=:pedido2 AND UPPER(rf.estado)='CONFIRMADO'),0) devuelto,
                    COALESCE((SELECT SUM(rp.monto) FROM reembolso rp WHERE rp.idPedido=:pedido3 AND UPPER(rp.estado)='PENDIENTE'),0) reintegroPendiente,
                    SUM(CASE WHEN UPPER(pg.estado)='PENDIENTE' THEN 1 ELSE 0 END) pagosPendientes
               FROM detalle_pedido_alojamiento d LEFT JOIN pago pg ON pg.idReserva=d.idReserva
              WHERE d.idPedido=:pedido"
        );
        $movimientos->execute(['pedido' => $idPedido, 'pedido2' => $idPedido, 'pedido3' => $idPedido]);
        $dinero = $movimientos->fetch();
        $pagadoNeto = max(0, round((float) $dinero['ingresos'] - (float) $dinero['devuelto'], 2));
        $reintegroPendiente = (float) $dinero['reintegroPendiente'];

        if ($activas === 0) {
            $estadoPedido = 'CANCELADO';
            $estadoPago = $reintegroPendiente > 0 ? 'REEMBOLSO_PENDIENTE'
                : ($pagadoNeto > 0 ? 'REEMBOLSO_PENDIENTE' : 'CANCELADO');
        } else {
            $estadoPedido = $pagadoNeto >= $montoSena && $montoSena > 0 ? 'CONFIRMADO' : 'ESPERANDO_SENA';
            if ($reintegroPendiente > 0) $estadoPago = 'REEMBOLSO_PENDIENTE';
            elseif ($pagadoNeto >= $total && $total > 0) $estadoPago = 'PAGADO';
            elseif ($pagadoNeto >= $montoSena && $montoSena > 0) $estadoPago = 'SENA_PAGADA';
            elseif ((int) $dinero['pagosPendientes'] > 0) $estadoPago = 'SENA_INFORMADA';
            else $estadoPago = 'SENA_PENDIENTE';
        }

        $conexion->prepare(
            'UPDATE pedido_alojamiento SET total=:total, montoSena=:sena, estadoPedido=:pedidoEstado,
                    estadoPago=:pagoEstado,
                    senaPagadaEn=CASE WHEN :confirmada=1 THEN COALESCE(senaPagadaEn,:momentoConfirmacion) ELSE NULL END,
                    canceladoEn=CASE WHEN :cancelado=1 THEN COALESCE(canceladoEn,:momentoCancelacion) ELSE NULL END
              WHERE idPedido=:pedido'
        )->execute([
            'total' => $total, 'sena' => $montoSena, 'pedidoEstado' => $estadoPedido,
            'pagoEstado' => $estadoPago, 'confirmada' => $pagadoNeto >= $montoSena && $montoSena > 0 ? 1 : 0,
            'cancelado' => $activas === 0 ? 1 : 0,
            'momentoConfirmacion' => $momentoOperativo,
            'momentoCancelacion' => $momentoOperativo,
            'pedido' => $idPedido,
        ]);
        if ($activas > 0) {
            $conexion->prepare(
                "UPDATE reservas r JOIN detalle_pedido_alojamiento d ON d.idReserva=r.idReserva
                    SET r.estadoReserva=:estado
                  WHERE d.idPedido=:pedido AND UPPER(r.estadoReserva) NOT IN ('CANCELADA','CANCELADO','FINALIZADA')"
            )->execute(['estado' => $pagadoNeto >= $montoSena && $montoSena > 0 ? 'CONFIRMADA' : 'PENDIENTE_SENA', 'pedido' => $idPedido]);
        }
        return compact('total', 'montoSena', 'pagadoNeto', 'activas', 'estadoPedido', 'estadoPago');
    }

    /** Crea un solo reintegro pendiente por el excedente que corresponde devolver. */
    public static function solicitarReembolsoCancelacion(PDO $conexion, int $idPedido): float
    {
        $anterior = $conexion->prepare('SELECT total FROM pedido_alojamiento WHERE idPedido=:pedido FOR UPDATE');
        $anterior->execute(['pedido' => $idPedido]);
        $totalAnterior = (float) ($anterior->fetchColumn() ?: 0);
        $estado = self::sincronizarPedido($conexion, $idPedido);
        $importeARetener = 0;
        if ($estado['activas'] > 0) {
            $importeARetener = $estado['pagadoNeto'] + 0.009 >= $totalAnterior
                ? $estado['total']
                : $estado['montoSena'];
        }
        $importe = round(max(0, $estado['pagadoNeto'] - $importeARetener), 2);
        $conexion->prepare("DELETE FROM reembolso WHERE idPedido=:pedido AND estado='PENDIENTE'")
            ->execute(['pedido' => $idPedido]);
        if ($importe > 0) {
            $conexion->prepare(
                "INSERT INTO reembolso (idPedido,monto,estado,motivo,fechaSolicitud)
                 VALUES (:pedido,:monto,'PENDIENTE','Cancelación de alojamiento',NOW())"
            )->execute(['pedido' => $idPedido, 'monto' => $importe]);
            self::sincronizarPedido($conexion, $idPedido);
        }
        return $importe;
    }
}
