<?php
declare(strict_types=1);

/**
 * Mantiene alineadas las inscripciones, los acompañantes y los cargos.
 *
 * Esta clase se usa después de modificar o cancelar una estadía. Así una
 * persona no puede conservar una actividad si ya no tiene alojamiento válido
 * para ese día, y los importes pendientes siempre reflejan los participantes
 * que realmente continúan inscriptos.
 */
final class GestorCoherenciaActividades
{
    private function __construct() {}

    /** Revisa todas las inscripciones activas de un cliente. */
    public static function sincronizarCliente(PDO $conexion, int $idCliente, string $motivo): void
    {
        $consulta = $conexion->prepare(
            "SELECT idInscripcion, fechaActividad
               FROM inscripcionActividad
              WHERE idCliente = :cliente AND UPPER(estado) = 'ACTIVA'
              FOR UPDATE"
        );
        $consulta->execute(['cliente' => $idCliente]);

        foreach ($consulta->fetchAll() as $inscripcion) {
            $idInscripcion = (int) $inscripcion['idInscripcion'];
            $fecha = (string) $inscripcion['fechaActividad'];

            $titularValido = self::clienteTieneEstadia($conexion, $idCliente, $fecha);
            if (!$titularValido) {
                $conexion->prepare(
                    'UPDATE inscripcionActividad SET participaTitular = 0
                      WHERE idInscripcion = :inscripcion'
                )->execute(['inscripcion' => $idInscripcion]);
            }

            $acompanantes = $conexion->prepare(
                'SELECT iaa.idInscripcionAcompanante, iaa.idAcompañante
                   FROM inscripcionActividadAcompanante iaa
                  WHERE iaa.idClienteTitular = :cliente
                    AND iaa.idActividad = (
                        SELECT idActividad FROM inscripcionActividad WHERE idInscripcion = :inscripcion
                    )
                    AND iaa.fechaActividad = :fecha
                    AND iaa.activo = 1
                  FOR UPDATE'
            );
            $acompanantes->execute(['cliente' => $idCliente, 'inscripcion' => $idInscripcion, 'fecha' => $fecha]);
            foreach ($acompanantes->fetchAll() as $acompanante) {
                if (!self::acompananteTieneEstadia(
                    $conexion,
                    (int) $acompanante['idAcompañante'],
                    $fecha
                )) {
                    $conexion->prepare(
                        'UPDATE inscripcionActividadAcompanante
                            SET activo = 0, canceladaEn = NOW()
                          WHERE idInscripcionAcompanante = :id'
                    )->execute(['id' => (int) $acompanante['idInscripcionAcompanante']]);
                }
            }

            $participantes = self::cantidadParticipantes($conexion, $idInscripcion);
            if ($participantes === 0) {
                $conexion->prepare(
                    "UPDATE inscripcionActividad
                        SET estado = 'CANCELADA', canceladaEn = NOW(), motivoCancelacion = :motivo
                      WHERE idInscripcion = :inscripcion"
                )->execute(['motivo' => $motivo, 'inscripcion' => $idInscripcion]);
            }
            self::sincronizarCargo($conexion, $idInscripcion);
        }
    }

    /** Crea o recalcula el cargo pendiente usando el precio aceptado al inscribirse. */
    public static function sincronizarCargo(PDO $conexion, int $idInscripcion): void
    {
        $consulta = $conexion->prepare(
            "SELECT ia.idCliente, ia.idActividad, ia.fechaActividad, ia.estado,
                    a.modalidad, a.precio,
                    (SELECT p.idPedido
                       FROM reservas r
                       JOIN detalle_pedido_alojamiento d ON d.idReserva = r.idReserva
                       JOIN pedido_alojamiento p ON p.idPedido = d.idPedido
                      WHERE r.idCliente = ia.idCliente
                        AND UPPER(r.estadoReserva) = 'CONFIRMADA'
                        AND UPPER(p.estadoPago) IN ('SENA_PAGADA', 'PAGADO')
                        AND ia.fechaActividad >= r.fechaIngreso AND ia.fechaActividad < r.fechaEgreso
                      ORDER BY r.fechaIngreso DESC, p.idPedido DESC LIMIT 1) AS idPedido
               FROM inscripcionActividad ia
               JOIN Actividad a ON a.idActividad = ia.idActividad
              WHERE ia.idInscripcion = :inscripcion
              FOR UPDATE"
        );
        $consulta->execute(['inscripcion' => $idInscripcion]);
        $datos = $consulta->fetch();
        if (!$datos) return;

        $cantidad = self::cantidadParticipantes($conexion, $idInscripcion);
        $esPaga = (string) $datos['modalidad'] === 'inscripcion' && (float) $datos['precio'] > 0;
        $activa = strtoupper((string) $datos['estado']) === 'ACTIVA' && $cantidad > 0;

        $cargo = $conexion->prepare(
            "SELECT idCargoActividad, precioUnitario
               FROM cargo_actividad
              WHERE idInscripcion = :inscripcion AND UPPER(estado) = 'PENDIENTE'
              ORDER BY idCargoActividad DESC LIMIT 1 FOR UPDATE"
        );
        $cargo->execute(['inscripcion' => $idInscripcion]);
        $pendiente = $cargo->fetch();

        if (!$esPaga || !$activa) {
            if ($pendiente) {
                $conexion->prepare(
                    "UPDATE cargo_actividad
                        SET estado = 'ANULADO', anuladoEn = NOW(), actualizadoEn = NOW()
                      WHERE idCargoActividad = :cargo"
                )->execute(['cargo' => (int) $pendiente['idCargoActividad']]);
            }
            return;
        }

        if ($pendiente) {
            $precioAceptado = (float) $pendiente['precioUnitario'];
            $conexion->prepare(
                'UPDATE cargo_actividad
                    SET idPedido = :pedido, fechaActividad = :fecha, cantidadParticipantes = :cantidad,
                        montoTotal = :total, actualizadoEn = NOW()
                  WHERE idCargoActividad = :cargo'
            )->execute([
                'pedido' => !empty($datos['idPedido']) ? (int) $datos['idPedido'] : null,
                'fecha' => (string) $datos['fechaActividad'],
                'cantidad' => $cantidad,
                'total' => number_format($precioAceptado * $cantidad, 2, '.', ''),
                'cargo' => (int) $pendiente['idCargoActividad'],
            ]);
            return;
        }

        $precio = (float) $datos['precio'];
        $conexion->prepare(
            "INSERT INTO cargo_actividad
                (idInscripcion, idPedido, idCliente, idActividad, fechaActividad,
                 cantidadParticipantes, precioUnitario, montoTotal, estado)
             VALUES (:inscripcion, :pedido, :cliente, :actividad, :fecha,
                     :cantidad, :precio, :total, 'PENDIENTE')"
        )->execute([
            'inscripcion' => $idInscripcion,
            'pedido' => !empty($datos['idPedido']) ? (int) $datos['idPedido'] : null,
            'cliente' => (int) $datos['idCliente'],
            'actividad' => (int) $datos['idActividad'],
            'fecha' => (string) $datos['fechaActividad'],
            'cantidad' => $cantidad,
            'precio' => number_format($precio, 2, '.', ''),
            'total' => number_format($precio * $cantidad, 2, '.', ''),
        ]);
    }

    /** Anula una inscripción sin borrar su historia. */
    public static function cancelarInscripcion(
        PDO $conexion,
        int $idInscripcion,
        string $motivo
    ): void {
        $conexion->prepare(
            'UPDATE inscripcionActividadAcompanante iaa
                JOIN inscripcionActividad ia
                  ON ia.idCliente = iaa.idClienteTitular AND ia.idActividad = iaa.idActividad
                 AND ia.fechaActividad = iaa.fechaActividad
                SET iaa.activo = 0, iaa.canceladaEn = NOW()
              WHERE ia.idInscripcion = :inscripcion AND iaa.activo = 1'
        )->execute(['inscripcion' => $idInscripcion]);
        $conexion->prepare(
            "UPDATE inscripcionActividad
                SET estado = 'CANCELADA', canceladaEn = NOW(), motivoCancelacion = :motivo
              WHERE idInscripcion = :inscripcion"
        )->execute(['motivo' => $motivo, 'inscripcion' => $idInscripcion]);
        self::sincronizarCargo($conexion, $idInscripcion);
    }

    private static function cantidadParticipantes(PDO $conexion, int $idInscripcion): int
    {
        $consulta = $conexion->prepare(
            'SELECT (CASE WHEN ia.participaTitular = 1 AND UPPER(ia.estado) = \'ACTIVA\' THEN 1 ELSE 0 END)
                    + (SELECT COUNT(*) FROM inscripcionActividadAcompanante iaa
                        WHERE iaa.idClienteTitular = ia.idCliente
                          AND iaa.idActividad = ia.idActividad
                          AND iaa.fechaActividad = ia.fechaActividad AND iaa.activo = 1)
               FROM inscripcionActividad ia
              WHERE ia.idInscripcion = :inscripcion'
        );
        $consulta->execute(['inscripcion' => $idInscripcion]);
        return (int) ($consulta->fetchColumn() ?: 0);
    }

    private static function clienteTieneEstadia(PDO $conexion, int $idCliente, string $fecha): bool
    {
        $consulta = $conexion->prepare(
            "SELECT 1 FROM reservas r
               JOIN detalle_pedido_alojamiento d ON d.idReserva = r.idReserva
               JOIN pedido_alojamiento p ON p.idPedido = d.idPedido
              WHERE r.idCliente = :cliente
                AND UPPER(r.estadoReserva) = 'CONFIRMADA'
                AND UPPER(p.estadoPago) IN ('SENA_PAGADA', 'PAGADO')
                AND :fechaInicio >= r.fechaIngreso AND :fechaFin < r.fechaEgreso
              LIMIT 1"
        );
        $consulta->execute(['cliente' => $idCliente, 'fechaInicio' => $fecha, 'fechaFin' => $fecha]);
        return (bool) $consulta->fetchColumn();
    }

    private static function acompananteTieneEstadia(PDO $conexion, int $idAcompanante, string $fecha): bool
    {
        $consulta = $conexion->prepare(
            "SELECT 1 FROM `reservaAcompañante` ra
               JOIN `acompañante` a ON a.idAcompañante = ra.idAcompañante
               JOIN reservas r ON r.idReserva = ra.idReserva
               JOIN detalle_pedido_alojamiento d ON d.idReserva = r.idReserva
               JOIN pedido_alojamiento p ON p.idPedido = d.idPedido
              WHERE ra.idAcompañante = :acompanante AND a.activo = 1
                AND UPPER(r.estadoReserva) = 'CONFIRMADA'
                AND UPPER(p.estadoPago) IN ('SENA_PAGADA', 'PAGADO')
                AND :fechaInicio >= r.fechaIngreso AND :fechaFin < r.fechaEgreso
              LIMIT 1"
        );
        $consulta->execute(['acompanante' => $idAcompanante, 'fechaInicio' => $fecha, 'fechaFin' => $fecha]);
        return (bool) $consulta->fetchColumn();
    }
}
