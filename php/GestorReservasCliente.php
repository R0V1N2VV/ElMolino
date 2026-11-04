<?php
declare(strict_types=1);

/**
 * Gestiona cambios que el cliente puede hacer sobre una reserva ya creada.
 * Todas las consultas verifican que la reserva pertenezca al usuario actual.
 */
final class GestorReservasCliente
{
    /** Inicializa el gestor con la conexión compartida a MySQL. */
    public function __construct(private PDO $conexion) {}

    /**
     * Reemplaza los acompañantes asociados a una reserva futura.
     *
     * @param int[] $idsAcompanantes
     */
    public function actualizarAcompanantes(int $idReserva, int $idCliente, array $idsAcompanantes): void
    {
        $this->conexion->beginTransaction();
        try {
            $reserva = $this->bloquearReserva($idReserva, $idCliente);
            if ((string) $reserva['fechaIngreso'] <= date('Y-m-d')) {
                throw new RuntimeException('Los acompañantes solo se pueden modificar antes del inicio de la estadía.');
            }
            if (in_array(strtoupper((string) $reserva['estadoReserva']), ['CANCELADA', 'CANCELADO', 'FINALIZADA'], true)) {
                throw new RuntimeException('Esta reserva ya no admite cambios.');
            }

            $idsValidos = $this->validarAcompanantes(
                $idCliente,
                $idsAcompanantes,
                max(0, (int) $reserva['cantidadPersonas'] - 1)
            );
            $borrar = $this->conexion->prepare('DELETE FROM `reservaAcompañante` WHERE idReserva = :reserva');
            $borrar->execute(['reserva' => $idReserva]);
            $agregar = $this->conexion->prepare(
                'INSERT INTO `reservaAcompañante` (idReserva, idAcompañante) VALUES (:reserva, :acompanante)'
            );
            foreach ($idsValidos as $idAcompanante) {
                $agregar->execute(['reserva' => $idReserva, 'acompanante' => $idAcompanante]);
            }
            $this->conexion->commit();
        } catch (Throwable $error) {
            if ($this->conexion->inTransaction()) $this->conexion->rollBack();
            throw $error;
        }
    }

    /**
     * Cancela una reserva antes de la fecha de ingreso. Si ya hubo pagos deja
     * el pedido marcado para que administración gestione el reintegro.
     */
    public function cancelar(int $idReserva, int $idCliente): void
    {
        $this->conexion->beginTransaction();
        try {
            $reserva = $this->bloquearReserva($idReserva, $idCliente);
            if ((string) $reserva['fechaIngreso'] <= date('Y-m-d')) {
                throw new RuntimeException('La estadía ya comenzó o comienza hoy y no se puede cancelar desde la web.');
            }
            if (in_array(strtoupper((string) $reserva['estadoReserva']), ['CANCELADA', 'CANCELADO'], true)) {
                throw new RuntimeException('La reserva ya estaba cancelada.');
            }

            $actualizar = $this->conexion->prepare(
                "UPDATE reservas SET estadoReserva = 'CANCELADA' WHERE idReserva = :reserva AND idCliente = :cliente"
            );
            $actualizar->execute(['reserva' => $idReserva, 'cliente' => $idCliente]);

            $pedido = $this->conexion->prepare(
                'SELECT d.idPedido,
                        (SELECT COUNT(*) FROM detalle_pedido_alojamiento dx
                          JOIN reservas rx ON rx.idReserva = dx.idReserva
                         WHERE dx.idPedido = d.idPedido
                           AND UPPER(rx.estadoReserva) NOT IN (\'CANCELADA\', \'CANCELADO\')) AS reservasActivas,
                        (SELECT COALESCE(SUM(p.montoTotal), 0) FROM pago p
                          JOIN detalle_pedido_alojamiento dp ON dp.idReserva = p.idReserva
                         WHERE dp.idPedido = d.idPedido) AS montoPagado
                   FROM detalle_pedido_alojamiento d
                  WHERE d.idReserva = :reserva LIMIT 1 FOR UPDATE'
            );
            $pedido->execute(['reserva' => $idReserva]);
            $datosPedido = $pedido->fetch();
            if ($datosPedido) {
                $quedanReservas = (int) $datosPedido['reservasActivas'] > 0;
                $estadoPedido = $quedanReservas ? 'PARCIALMENTE_CANCELADO' : 'CANCELADO';
                $estadoPago = $quedanReservas
                    ? null
                    : ((float) $datosPedido['montoPagado'] > 0 ? 'REEMBOLSO_PENDIENTE' : 'CANCELADO');
                $sql = 'UPDATE pedido_alojamiento SET estadoPedido = :estadoPedido, canceladoEn = NOW()';
                $parametros = ['estadoPedido' => $estadoPedido, 'pedido' => (int) $datosPedido['idPedido']];
                if ($estadoPago !== null) {
                    $sql .= ', estadoPago = :estadoPago';
                    $parametros['estadoPago'] = $estadoPago;
                }
                $sql .= ' WHERE idPedido = :pedido';
                $this->conexion->prepare($sql)->execute($parametros);
            }
            $this->conexion->commit();
        } catch (Throwable $error) {
            if ($this->conexion->inTransaction()) $this->conexion->rollBack();
            throw $error;
        }
    }

    /** Devuelve y bloquea una reserva del cliente para evitar cambios simultáneos. */
    private function bloquearReserva(int $idReserva, int $idCliente): array
    {
        $consulta = $this->conexion->prepare(
            'SELECT idReserva, fechaIngreso, cantidadPersonas, estadoReserva
               FROM reservas WHERE idReserva = :reserva AND idCliente = :cliente FOR UPDATE'
        );
        $consulta->execute(['reserva' => $idReserva, 'cliente' => $idCliente]);
        $reserva = $consulta->fetch();
        if (!$reserva) throw new RuntimeException('No encontramos esa reserva en tu cuenta.');
        return $reserva;
    }

    /**
     * Comprueba propiedad, duplicados y límite de huéspedes.
     *
     * @return int[]
     */
    private function validarAcompanantes(int $idCliente, array $ids, int $maximo): array
    {
        $ids = array_values(array_unique(array_filter(array_map('intval', $ids), static fn(int $id): bool => $id > 0)));
        if (count($ids) > $maximo) throw new RuntimeException('La selección supera la cantidad de huéspedes de la reserva.');
        if (!$ids) return [];
        $marcadores = implode(',', array_fill(0, count($ids), '?'));
        $consulta = $this->conexion->prepare(
            "SELECT idAcompañante FROM `acompañante` WHERE idCliente = ? AND idAcompañante IN ($marcadores)"
        );
        $consulta->execute([$idCliente, ...$ids]);
        $validos = array_map('intval', $consulta->fetchAll(PDO::FETCH_COLUMN));
        sort($ids);
        sort($validos);
        if ($ids !== $validos) throw new RuntimeException('Uno de los acompañantes no pertenece a tu cuenta.');
        return $validos;
    }
}
