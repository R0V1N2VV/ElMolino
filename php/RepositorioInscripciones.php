<?php
/**
 * ARCHIVO EXPLICADO: php/RepositorioInscripciones.php
 *
 * Gestiona inscripciones del titular y sus acompañantes, incluyendo los cupos.
 * Las funciones de este archivo están documentadas junto a su declaración.
 */

declare(strict_types=1);

final class RepositorioInscripciones
{
    /**
     * Inicializa el objeto y deja preparadas las dependencias que utilizarán sus demás métodos.
     *
     * @param mixed $conexion Valor de conexion que recibe la función.
     */
    public function __construct(private PDO $conexion) {}

    /**
     * Comprueba esta inscripto y devuelve verdadero o falso según se cumpla la condición.
     *
     * @param mixed $idActividad Valor de id actividad que recibe la función.
     * @param mixed $idCliente Valor de id cliente que recibe la función.
     * @return bool Resultado producido por esta operación.
     */
    public function estaInscripto(int $idActividad, int $idCliente): bool
    {
        $consulta = $this->conexion->prepare(
            'SELECT 1 FROM inscripcionActividad
              WHERE idActividad = :idActividad AND idCliente = :idCliente LIMIT 1'
        );
        $consulta->execute(['idActividad' => $idActividad, 'idCliente' => $idCliente]);
        return (bool) $consulta->fetchColumn();
    }

    /**
     * Ejecuta la lógica de “titular participa” en RepositorioInscripciones.php.
     *
     * @param mixed $idActividad Valor de id actividad que recibe la función.
     * @param mixed $idCliente Valor de id cliente que recibe la función.
     * @return bool Resultado producido por esta operación.
     */
    public function titularParticipa(int $idActividad, int $idCliente): bool
    {
        $consulta = $this->conexion->prepare(
            'SELECT participaTitular FROM inscripcionActividad
              WHERE idActividad = :idActividad AND idCliente = :idCliente LIMIT 1'
        );
        $consulta->execute(['idActividad' => $idActividad, 'idCliente' => $idCliente]);
        return (bool) $consulta->fetchColumn();
    }

    /**
     * Devuelve el día elegido por el titular para participar en la actividad.
     */
    public function fechaSeleccionada(int $idActividad, int $idCliente): ?string
    {
        $consulta = $this->conexion->prepare(
            'SELECT fechaActividad FROM inscripcionActividad
              WHERE idActividad = :idActividad AND idCliente = :idCliente LIMIT 1'
        );
        $consulta->execute(['idActividad' => $idActividad, 'idCliente' => $idCliente]);
        $fecha = $consulta->fetchColumn();
        return $fecha === false ? null : (string) $fecha;
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
            // Esta función anónima procesa cada elemento recibido sin crear un método público adicional.
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
        array $idsAcompanantes = [],
        string $fechaActividad = ''
    ): void
    {
        try {
            $this->conexion->beginTransaction();
            $datosActividad = $this->bloquearActividad($idActividad);
            $this->validarFechaActividad($fechaActividad, (string) ($datosActividad['dias'] ?? ''));
            $idsValidos = $this->validarAcompanantes($idCliente, $idsAcompanantes);
            $lugaresSolicitados = ($participaTitular ? 1 : 0) + count($idsValidos);
            if ($lugaresSolicitados === 0) {
                throw new InvalidArgumentException('Seleccioná al menos una persona para participar.');
            }

            $inscripcion = $this->conexion->prepare(
                'SELECT participaTitular, fechaActividad FROM inscripcionActividad
                  WHERE idActividad = :idActividad AND idCliente = :idCliente FOR UPDATE'
            );
            $inscripcion->execute(['idActividad' => $idActividad, 'idCliente' => $idCliente]);
            $inscripcionActual = $inscripcion->fetch();
            $existeInscripcion = is_array($inscripcionActual);
            $acompanantesActuales = $existeInscripcion
                ? $this->acompanantesInscriptos($idActividad, $idCliente)
                : [];
            $lugaresActuales = ($existeInscripcion && (bool) $inscripcionActual['participaTitular'] ? 1 : 0)
                + count($acompanantesActuales);
            $misLugaresEnLaFecha = $existeInscripcion && (string) $inscripcionActual['fechaActividad'] === $fechaActividad
                ? $lugaresActuales
                : 0;
            $cantidadFinal = $this->cantidadInscriptos($idActividad, $fechaActividad) - $misLugaresEnLaFecha + $lugaresSolicitados;
            if ($cantidadFinal > (int) $datosActividad['cupoMax']) {
                $lugaresNuevos = max(1, $lugaresSolicitados - $lugaresActuales);
                throw new RuntimeException($this->mensajeCupo($lugaresNuevos));
            }

            if ($existeInscripcion) {
                $actualizar = $this->conexion->prepare(
                    'UPDATE inscripcionActividad SET participaTitular = :participaTitular, fechaActividad = :fechaActividad
                      WHERE idActividad = :idActividad AND idCliente = :idCliente'
                );
                $actualizar->execute([
                    'participaTitular' => $participaTitular ? 1 : 0,
                    'fechaActividad' => $fechaActividad,
                    'idActividad' => $idActividad,
                    'idCliente' => $idCliente,
                ]);
            } else {
                $alta = $this->conexion->prepare(
                    'INSERT INTO inscripcionActividad
                        (idCliente, idActividad, participaTitular, fechaActividad, fechaInscripcion, asistencia)
                     VALUES (:idCliente, :idActividad, :participaTitular, :fechaActividad, CURRENT_DATE, 0)'
                );
                $alta->execute([
                    'idCliente' => $idCliente,
                    'idActividad' => $idActividad,
                    'participaTitular' => $participaTitular ? 1 : 0,
                    'fechaActividad' => $fechaActividad,
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

    /**
     * Cancela la operación de manera controlada.
     *
     * @param mixed $idActividad Valor de id actividad que recibe la función.
     * @param mixed $idCliente Valor de id cliente que recibe la función.
     */
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

    /**
     * Ejecuta la lógica de “bloquear actividad” en RepositorioInscripciones.php.
     *
     * @param mixed $idActividad Valor de id actividad que recibe la función.
     * @param mixed $exigirActiva Valor de exigir activa que recibe la función.
     * @return array Resultado producido por esta operación.
     */
    private function bloquearActividad(int $idActividad, bool $exigirActiva = true): array
    {
        $actividad = $this->conexion->prepare(
            'SELECT cupoMax, activa, dias FROM Actividad
              WHERE idActividad = :idActividad FOR UPDATE'
        );
        $actividad->execute(['idActividad' => $idActividad]);
        $datos = $actividad->fetch();
        if (!$datos || ($exigirActiva && !(bool) $datos['activa'])) {
            throw new InvalidArgumentException('La actividad ya no está disponible.');
        }
        return $datos;
    }

    /**
     * Calcula cantidad inscriptos y devuelve el resultado.
     *
     * @param mixed $idActividad Valor de id actividad que recibe la función.
     * @return int Resultado producido por esta operación.
     */
    private function cantidadInscriptos(int $idActividad, string $fechaActividad): int
    {
        $cantidad = $this->conexion->prepare(
            'SELECT
                (SELECT COALESCE(SUM(participaTitular), 0)
                   FROM inscripcionActividad
                  WHERE idActividad = :actividadTitulares AND fechaActividad = :fechaTitulares) +
                (SELECT COUNT(*)
                   FROM inscripcionActividadAcompanante iaa
                   JOIN inscripcionActividad ia
                     ON ia.idCliente = iaa.idClienteTitular AND ia.idActividad = iaa.idActividad
                  WHERE iaa.idActividad = :actividadAcompanantes AND ia.fechaActividad = :fechaAcompanantes)'
        );
        $cantidad->execute([
            'actividadTitulares' => $idActividad,
            'fechaTitulares' => $fechaActividad,
            'actividadAcompanantes' => $idActividad,
            'fechaAcompanantes' => $fechaActividad,
        ]);
        return (int) $cantidad->fetchColumn();
    }

    /**
     * Verifica que la fecha esté dentro del plazo habilitado y coincida con los
     * días semanales configurados por coordinación.
     */
    private function validarFechaActividad(string $fecha, string $dias): void
    {
        $modelo = new Actividad(0, '', '', '', '', '', '', '', '', $dias, '', '', '', '', '', 0, '', '', 0, false, '');
        if (!$modelo->admiteFecha($fecha)) {
            throw new InvalidArgumentException('Elegí una de las fechas disponibles para esta actividad.');
        }
    }

    /** @param int[] $idsSolicitados @return int[] */
    private function validarAcompanantes(int $idCliente, array $idsSolicitados): array
    {
        $idsSolicitados = array_values(array_unique(array_filter(
            array_map('intval', $idsSolicitados),
            // Esta función anónima procesa cada elemento recibido sin crear un método público adicional.
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

    /**
     * Ejecuta la lógica de “mensaje cupo” en RepositorioInscripciones.php.
     *
     * @param mixed $lugares Valor de lugares que recibe la función.
     * @return string Resultado producido por esta operación.
     */
    private function mensajeCupo(int $lugares): string
    {
        return $lugares === 1
            ? 'No queda un lugar disponible para completar ese cambio.'
            : 'No quedan ' . $lugares . ' lugares disponibles para incluir a todas las personas seleccionadas.';
    }
}
