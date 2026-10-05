<?php
declare(strict_types=1);

final class RepositorioInscripciones
{
    public function __construct(private PDO $conexion) {}

    public function estaInscripto(int $idActividad, int $idCliente): bool
    {
        $consulta = $this->conexion->prepare(
            'SELECT 1
               FROM inscripcionActividad
              WHERE idActividad = :idActividad AND idCliente = :idCliente
              LIMIT 1'
        );
        $consulta->execute(['idActividad' => $idActividad, 'idCliente' => $idCliente]);
        return (bool) $consulta->fetchColumn();
    }

    public function inscribir(int $idActividad, int $idCliente): void
    {
        try {
            $this->conexion->beginTransaction();
            $actividad = $this->conexion->prepare(
                'SELECT cupoMax, activa
                   FROM Actividad
                  WHERE idActividad = :idActividad
                  FOR UPDATE'
            );
            $actividad->execute(['idActividad' => $idActividad]);
            $datosActividad = $actividad->fetch();
            if (!$datosActividad || !(bool) $datosActividad['activa']) {
                throw new InvalidArgumentException('La actividad ya no está disponible.');
            }
            if ($this->estaInscripto($idActividad, $idCliente)) {
                throw new InvalidArgumentException('Ya estás inscripto en esta actividad.');
            }

            $cantidad = $this->conexion->prepare(
                'SELECT COUNT(*) FROM inscripcionActividad WHERE idActividad = :idActividad'
            );
            $cantidad->execute(['idActividad' => $idActividad]);
            if ((int) $cantidad->fetchColumn() >= (int) $datosActividad['cupoMax']) {
                throw new RuntimeException('La actividad ya no tiene lugares disponibles.');
            }

            $alta = $this->conexion->prepare(
                'INSERT INTO inscripcionActividad (idCliente, idActividad, fechaInscripcion, asistencia)
                 VALUES (:idCliente, :idActividad, CURRENT_DATE, 0)'
            );
            $alta->execute(['idCliente' => $idCliente, 'idActividad' => $idActividad]);
            $this->conexion->commit();
        } catch (Throwable $excepcion) {
            if ($this->conexion->inTransaction()) $this->conexion->rollBack();
            throw $excepcion;
        }
    }

    public function cancelar(int $idActividad, int $idCliente): void
    {
        $consulta = $this->conexion->prepare(
            'DELETE FROM inscripcionActividad
              WHERE idActividad = :idActividad AND idCliente = :idCliente'
        );
        $consulta->execute(['idActividad' => $idActividad, 'idCliente' => $idCliente]);
        if ($consulta->rowCount() !== 1) {
            throw new InvalidArgumentException('No tenés una inscripción activa en esta actividad.');
        }
    }
}
