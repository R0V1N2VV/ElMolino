<?php
declare(strict_types=1);

final class RepositorioInscripciones
{
    public function __construct(private PDO $conexion) {}

    public function estaInscripto(int $idActividad, int $idCliente): bool
    {
        $consulta = $this->conexion->prepare(
            'SELECT 1 FROM inscripcionActividad
              WHERE idActividad = :idActividad AND idCliente = :idCliente LIMIT 1'
        );
        $consulta->execute(['idActividad' => $idActividad, 'idCliente' => $idCliente]);
        return (bool) $consulta->fetchColumn();
    }

    public function titularParticipa(int $idActividad, int $idCliente): bool
    {
        $consulta = $this->conexion->prepare(
            'SELECT participaTitular FROM inscripcionActividad
              WHERE idActividad = :idActividad AND idCliente = :idCliente LIMIT 1'
        );
        $consulta->execute(['idActividad' => $idActividad, 'idCliente' => $idCliente]);
        return (bool) $consulta->fetchColumn();
    }

    /** @return array<int, array{id: int, nombre: string, apellido: string, dni: string, edad: int, parentesco: string}> */
    public function listarAcompanantes(int $idCliente): array
    {
        $consulta = $this->conexion->prepare(
            'SELECT idAcompañante AS id, nombre, apellido, dni, edad, parentesco
               FROM acompañante WHERE idCliente = :idCliente
              ORDER BY apellido, nombre, idAcompañante'
        );
        $consulta->execute(['idCliente' => $idCliente]);
        return array_map(
            static fn (array $fila): array => [
                'id' => (int) $fila['id'], 'nombre' => (string) $fila['nombre'],
                'apellido' => (string) $fila['apellido'], 'dni' => (string) $fila['dni'],
                'edad' => (int) $fila['edad'], 'parentesco' => (string) $fila['parentesco'],
            ],
            $consulta->fetchAll()
        );
    }

    /** @return int[] */
    public function acompanantesInscriptos(int $idActividad, int $idCliente): array
    {
        $consulta = $this->conexion->prepare(
            'SELECT idAcompañante FROM inscripcionActividadAcompanante
              WHERE idActividad = :idActividad AND idClienteTitular = :idCliente
              ORDER BY idAcompañante'
        );
        $consulta->execute(['idActividad' => $idActividad, 'idCliente' => $idCliente]);
        return array_map('intval', $consulta->fetchAll(PDO::FETCH_COLUMN));
    }

    /** @param int[] $idsAcompanantes */
    public function guardarParticipantes(
        int $idActividad,
        int $idCliente,
        bool $participaTitular,
        array $idsAcompanantes = []
    ): void
    {
        try {
            $this->conexion->beginTransaction();
            $datosActividad = $this->bloquearActividad($idActividad);
            $idsValidos = $this->validarAcompanantes($idCliente, $idsAcompanantes);
            $lugaresSolicitados = ($participaTitular ? 1 : 0) + count($idsValidos);
            if ($lugaresSolicitados === 0) {
                throw new InvalidArgumentException('Seleccioná al menos una persona para participar.');
            }

            $inscripcion = $this->conexion->prepare(
                'SELECT participaTitular FROM inscripcionActividad
                  WHERE idActividad = :idActividad AND idCliente = :idCliente FOR UPDATE'
            );
            $inscripcion->execute(['idActividad' => $idActividad, 'idCliente' => $idCliente]);
            $participacionActual = $inscripcion->fetchColumn();
            $existeInscripcion = $participacionActual !== false;
            $acompanantesActuales = $existeInscripcion
                ? $this->acompanantesInscriptos($idActividad, $idCliente)
                : [];
            $lugaresActuales = ($existeInscripcion && (bool) $participacionActual ? 1 : 0)
                + count($acompanantesActuales);
            $cantidadFinal = $this->cantidadInscriptos($idActividad) - $lugaresActuales + $lugaresSolicitados;
            if ($cantidadFinal > (int) $datosActividad['cupoMax']) {
                $lugaresNuevos = max(1, $lugaresSolicitados - $lugaresActuales);
                throw new RuntimeException($this->mensajeCupo($lugaresNuevos));
            }

            if ($existeInscripcion) {
                $actualizar = $this->conexion->prepare(
                    'UPDATE inscripcionActividad SET participaTitular = :participaTitular
                      WHERE idActividad = :idActividad AND idCliente = :idCliente'
                );
                $actualizar->execute([
                    'participaTitular' => $participaTitular ? 1 : 0,
                    'idActividad' => $idActividad,
                    'idCliente' => $idCliente,
                ]);
            } else {
                $alta = $this->conexion->prepare(
                    'INSERT INTO inscripcionActividad
                        (idCliente, idActividad, participaTitular, fechaInscripcion, asistencia)
                     VALUES (:idCliente, :idActividad, :participaTitular, CURRENT_DATE, 0)'
                );
                $alta->execute([
                    'idCliente' => $idCliente,
                    'idActividad' => $idActividad,
                    'participaTitular' => $participaTitular ? 1 : 0,
                ]);
            }

            $baja = $this->conexion->prepare(
                'DELETE FROM inscripcionActividadAcompanante
                  WHERE idActividad = :idActividad AND idClienteTitular = :idCliente'
            );
            $baja->execute(['idActividad' => $idActividad, 'idCliente' => $idCliente]);
            $this->guardarAcompanantes($idActividad, $idCliente, $idsValidos);
            $this->conexion->commit();
        } catch (Throwable $excepcion) {
            if ($this->conexion->inTransaction()) $this->conexion->rollBack();
            throw $excepcion;
        }
    }

    public function cancelar(int $idActividad, int $idCliente): void
    {
        try {
            $this->conexion->beginTransaction();
            $this->bloquearActividad($idActividad, false);
            $acompanantes = $this->conexion->prepare(
                'DELETE FROM inscripcionActividadAcompanante
                  WHERE idActividad = :idActividad AND idClienteTitular = :idCliente'
            );
            $acompanantes->execute(['idActividad' => $idActividad, 'idCliente' => $idCliente]);

            $consulta = $this->conexion->prepare(
                'DELETE FROM inscripcionActividad
                  WHERE idActividad = :idActividad AND idCliente = :idCliente'
            );
            $consulta->execute(['idActividad' => $idActividad, 'idCliente' => $idCliente]);
            if ($consulta->rowCount() !== 1) {
                throw new InvalidArgumentException('No tenés una inscripción activa en esta actividad.');
            }
            $this->conexion->commit();
        } catch (Throwable $excepcion) {
            if ($this->conexion->inTransaction()) $this->conexion->rollBack();
            throw $excepcion;
        }
    }

    private function bloquearActividad(int $idActividad, bool $exigirActiva = true): array
    {
        $actividad = $this->conexion->prepare(
            'SELECT cupoMax, activa FROM Actividad
              WHERE idActividad = :idActividad FOR UPDATE'
        );
        $actividad->execute(['idActividad' => $idActividad]);
        $datos = $actividad->fetch();
        if (!$datos || ($exigirActiva && !(bool) $datos['activa'])) {
            throw new InvalidArgumentException('La actividad ya no está disponible.');
        }
        return $datos;
    }

    private function cantidadInscriptos(int $idActividad): int
    {
        $cantidad = $this->conexion->prepare(
            'SELECT
                (SELECT COALESCE(SUM(participaTitular), 0) FROM inscripcionActividad WHERE idActividad = :actividadTitulares) +
                (SELECT COUNT(*) FROM inscripcionActividadAcompanante WHERE idActividad = :actividadAcompanantes)'
        );
        $cantidad->execute([
            'actividadTitulares' => $idActividad,
            'actividadAcompanantes' => $idActividad,
        ]);
        return (int) $cantidad->fetchColumn();
    }

    /** @param int[] $idsSolicitados @return int[] */
    private function validarAcompanantes(int $idCliente, array $idsSolicitados): array
    {
        $idsSolicitados = array_values(array_unique(array_filter(
            array_map('intval', $idsSolicitados),
            static fn (int $id): bool => $id > 0
        )));
        if ($idsSolicitados === []) return [];

        $marcadores = implode(',', array_fill(0, count($idsSolicitados), '?'));
        $consulta = $this->conexion->prepare(
            "SELECT idAcompañante FROM acompañante WHERE idCliente = ? AND idAcompañante IN ($marcadores)"
        );
        $consulta->execute([$idCliente, ...$idsSolicitados]);
        $idsValidos = array_map('intval', $consulta->fetchAll(PDO::FETCH_COLUMN));
        sort($idsValidos);
        sort($idsSolicitados);
        if ($idsValidos !== $idsSolicitados) {
            throw new InvalidArgumentException('Uno de los acompañantes seleccionados no pertenece a tu cuenta.');
        }
        return $idsValidos;
    }

    /** @param int[] $idsAcompanantes */
    private function guardarAcompanantes(int $idActividad, int $idCliente, array $idsAcompanantes): void
    {
        if ($idsAcompanantes === []) return;
        $alta = $this->conexion->prepare(
            'INSERT INTO inscripcionActividadAcompanante
                (idClienteTitular, idAcompañante, idActividad, fechaInscripcion, asistencia)
             VALUES (:idCliente, :idAcompanante, :idActividad, CURRENT_DATE, 0)'
        );
        foreach ($idsAcompanantes as $idAcompanante) {
            $alta->execute([
                'idCliente' => $idCliente,
                'idAcompanante' => $idAcompanante,
                'idActividad' => $idActividad,
            ]);
        }
    }

    private function mensajeCupo(int $lugares): string
    {
        return $lugares === 1
            ? 'No queda un lugar disponible para completar ese cambio.'
            : 'No quedan ' . $lugares . ' lugares disponibles para incluir a todas las personas seleccionadas.';
    }
}
