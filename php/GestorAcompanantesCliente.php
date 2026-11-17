<?php
/** Permite que el cliente administre las personas guardadas en su cuenta. */
declare(strict_types=1);

require_once __DIR__ . '/GestorCoherenciaActividades.php';

final class GestorAcompanantesCliente
{
    public function __construct(private PDO $conexion) {}

    public function agregar(int $idCliente, array $entrada): void
    {
        $datos = [
            'nombre' => trim((string) ($entrada['nombre'] ?? '')),
            'apellido' => trim((string) ($entrada['apellido'] ?? '')),
            'dni' => preg_replace('/\D+/', '', (string) ($entrada['dni'] ?? '')),
            'edad' => filter_var($entrada['edad'] ?? null, FILTER_VALIDATE_INT),
            'parentesco' => trim((string) ($entrada['parentesco'] ?? '')),
        ];
        if ($datos['nombre'] === '' || $datos['apellido'] === '' || $datos['parentesco'] === '') {
            throw new InvalidArgumentException('Completá nombre, apellido y vínculo del acompañante.');
        }
        if (strlen($datos['dni']) < 7 || strlen($datos['dni']) > 8) {
            throw new InvalidArgumentException('Ingresá un DNI válido para el acompañante.');
        }
        if ($datos['edad'] === false || $datos['edad'] < 0 || $datos['edad'] > 120) {
            throw new InvalidArgumentException('Ingresá una edad válida para el acompañante.');
        }

        $this->conexion->beginTransaction();
        try {
            $existente = $this->conexion->prepare(
                'SELECT idAcompañante, idCliente, activo FROM `acompañante` WHERE dni = :dni FOR UPDATE'
            );
            $existente->execute(['dni' => $datos['dni']]);
            $fila = $existente->fetch();
            if ($fila && (int) $fila['idCliente'] !== $idCliente) {
                throw new RuntimeException('Ese DNI ya está asociado a otra cuenta.');
            }
            if ($fila && (bool) $fila['activo']) {
                throw new RuntimeException('Ese acompañante ya está guardado en tu cuenta.');
            }

            if ($fila) {
                $datos['id'] = (int) $fila['idAcompañante'];
                $datos['cliente'] = $idCliente;
                $this->conexion->prepare(
                    'UPDATE `acompañante`
                        SET dni = :dni, nombre = :nombre, apellido = :apellido, edad = :edad,
                            parentesco = :parentesco, activo = 1
                      WHERE idAcompañante = :id AND idCliente = :cliente'
                )->execute($datos);
            } else {
                $datos['cliente'] = $idCliente;
                $this->conexion->prepare(
                    'INSERT INTO `acompañante`
                        (idCliente, dni, nombre, apellido, edad, parentesco, activo)
                     VALUES (:cliente, :dni, :nombre, :apellido, :edad, :parentesco, 1)'
                )->execute($datos);
            }
            $this->conexion->commit();
        } catch (Throwable $error) {
            if ($this->conexion->inTransaction()) $this->conexion->rollBack();
            throw $error;
        }
    }

    public function quitar(int $idAcompanante, int $idCliente): void
    {
        $this->conexion->beginTransaction();
        try {
            $consulta = $this->conexion->prepare(
                'SELECT idAcompañante FROM `acompañante`
                  WHERE idAcompañante = :acompanante AND idCliente = :cliente AND activo = 1
                  FOR UPDATE'
            );
            $consulta->execute(['acompanante' => $idAcompanante, 'cliente' => $idCliente]);
            if (!$consulta->fetchColumn()) throw new RuntimeException('No encontramos ese acompañante en tu cuenta.');

            // Lo quita de reservas futuras y actividades pendientes, pero conserva
            // los registros históricos de estadías que ya terminaron.
            $this->conexion->prepare(
                'DELETE ra FROM `reservaAcompañante` ra
                  JOIN reservas r ON r.idReserva = ra.idReserva
                 WHERE ra.idAcompañante = :acompanante AND r.fechaIngreso > CURRENT_DATE'
            )->execute(['acompanante' => $idAcompanante]);
            $this->conexion->prepare(
                'UPDATE `acompañante` SET activo = 0
                  WHERE idAcompañante = :acompanante AND idCliente = :cliente'
            )->execute(['acompanante' => $idAcompanante, 'cliente' => $idCliente]);
            GestorCoherenciaActividades::sincronizarCliente(
                $this->conexion,
                $idCliente,
                'ACOMPAÑANTE_QUITADO_DE_LA_CUENTA'
            );
            $this->conexion->commit();
        } catch (Throwable $error) {
            if ($this->conexion->inTransaction()) $this->conexion->rollBack();
            throw $error;
        }
    }
}
