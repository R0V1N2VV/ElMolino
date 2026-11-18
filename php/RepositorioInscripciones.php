<?php
/**
 * ARCHIVO EXPLICADO: php/RepositorioInscripciones.php
 *
 * Gestiona inscripciones del titular y sus acompañantes, incluyendo los cupos.
 * Las funciones de este archivo están documentadas junto a su declaración.
 */

declare(strict_types=1);

require_once __DIR__ . '/GestorCoherenciaActividades.php';
require_once __DIR__ . '/RelojDemostracion.php';

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
    /**
     * Guarda la inscripción y devuelve false únicamente cuando el cupo se
     * llenó y el cliente quedó anotado en la lista de espera.
     */
    public function guardarParticipantes(
        int $idActividad,
        int $idCliente,
        bool $participaTitular,
        array $idsAcompanantes = [],
        string $fechaActividad = ''
    ): bool
    {
        try {
            $this->conexion->beginTransaction();
            $datosActividad = $this->bloquearActividad($idActividad);
            $this->validarFechaActividad($fechaActividad, (string) ($datosActividad['dias'] ?? ''));
            $this->validarJornadaAbierta($idActividad, $fechaActividad);
            $this->validarEstadiaConfirmada($idCliente, $fechaActividad);
            $idsValidos = $this->validarAcompanantes($idCliente, $idsAcompanantes, $fechaActividad);
            $lugaresSolicitados = ($participaTitular ? 1 : 0) + count($idsValidos);
            if ($lugaresSolicitados === 0) {
                throw new InvalidArgumentException('Seleccioná al menos una persona para participar.');
            }
            if ($participaTitular) {
                $this->validarSinCruceHorario(
                    $idCliente,
                    $idActividad,
                    $fechaActividad,
                    (string) ($datosActividad['horario'] ?? '')
                );
            }
            $this->validarAcompanantesSinCruceHorario(
                $idsValidos,
                $idActividad,
                $fechaActividad,
                (string) ($datosActividad['horario'] ?? '')
            );

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
                // No se pierde la intención del cliente: queda en espera, pero
                // no se crea una inscripción activa ni se ocupa un cupo real.
                if (!$this->listaEsperaDisponible()) {
                    throw new RuntimeException('La actividad alcanzó su cupo máximo. Podés consultar en recepción si se libera un lugar.');
                }
                $this->registrarEnListaDeEspera($idActividad, $idCliente, $fechaActividad, $lugaresSolicitados);
                $this->conexion->commit();
                return false;
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
                     VALUES (:idCliente, :idActividad, :participaTitular, :fechaActividad, :fechaInscripcion, 0, \'ACTIVA\')'
                );
                $alta->execute([
                    'idCliente' => $idCliente,
                    'idActividad' => $idActividad,
                    'participaTitular' => $participaTitular ? 1 : 0,
                    'fechaActividad' => $fechaActividad,
                    'fechaInscripcion' => RelojDemostracion::fecha($this->conexion),
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
            // Si la persona consiguió lugar, cualquier espera anterior para la
            // misma actividad y fecha deja de tener sentido.
            if ($this->listaEsperaDisponible()) {
                $this->conexion->prepare(
                    "DELETE FROM listaEsperaActividad
                      WHERE idActividad=:actividad AND idCliente=:cliente AND fechaActividad=:fecha"
                )->execute(['actividad' => $idActividad, 'cliente' => $idCliente, 'fecha' => $fechaActividad]);
            }
            $this->conexion->commit();
            return true;
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
            if ((string) $inscripcion['fechaActividad'] < RelojDemostracion::fecha($this->conexion)) {
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

    /** Quita una solicitud de la lista de espera que pertenece al cliente. */
    public function cancelarEspera(int $idActividad, int $idCliente, string $fechaActividad): void
    {
        $baja = $this->conexion->prepare(
            "UPDATE listaEsperaActividad
                SET estado='CANCELADA', canceladaEn=NOW()
              WHERE idActividad=:actividad AND idCliente=:cliente AND fechaActividad=:fecha
                AND estado='PENDIENTE'"
        );
        $baja->execute(['actividad' => $idActividad, 'cliente' => $idCliente, 'fecha' => $fechaActividad]);
        if ($baja->rowCount() === 0) {
            throw new InvalidArgumentException('No encontramos una solicitud pendiente en la lista de espera.');
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
            'SELECT cupoMax, activa, dias, horario, modalidad, precio FROM Actividad
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
     * Evita que el titular reserve dos propuestas con una franja horaria que
     * se superpone el mismo día. Solo se evalúan horarios con inicio y fin
     * explícitos (por ejemplo, “15:00 a 16:00”), para no inventar duraciones.
     */
    private function validarSinCruceHorario(
        int $idCliente,
        int $idActividad,
        string $fechaActividad,
        string $horarioSolicitado
    ): void {
        $intervaloSolicitado = $this->intervaloHorario($horarioSolicitado);
        if ($intervaloSolicitado === null) return;

        $consulta = $this->conexion->prepare(
            "SELECT a.nombre, a.horario
               FROM inscripcionActividad ia
               JOIN Actividad a ON a.idActividad=ia.idActividad
              WHERE ia.idCliente=:cliente AND ia.fechaActividad=:fecha
                AND ia.idActividad<>:actividad AND ia.participaTitular=1
                AND UPPER(ia.estado)='ACTIVA'"
        );
        $consulta->execute([
            'cliente' => $idCliente,
            'fecha' => $fechaActividad,
            'actividad' => $idActividad,
        ]);
        foreach ($consulta->fetchAll() as $existente) {
            $intervaloExistente = $this->intervaloHorario((string) $existente['horario']);
            if ($intervaloExistente === null) continue;
            [$inicioNuevo, $finNuevo] = $intervaloSolicitado;
            [$inicioExistente, $finExistente] = $intervaloExistente;
            if ($inicioNuevo < $finExistente && $inicioExistente < $finNuevo) {
                throw new RuntimeException(
                    'El horario se superpone con tu inscripción a “' . $existente['nombre'] . '” ('
                    . $existente['horario'] . '). Elegí otra fecha u horario.'
                );
            }
        }
    }

    /**
     * Aplica la misma regla de horarios a cada acompañante seleccionado.
     * Un acompañante no puede quedar anotado al mismo tiempo en dos actividades
     * distintas, aun cuando las reservas hayan sido cargadas por titulares
     * diferentes.
     *
     * @param int[] $idsAcompanantes
     */
    private function validarAcompanantesSinCruceHorario(
        array $idsAcompanantes,
        int $idActividad,
        string $fechaActividad,
        string $horarioSolicitado
    ): void {
        $intervaloSolicitado = $this->intervaloHorario($horarioSolicitado);
        if ($intervaloSolicitado === null || $idsAcompanantes === []) return;

        $consulta = $this->conexion->prepare(
            "SELECT CONCAT(ac.nombre, ' ', ac.apellido) AS acompanante, a.nombre, a.horario
               FROM inscripcionActividadAcompanante iaa
               JOIN inscripcionActividad ia
                 ON ia.idCliente = iaa.idClienteTitular
                AND ia.idActividad = iaa.idActividad
                AND ia.fechaActividad = iaa.fechaActividad
               JOIN Actividad a ON a.idActividad = iaa.idActividad
               JOIN acompañante ac ON ac.idAcompañante = iaa.idAcompañante
              WHERE iaa.idAcompañante = :acompanante
                AND iaa.fechaActividad = :fecha
                AND iaa.idActividad <> :actividad
                AND iaa.activo = 1
                AND UPPER(ia.estado) = 'ACTIVA'"
        );

        foreach ($idsAcompanantes as $idAcompanante) {
            $consulta->execute([
                'acompanante' => $idAcompanante,
                'fecha' => $fechaActividad,
                'actividad' => $idActividad,
            ]);
            foreach ($consulta->fetchAll() as $existente) {
                $intervaloExistente = $this->intervaloHorario((string) $existente['horario']);
                if ($intervaloExistente === null) continue;
                [$inicioNuevo, $finNuevo] = $intervaloSolicitado;
                [$inicioExistente, $finExistente] = $intervaloExistente;
                if ($inicioNuevo < $finExistente && $inicioExistente < $finNuevo) {
                    throw new RuntimeException(
                        '“' . $existente['acompanante'] . '” ya está anotado/a en “'
                        . $existente['nombre'] . '” (' . $existente['horario']
                        . ') para ese mismo horario.'
                    );
                }
            }
        }
    }

    /** @return array{string,string}|null */
    private function intervaloHorario(string $horario): ?array
    {
        if (!preg_match('/\b([01]?\d|2[0-3]):([0-5]\d)\s*(?:a|hasta|-)\s*([01]?\d|2[0-3]):([0-5]\d)\b/ui', $horario, $coincidencias)) {
            return null;
        }
        $inicio = str_pad($coincidencias[1], 2, '0', STR_PAD_LEFT) . ':' . $coincidencias[2];
        $fin = str_pad($coincidencias[3], 2, '0', STR_PAD_LEFT) . ':' . $coincidencias[4];
        return $fin > $inicio ? [$inicio, $fin] : null;
    }

    /** Guarda una sola solicitud vigente por cliente, actividad y fecha. */
    private function registrarEnListaDeEspera(
        int $idActividad,
        int $idCliente,
        string $fechaActividad,
        int $cantidadSolicitada
    ): void {
        $alta = $this->conexion->prepare(
            "INSERT INTO listaEsperaActividad
                (idActividad,idCliente,fechaActividad,cantidadPersonas,estado,creadoEn,canceladaEn)
             VALUES (:actividad,:cliente,:fecha,:cantidad,'PENDIENTE',NOW(),NULL)
             ON DUPLICATE KEY UPDATE cantidadPersonas=VALUES(cantidadPersonas),
                 estado='PENDIENTE', creadoEn=NOW(), canceladaEn=NULL"
        );
        $alta->execute([
            'actividad' => $idActividad,
            'cliente' => $idCliente,
            'fecha' => $fechaActividad,
            'cantidad' => $cantidadSolicitada,
        ]);
    }

    /** Permite que las inscripciones existentes sigan funcionando antes de importar la actualización 12. */
    private function listaEsperaDisponible(): bool
    {
        try {
            $consulta = $this->conexion->query("SHOW TABLES LIKE 'listaEsperaActividad'");
            return (bool) $consulta->fetchColumn();
        } catch (PDOException) {
            return false;
        }
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
        $fechasCerradas = $this->fechasCerradas($actividad->id);

        return array_values(array_filter(
            $actividad->fechasDisponibles(100),
            static function (array $fecha) use ($rangos, $fechasCerradas): bool {
                $valor = (string) ($fecha['valor'] ?? '');
                if (isset($fechasCerradas[$valor])) return false;
                foreach ($rangos as $rango) {
                    if ($valor >= $rango['ingreso'] && $valor < $rango['egreso']) return true;
                }
                return false;
            }
        ));
    }

    /**
     * Una jornada ya realizada o suspendida no admite altas ni cambios de
     * participantes. Si todavía no se importó BD/14, se conserva el
     * comportamiento anterior para no bloquear el resto de las inscripciones.
     */
    private function validarJornadaAbierta(int $idActividad, string $fechaActividad): void
    {
        if (!$this->cierresDisponibles()) return;
        $consulta = $this->conexion->prepare(
            'SELECT estado FROM cierreActividad
              WHERE idActividad=:actividad AND fechaActividad=:fecha LIMIT 1 FOR UPDATE'
        );
        $consulta->execute(['actividad' => $idActividad, 'fecha' => $fechaActividad]);
        $estado = strtoupper((string) ($consulta->fetchColumn() ?: ''));
        if ($estado === 'SUSPENDIDA') {
            throw new RuntimeException('Esta jornada fue suspendida por coordinación y ya no admite inscripciones.');
        }
        if ($estado === 'REALIZADA') {
            throw new RuntimeException('Esta jornada ya fue realizada y quedó cerrada.');
        }
    }

    /** @return array<string, true> Fechas ya cerradas para excluir del selector web. */
    private function fechasCerradas(int $idActividad): array
    {
        if (!$this->cierresDisponibles()) return [];
        $consulta = $this->conexion->prepare(
            'SELECT fechaActividad FROM cierreActividad WHERE idActividad=:actividad'
        );
        $consulta->execute(['actividad' => $idActividad]);
        $resultado = [];
        foreach ($consulta->fetchAll(PDO::FETCH_COLUMN) as $fecha) {
            $resultado[(string) $fecha] = true;
        }
        return $resultado;
    }

    /** Comprueba si ya está instalada la tabla agregada por BD/14. */
    private function cierresDisponibles(): bool
    {
        try {
            $consulta = $this->conexion->query("SHOW TABLES LIKE 'cierreActividad'");
            return (bool) $consulta->fetchColumn();
        } catch (PDOException) {
            return false;
        }
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
                AND r.fechaEgreso >= :hoy
              ORDER BY r.fechaIngreso"
        );
        $consulta->execute(['cliente' => $idCliente, 'hoy' => RelojDemostracion::fecha($this->conexion)]);
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
             VALUES (:idCliente, :idAcompanante, :idActividad, :fechaActividad, :fechaInscripcion, 0, 1, NULL)
             ON DUPLICATE KEY UPDATE activo = 1, canceladaEn = NULL, fechaInscripcion = VALUES(fechaInscripcion)'
        );
        foreach ($idsAcompanantes as $idAcompanante) {
            $alta->execute([
                'idCliente' => $idCliente,
                'idAcompanante' => $idAcompanante,
                'idActividad' => $idActividad,
                'fechaActividad' => $fechaActividad,
                'fechaInscripcion' => RelojDemostracion::fecha($this->conexion),
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
