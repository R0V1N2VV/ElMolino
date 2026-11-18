<?php
/**
 * Registra que un cliente realizó la transferencia de la seña.
 *
 * El pago se guarda como PENDIENTE. Administración debe verificar el dinero
 * recibido y cambiarlo a CONFIRMADO desde la aplicación Java. Ese cambio
 * confirma automáticamente el pedido y las reservas relacionadas.
 */
declare(strict_types=1);
require_once __DIR__ . '/GestorComprobantePago.php';
require_once __DIR__ . '/RelojDemostracion.php';

final class GestorPagosSena
{
    public function __construct(private PDO $conexion) {}

    public function informarTransferencia(int $idPedido, int $idCliente, string $referencia, string $montoInformado, array $archivo): void
    {
        $referencia = trim($referencia);
        if (strlen($referencia) < 4 || strlen($referencia) > 150) {
            throw new InvalidArgumentException('Ingresá el número de operación o una referencia válida de la transferencia.');
        }
        $monto = (float) str_replace(',', '.', trim($montoInformado));
        if ($monto <= 0) throw new InvalidArgumentException('Ingresá el importe que transferiste.');

        $archivoGuardado = null;
        $this->conexion->beginTransaction();
        try {
            $consulta = $this->conexion->prepare(
                'SELECT idPedido, total, montoSena, estadoPedido, estadoPago, fechaLimiteSena
                   FROM pedido_alojamiento
                  WHERE idPedido = :pedido AND idCliente = :cliente
                  FOR UPDATE'
            );
            $consulta->execute(['pedido' => $idPedido, 'cliente' => $idCliente]);
            $pedido = $consulta->fetch();
            if (!$pedido) throw new RuntimeException('No encontramos ese pedido en tu cuenta.');

            $estadoPedido = strtoupper((string) $pedido['estadoPedido']);
            $estadoPago = strtoupper((string) $pedido['estadoPago']);
            if (in_array($estadoPedido, ['CANCELADO', 'VENCIDO'], true)) {
                throw new RuntimeException('El pedido está cancelado y ya no admite pagos.');
            }
            $pagado = $this->conexion->prepare(
                "SELECT COALESCE(SUM(pg.montoTotal),0) FROM pago pg
                  JOIN detalle_pedido_alojamiento dpg ON dpg.idReserva=pg.idReserva
                 WHERE dpg.idPedido=:pedido AND pg.estado='CONFIRMADO' AND pg.tipoPago IN ('SENA','SALDO')"
            );
            $pagado->execute(['pedido' => $idPedido]);
            $montoYaConfirmado = (float) $pagado->fetchColumn();
            $restanteTotal = max(0, (float) $pedido['total'] - $montoYaConfirmado);
            if ($monto > $restanteTotal + 0.009) throw new RuntimeException('El importe informado supera el saldo pendiente del pedido.');
            $senaPendiente = max(0, (float) $pedido['montoSena'] - $montoYaConfirmado);
            if ($monto + 0.009 < $senaPendiente) throw new RuntimeException('Para retener la estadía informá el importe completo de la seña: $' . number_format($senaPendiente, 2, ',', '.'));
            $duplicado = $this->conexion->prepare("SELECT COUNT(*) FROM pago WHERE comprobante=:referencia AND estado<>'ANULADO'");
            $duplicado->execute(['referencia' => $referencia]);
            if ((int) $duplicado->fetchColumn() > 0) throw new RuntimeException('Ese número de operación ya fue informado.');
            if (in_array($estadoPago, ['SENA_PAGADA', 'PAGADO'], true)) {
                throw new RuntimeException('La seña de este pedido ya está acreditada.');
            }
            if (!empty($pedido['fechaLimiteSena'])) {
                $zona = new DateTimeZone('America/Argentina/Buenos_Aires');
                $vencimiento = new DateTimeImmutable((string) $pedido['fechaLimiteSena'], $zona);
                if ($vencimiento < RelojDemostracion::ahora($this->conexion)) {
                    throw new RuntimeException('Venció el plazo de la seña. Contactá a recepción antes de transferir.');
                }
            }

            $reserva = $this->conexion->prepare(
                'SELECT idReserva FROM detalle_pedido_alojamiento
                  WHERE idPedido = :pedido ORDER BY idDetalle LIMIT 1'
            );
            $reserva->execute(['pedido' => $idPedido]);
            $idReserva = (int) ($reserva->fetchColumn() ?: 0);
            if ($idReserva <= 0) throw new RuntimeException('El pedido no tiene una reserva asociada.');

            $pagoExistente = $this->conexion->prepare(
                "SELECT p.idPago
                   FROM pago p
                   JOIN detalle_pedido_alojamiento d ON d.idReserva = p.idReserva
                  WHERE d.idPedido = :pedido AND p.tipoPago = 'SENA' AND p.estado = 'PENDIENTE'
                  ORDER BY p.idPago DESC LIMIT 1 FOR UPDATE"
            );
            $pagoExistente->execute(['pedido' => $idPedido]);
            $idPago = (int) ($pagoExistente->fetchColumn() ?: 0);
            $archivoGuardado = GestorComprobantePago::guardar($archivo);

            $datosPago = [
                'reserva' => $idReserva,
                'monto' => number_format($monto, 2, '.', ''),
                'comprobante' => $referencia,
                'archivo' => $archivoGuardado,
                'fechaPago' => RelojDemostracion::fecha($this->conexion),
            ];
            if ($idPago > 0) {
                $datosPago['pago'] = $idPago;
                $this->conexion->prepare(
                    "UPDATE pago
                        SET idReserva = :reserva, montoTotal = :monto,
                            metodoPago = 'Transferencia', fechaPago = :fechaPago,
                            comprobante = :comprobante, archivoComprobante = :archivo, observacion = NULL
                      WHERE idPago = :pago"
                )->execute($datosPago);
            } else {
                $this->conexion->prepare(
                    "INSERT INTO pago
                        (idReserva, montoTotal, metodoPago, tipoPago, estado, fechaPago, comprobante, archivoComprobante)
                     VALUES (:reserva, :monto, 'Transferencia', 'SENA', 'PENDIENTE', :fechaPago, :comprobante, :archivo)"
                )->execute($datosPago);
            }

            $this->conexion->prepare(
                "UPDATE pedido_alojamiento
                    SET estadoPago = 'SENA_INFORMADA', fechaLimiteRevisionSena = DATE_ADD(:momento, INTERVAL 24 HOUR)
                  WHERE idPedido = :pedido"
            )->execute(['pedido' => $idPedido, 'momento' => RelojDemostracion::ahora($this->conexion)->format('Y-m-d H:i:s')]);
            $this->conexion->commit();
        } catch (Throwable $error) {
            if ($this->conexion->inTransaction()) $this->conexion->rollBack();
            if ($archivoGuardado !== null) @unlink(dirname(__DIR__) . '/' . $archivoGuardado);
            throw $error;
        }
    }
}
