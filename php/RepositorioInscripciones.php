<?php
/**
 * ARCHIVO EXPLICADO: php/RepositorioInscripciones.php
 *
 * Gestiona inscripciones del titular y sus acompañantes, incluyendo los cupos.
 * Las funciones de este archivo están documentadas junto a su declaración.
 */

declare(strict_types=1);

require_once __DIR__ . '/GestorCoherenciaActividades.php';

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
    public function estaInscripto(int $idActividad, int $idCliente, ?string $fechaActividad = null): bool
    {
        $sql =
            "SELECT 1 FROM inscripcionActividad
              WHERE idActividad = :idActividad AND idCliente = :idCliente
                AND UPPER(estado) = 'ACTIVA'";
        if ($fechaActividad !== null) $sql .= ' AND fechaActividad = :fechaActividad';
        $consulta = $this->conexion->prepare($sql . ' LIMIT 1');
        $datos = ['idActividad' => $idActividad, 'idCliente' => $idCliente];
        if ($fechaActividad !== null) $datos['fechaActividad'] = $fechaActividad;
        $consulta->execute($datos);
        return (bool) $consulta->fetchColumn();
    }

    /**
     * Ejecuta la lógica de “titular participa” en RepositorioInscripciones.php.
     *
     * @param mixed $idActividad Valor de id actividad que recibe la función.
     * @param mixed $idCliente Valor de id cliente que recibe la función.
     * @return bool Resultado producido por esta operación.
     */
    public function titularParticipa(int $idActividad, int $idCliente, ?string $fechaActividad = null): bool
    {
        $sql =
            "SELECT participaTitular FROM inscripcionActividad
              WHERE idActividad = :idActividad AND idCliente = :idCliente
                AND UPPER(estado) = 'ACTIVA'";
        if ($fechaActividad !== null) $sql .= ' AND fechaActividad = :fechaActividad';
        $consulta = $this->conexion->prepare($sql . ' ORDER BY fechaActividad LIMIT 1');
        $datos = ['idActividad' => $idActividad, 'idCliente' => $idCliente];
        if ($fechaActividad !== null) $datos['fechaActividad'] = $fechaActividad;
        $consulta->execute($datos);
        return (bool) $consulta->fetchColumn();
    }

    /**
     * Devuelve el día elegido por el titular para participar en la actividad.
     */
    public function fechaSeleccionada(int $idActividad, int $idCliente): ?string
    {
        $consulta = $this->conexion->prepare(
            "SELECT fechaActividad FROM inscripcionActividad
              WHERE idActividad = :idActividad AND idCliente = :idCliente
                AND UPPER(estado) = 'ACTIVA' ORDER BY fechaActividad LIMIT 1"
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
               FROM acompañante WHERE idCliente = :idCliente AND activo = 1
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
    public function acompanantesInscriptos(int $idActividad, int $idCliente, ?string $fechaActividad = null): array
    {
        $sql =
            'SELECT idAcompañante FROM inscripcionActividadAcompanante
              WHERE idActividad = :idActividad AND idClienteTitular = :idCliente AND activo = 1';
        if ($fechaActividad !== null) $sql .= ' AND fechaActividad = :fechaActividad';
        $consulta = $this->conexion->prepare($sql . ' ORDER BY idAcompañante');
        $datos = ['idActividad' => $idActividad, 'idCliente' => $idCliente];
        if ($fechaActividad !== null) $datos['fechaActividad'] = $fechaActividad;
        $consulta->execute($datos);
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
            $this->validarEstadiaConfirmada($idCliente, $fechaActividad);
            $idsValidos = $this->validarAcompanantes($idCliente, $idsAcompanantes, $fechaActividad);
            $lugaresSolicitados = ($participaTitular ? 1 : 0) + count($idsValidos);
            if ($lugaresSolicitados === 0) {
                throw new InvalidArgumentException('Seleccioná al menos una persona para participar.');
            }

            $inscripcion = $this->conexion->prepare(
                'SELECT idInscripcion, participaTitular, fechaActividad, estado FROM inscripcionActividad
                  WHERE idActividad = :idActividad AND idCliente = :idCliente AND fechaActividad = :fechaActividad FOR UPDATE'
            );
            $inscripcion->execute(['idActividad' => $idActividad, 'idCliente' => $idCliente, 'fechaActividad' => $fechaActividad]);
            $inscripcionActual = $inscripcion->fetch();
            $existeRegistro = is_array($inscripcionActual);
            $estabaActiva = $existeRegistro && strtoupper((string) $inscripcionActual['estado']) === 'ACTIVA';
            $acompanantesActuales = $estabaActiva
                ? $this->acompanantesInscriptos($idActividad, $idCliente, $fechaActividad)
                : [];
            $lugaresActuales = ($estabaActiva && (bool) $inscripcionActual['participaTitular'] ? 1 : 0)
                + count($acompanantesActuales);
            $misLugaresEnLaFecha = $estabaActiva ? $lugaresActuales : 0;
            $cantidadFinal = $this->cantidadInscriptos($idActividad, $fechaActividad) - $misLugaresEnLaFecha + $lugaresSolicitados;
            if ($cantidadFinal > (int) $datosActividad['cupoMax']) {
                $lugaresNuevos = max(1, $lugaresSolicitados - $lugaresActuales);
                throw new RuntimeException($this->mensajeCupo($lugaresNuevos));
            }

            if ($existeRegistro) {
                $actualizar = $this->conexion->prepare(
                    "UPDATE inscripcionActividad
                        SET participaTitular = :participaTitular,
                            estado = 'ACTIVA', canceladaEn = NULL, motivoCancelacion = NULL,
                            actualizadaEn = NOW()
                      WHERE idActividad = :idActividad AND idCliente = :idCliente AND fechaActividad = :fechaActividad"
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
                        (idCliente, idActividad, participaTitular, fechaActividad, fechaInscripcion, asistencia, estado)
                     VALUES (:idCliente, :idActividad, :participaTitular, :fechaActividad, CURRENT_DATE, 0, \'ACTIVA\')'
                );
                $alta->execute([
                    'idCliente' => $idCliente,
                    'idActividad' => $idActividad,
                    'participaTitular' => $participaTitular ? 1 : 0,
                    'fechaActividad' => $fechaActividad,
                ]);
            }

            $baja = $this->conexion->prepare(
                'UPDATE inscripcionActividadAcompanante
                    SET activo = 0, canceladaEn = NOW()
                  WHERE idActividad = :idActividad AND idClienteTitular = :idCliente AND fechaActividad = :fechaActividad'
            );
            $baja->execute(['idActividad' => $idActividad, 'idCliente' => $idCliente, 'fechaActividad' => $fechaActividad]);
            $this->guardarAcompanantes($idActividad, $idCliente, $idsValidos, $fechaActividad);
            $idInscripcion = $existeRegistro
                ? (int) $inscripcionActual['idInscripcion']
                : (int) $this->conexion->lastInsertId();
            GestorCoherenciaActividades::sincronizarCargo($this->conexion, $idInscripcion);
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
    public function cancelar(int $idActividad, int $idCliente, string $fechaActividad): void
    {
        try {
            $this->conexion->beginTransaction();
            $consulta = $this->conexion->prepare(
                "SELECT idInscripcion, fechaActividad FROM inscripcionActividad
                  WHERE idActividad = :idActividad AND idCliente = :idCliente AND fechaActividad = :fechaActividad
                    AND UPPER(estado) = 'ACTIVA' FOR UPDATE"
            );
            $consulta->execute(['idActividad' => $idActividad, 'idCliente' => $idCliente, 'fechaActividad' => $fechaActividad]);
            $inscripcion = $consulta->fetch();
            if (!$inscripcion) {
                throw new InvalidArgumentException('No tenés una inscripción activa en esta actividad.');
            }
            if ((string) $inscripcion['fechaActividad'] < date('Y-m-d')) {
                throw new RuntimeException('No se puede cancelar una actividad que ya ocurrió. El registro se conserva como historial.');
            }
            GestorCoherenciaActividades::cancelarInscripcion(
                $this->conexion,
                (int) $inscripcion['idInscripcion'],
                'CANCELADA_POR_CLIENTE'
            );
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
            'SELECT cupoMax, activa, dias, modalidad, precio FROM Actividad
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
                  WHERE idActividad = :actividadTitulares AND fechaActividad = :fechaTitulares
                    AND UPPER(estado) = \'ACTIVA\') +
                (SELECT COUNT(*)
                   FROM inscripcionActividadAcompanante iaa
                   JOIN inscripcionActividad ia
                     ON ia.idCliente = iaa.idClienteTitular AND ia.idActividad = iaa.idActividad
                    AND ia.fechaActividad = iaa.fechaActividad
                  WHERE iaa.idActividad = :actividadAcompanantes AND ia.fechaActividad = :fechaAcompanantes
                    AND UPPER(ia.estado) = \'ACTIVA\' AND iaa.activo = 1)'
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

    /**
     * Devuelve solamente fechas de la actividad que estén dentro de una
     * estadía cuya seña ya fue acreditada.
     *
     * @return array<int, array{valor:string, etiqueta:string}>
     */
    public function fechasDisponiblesParaCliente(Actividad $actividad, int $idCliente): array
    {
        $rangos = $this->estanciasConfirmadas($idCliente);
        if ($rangos === []) return [];

        return array_values(array_filter(
            $actividad->fechasDisponibles(100),
            static function (array $fecha) use ($rangos): bool {
                $valor = (string) ($fecha['valor'] ?? '');
                foreach ($rangos as $rango) {
                    if ($valor >= $rango['ingreso'] && $valor < $rango['egreso']) return true;
                }
                return false;
            }
        ));
    }

    public function tieneEstadiaConfirmada(int $idCliente): bool
    {
        return $this->estanciasConfirmadas($idCliente) !== [];
    }

    /** @return array<int, array{ingreso:string, egreso:string}> */
    private function estanciasConfirmadas(int $idCliente): array
    {
        $consulta = $this->conexion->prepare(
            "SELECT DISTINCT r.fechaIngreso AS ingreso, r.fechaEgreso AS egreso
               FROM reservas r
               JOIN detalle_pedido_alojamiento d ON d.idReserva = r.idReserva
               JOIN pedido_alojamiento p ON p.idPedido = d.idPedido
              WHERE r.idCliente = :cliente
                AND UPPER(r.estadoReserva) = 'CONFIRMADA'
                AND UPPER(p.estadoPago) IN ('SENA_PAGADA', 'PAGADO')
                AND r.fechaEgreso >= CURRENT_DATE
              ORDER BY r.fechaIngreso"
        );
        $consulta->execute(['cliente' => $idCliente]);
        return array_map(
            static fn(array $fila): array => [
                'ingreso' => (string) $fila['ingreso'],
                'egreso' => (string) $fila['egreso'],
            ],
            $consulta->fetchAll()
        );
    }

    private function validarEstadiaConfirmada(int $idCliente, string $fechaActividad): void
    {
        $consulta = $this->conexion->prepare(
            "SELECT 1
               FROM reservas r
               JOIN detalle_pedido_alojamiento d ON d.idReserva = r.idReserva
               JOIN pedido_alojamiento p ON p.idPedido = d.idPedido
              WHERE r.idCliente = :cliente
                AND UPPER(r.estadoReserva) = 'CONFIRMADA'
                AND UPPER(p.estadoPago) IN ('SENA_PAGADA', 'PAGADO')
                AND :fechaInicio >= r.fechaIngreso AND :fechaFin < r.fechaEgreso
              LIMIT 1 FOR UPDATE"
        );
        $consulta->execute(['cliente' => $idCliente, 'fechaInicio' => $fechaActividad, 'fechaFin' => $fechaActividad]);
        if (!$consulta->fetchColumn()) {
            throw new RuntimeException('Para inscribirte necesitás una estadía confirmada y elegir un día comprendido dentro de esa estadía.');
        }
    }

    /** @param int[] $idsSolicitados @return int[] */
    private function validarAcompanantes(int $idCliente, array $idsSolicitados, string $fechaActividad): array
    {
        $idsSolicitados = array_values(array_unique(array_filter(
            array_map('intval', $idsSolicitados),
            // Esta función anónima procesa cada elemento recibido sin crear un método público adicional.
            static fn (int $id): bool => $id > 0
        )));
        if ($idsSolicitados === []) return [];

        $marcadores = implode(',', array_fill(0, count($idsSolicitados), '?'));
        $consulta = $this->conexion->prepare(
            "SELECT a.idAcompañante
               FROM acompañante a
              WHERE a.idCliente = ? AND a.activo = 1
                AND a.idAcompañante IN ($marcadores)
                AND EXISTS (
                    SELECT 1
                      FROM `reservaAcompañante` ra
                      JOIN reservas r ON r.idReserva = ra.idReserva
                      JOIN detalle_pedido_alojamiento d ON d.idReserva = r.idReserva
                      JOIN pedido_alojamiento p ON p.idPedido = d.idPedido
                     WHERE ra.idAcompañante = a.idAcompañante
                       AND UPPER(r.estadoReserva) = 'CONFIRMADA'
                       AND UPPER(p.estadoPago) IN ('SENA_PAGADA', 'PAGADO')
                       AND ? >= r.fechaIngreso AND ? < r.fechaEgreso
                )"
        );
        $consulta->execute([$idCliente, ...$idsSolicitados, $fechaActividad, $fechaActividad]);
        $idsValidos = array_map('intval', $consulta->fetchAll(PDO::FETCH_COLUMN));
        sort($idsValidos);
        sort($idsSolicitados);
        if ($idsValidos !== $idsSolicitados) {
            throw new InvalidArgumentException('Uno de los acompañantes no está activo o no figura como huésped de la estadía elegida.');
        }
        return $idsValidos;
    }

    /** @param int[] $idsAcompanantes */
    private function guardarAcompanantes(int $idActividad, int $idCliente, array $idsAcompanantes, string $fechaActividad): void
    {
        if ($idsAcompanantes === []) return;
        $alta = $this->conexion->prepare(
            'INSERT INTO inscripcionActividadAcompanante
                (idClienteTitular, idAcompañante, idActividad, fechaActividad, fechaInscripcion, asistencia, activo, canceladaEn)
             VALUES (:idCliente, :idAcompanante, :idActividad, :fechaActividad, CURRENT_DATE, 0, 1, NULL)
             ON DUPLICATE KEY UPDATE activo = 1, canceladaEn = NULL, fechaInscripcion = CURRENT_DATE'
        );
        foreach ($idsAcompanantes as $idAcompanante) {
            $alta->execute([
                'idCliente' => $idCliente,
                'idAcompanante' => $idAcompanante,
                'idActividad' => $idActividad,
                'fechaActividad' => $fechaActividad,
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
