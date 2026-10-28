<?php
declare(strict_types=1);

final class GestorCarritoAlojamiento
{
    private const CLAVE_CARRITO = 'carrito_alojamiento';
    private const ZONA_HORARIA = 'America/Argentina/Buenos_Aires';
    public const METODOS_PAGO = [
        'Efectivo al llegar',
        'Transferencia bancaria',
        'Tarjeta en el complejo',
    ];

    public function __construct(private PDO $conexion) {}

    public function catalogo(?string $entrada = null, ?string $salida = null): array
    {
        try {
            [$fechaEntrada, $fechaSalida] = $this->validarFechas((string) $entrada, (string) $salida);
        } catch (RuntimeException) {
            return $this->conexion->query(
                'SELECT idAlojamiento, numero, tipo, capacidad, precioPorNoche, estado, 0 AS reservadoEnFechas FROM alojamientos ORDER BY tipo, numero'
            )->fetchAll();
        }

        $consulta = $this->conexion->prepare(
            "SELECT a.idAlojamiento, a.numero, a.tipo, a.capacidad, a.precioPorNoche, a.estado,
                    EXISTS (
                        SELECT 1
                        FROM reservas r
                        WHERE r.idAlojamiento = a.idAlojamiento
                          AND UPPER(TRIM(r.estadoReserva)) NOT IN ('CANCELADA', 'CANCELADO')
                          AND r.fechaIngreso < :fecha_salida
                          AND r.fechaEgreso > :fecha_entrada
                    ) AS reservadoEnFechas
             FROM alojamientos a
             ORDER BY a.tipo, a.numero"
        );
        $consulta->execute([
            'fecha_salida' => $fechaSalida,
            'fecha_entrada' => $fechaEntrada,
        ]);
        return $consulta->fetchAll();
    }

    public function catalogoAgrupado(?string $entrada = null, ?string $salida = null): array
    {
        $grupos = [];
        foreach ($this->catalogo($entrada, $salida) as $alojamiento) {
            $tipoClave = self::claveTipo((string) ($alojamiento['tipo'] ?? ''));
            if (!in_array($tipoClave, ['cabanas', 'habitaciones', 'hoteles'], true)) continue;
            $estado = self::informacionEstado($alojamiento);
            if (!$estado['permiteSeleccionarFechas']) continue;

            $capacidad = (int) ($alojamiento['capacidad'] ?? 0);
            if ($capacidad <= 0) continue;
            $clave = $tipoClave . '-' . $capacidad;
            if (!isset($grupos[$clave])) {
                $grupos[$clave] = $alojamiento + [
                    'tipoClave' => $tipoClave,
                    'unidades' => 0,
                    'unidadesDisponibles' => 0,
                ];
            }
            $grupos[$clave]['unidades']++;
            if ($estado['disponible']) $grupos[$clave]['unidadesDisponibles']++;
            $grupos[$clave]['precioPorNoche'] = min(
                (float) $grupos[$clave]['precioPorNoche'],
                (float) $alojamiento['precioPorNoche']
            );
        }

        $ordenTipos = ['cabanas' => 1, 'habitaciones' => 2, 'hoteles' => 3];
        uasort($grupos, static function (array $a, array $b) use ($ordenTipos): int {
            return (($ordenTipos[$a['tipoClave']] ?? 99) <=> ($ordenTipos[$b['tipoClave']] ?? 99))
                ?: ((int) $a['capacidad'] <=> (int) $b['capacidad']);
        });
        return array_values($grupos);
    }

    public static function informacionEstado(array $alojamiento): array
    {
        $estado = self::normalizarTexto((string) ($alojamiento['estado'] ?? ''));
        $reservadoEnFechas = (int) ($alojamiento['reservadoEnFechas'] ?? 0) === 1;

        if (in_array($estado, ['fuera de servicio', 'fuera servicio', 'mantenimiento'], true)) {
            return [
                'clave' => 'fuera-servicio',
                'etiqueta' => 'Fuera de servicio',
                'detalle' => 'Este alojamiento no está habilitado temporalmente.',
                'disponible' => false,
                'permiteSeleccionarFechas' => false,
            ];
        }
        if (in_array($estado, ['limpieza', 'en limpieza'], true)) {
            return [
                'clave' => 'limpieza',
                'etiqueta' => 'En limpieza',
                'detalle' => 'Volverá a estar disponible cuando finalice la limpieza.',
                'disponible' => false,
                'permiteSeleccionarFechas' => false,
            ];
        }
        if (in_array($estado, ['reservado', 'reservada', 'ocupado', 'ocupada'], true)) {
            return [
                'clave' => 'reservado',
                'etiqueta' => 'Reservada',
                'detalle' => 'Este alojamiento ya se encuentra reservado.',
                'disponible' => false,
                'permiteSeleccionarFechas' => false,
            ];
        }
        if (!in_array($estado, ['disponible', 'libre', 'habilitado', 'habilitada'], true)) {
            return [
                'clave' => 'no-disponible',
                'etiqueta' => 'No disponible',
                'detalle' => 'Consultá con el complejo para conocer su disponibilidad.',
                'disponible' => false,
                'permiteSeleccionarFechas' => false,
            ];
        }
        if ($reservadoEnFechas) {
            return [
                'clave' => 'reservado',
                'etiqueta' => 'Reservada para esas fechas',
                'detalle' => 'Podés probar con otras fechas.',
                'disponible' => false,
                'permiteSeleccionarFechas' => true,
            ];
        }

        return [
            'clave' => 'disponible',
            'etiqueta' => 'Disponible',
            'detalle' => '',
            'disponible' => true,
            'permiteSeleccionarFechas' => true,
        ];
    }

    public static function claveTipo(string $tipo): string
    {
        $tipo = self::normalizarTexto($tipo);
        if (str_contains($tipo, 'caban')) return 'cabanas';
        if (str_contains($tipo, 'habitacion')) return 'habitaciones';
        if (str_contains($tipo, 'hotel')) return 'hoteles';
        return 'otros';
    }

    public static function detallesPublicos(array $alojamiento): array
    {
        $capacidad = max(1, (int) ($alojamiento['capacidad'] ?? 1));
        $tipo = self::claveTipo((string) ($alojamiento['tipo'] ?? ''));

        if ($tipo === 'cabanas') {
            return [
                'titulo' => 'Cabaña para ' . $capacidad . ' personas',
                'descripcion' => 'Un espacio independiente y cómodo para organizar la estadía con mayor libertad.',
                'servicios' => ['Wi-Fi', 'Baño privado', 'Cocina equipada', 'Parrilla', 'Ropa de cama', 'Estacionamiento'],
            ];
        }
        if ($tipo === 'habitaciones') {
            $nombre = $capacidad <= 2 ? 'Habitación doble' : ($capacidad <= 4 ? 'Habitación familiar' : 'Habitación grupal');
            return [
                'titulo' => $nombre . ' · ' . $capacidad . ' personas',
                'descripcion' => 'Una opción práctica para descansar dentro del complejo y tener todo cerca.',
                'servicios' => ['Wi-Fi', 'Baño privado', 'Climatización', 'Ropa de cama', 'Guardado personal'],
            ];
        }
        if ($tipo === 'hoteles') {
            return [
                'titulo' => 'Hotel · Habitación para ' . $capacidad . ' personas',
                'descripcion' => 'Una estadía con servicios diarios y acceso cómodo a los espacios comunes.',
                'servicios' => ['Wi-Fi', 'Baño privado', 'Desayuno', 'Limpieza diaria', 'Climatización', 'Espacios comunes'],
            ];
        }

        return [
            'titulo' => (string) ($alojamiento['tipo'] ?? 'Alojamiento') . ' para ' . $capacidad . ' personas',
            'descripcion' => 'Una alternativa para disfrutar la estadía dentro de El Molino.',
            'servicios' => ['Wi-Fi', 'Baño privado', 'Ropa de cama'],
        ];
    }

    public function agregar(int $idAlojamiento, string $entrada, string $salida, int $personas): void
    {
        [$fechaEntrada, $fechaSalida, $noches] = $this->validarFechas($entrada, $salida);
        $alojamiento = $this->buscarAlojamiento($idAlojamiento);
        $this->validarAlojamiento($alojamiento, $personas);
        $this->validarDisponibilidad($idAlojamiento, $fechaEntrada, $fechaSalida);

        $carrito = $this->carrito();
        foreach ($carrito as $item) {
            if ((int) ($item['idAlojamiento'] ?? 0) === $idAlojamiento
                && (string) ($item['fechaIngreso'] ?? '') < $fechaSalida
                && (string) ($item['fechaEgreso'] ?? '') > $fechaEntrada) {
                throw new RuntimeException('Ese alojamiento ya está en el carrito para fechas que se superponen.');
            }
        }

        $clave = bin2hex(random_bytes(8));
        $carrito[$clave] = [
            'idAlojamiento' => $idAlojamiento,
            'fechaIngreso' => $fechaEntrada,
            'fechaEgreso' => $fechaSalida,
            'cantidadPersonas' => $personas,
        ];
        Sesion::guardar(self::CLAVE_CARRITO, $carrito);
    }

    public function agregarPorTipoCapacidad(string $tipoClave, int $capacidad, string $entrada, string $salida, int $personas): void
    {
        [$fechaEntrada, $fechaSalida] = $this->validarFechas($entrada, $salida);
        if (!in_array($tipoClave, ['cabanas', 'habitaciones', 'hoteles'], true) || $capacidad <= 0) {
            throw new RuntimeException('La opción de alojamiento no es válida. Volvé a elegirla.');
        }
        if ($personas < 1 || $personas > $capacidad) {
            throw new RuntimeException('La cantidad de personas supera la capacidad de esta opción.');
        }

        $carrito = $this->carrito();
        $ocupadosTemporales = $this->ocupacionesTemporales($carrito);
        $alojamiento = $this->buscarDisponiblePorGrupo(
            $tipoClave,
            $capacidad,
            $fechaEntrada,
            $fechaSalida,
            $personas,
            $ocupadosTemporales
        );
        if (!$alojamiento) throw new RuntimeException($this->mensajeSinDisponibilidad($tipoClave, $capacidad));

        $clave = bin2hex(random_bytes(8));
        $carrito[$clave] = [
            'idAlojamiento' => (int) $alojamiento['idAlojamiento'],
            'tipoGrupo' => $tipoClave,
            'capacidadGrupo' => $capacidad,
            'fechaIngreso' => $fechaEntrada,
            'fechaEgreso' => $fechaSalida,
            'cantidadPersonas' => $personas,
        ];
        Sesion::guardar(self::CLAVE_CARRITO, $carrito);
    }

    public function elementos(): array
    {
        $elementos = [];
        $carrito = $this->carrito();
        $ocupadosTemporales = [];
        $carritoActualizado = false;
        foreach ($carrito as $clave => $item) {
            try {
                [$entrada, $salida, $noches] = $this->validarFechas(
                    (string) ($item['fechaIngreso'] ?? ''),
                    (string) ($item['fechaEgreso'] ?? '')
                );
                $alojamientoAnterior = $this->buscarAlojamiento((int) ($item['idAlojamiento'] ?? 0));
                $tipoClave = (string) ($item['tipoGrupo'] ?? ($alojamientoAnterior ? self::claveTipo((string) $alojamientoAnterior['tipo']) : ''));
                $capacidad = (int) ($item['capacidadGrupo'] ?? ($alojamientoAnterior['capacidad'] ?? 0));
                $personas = (int) ($item['cantidadPersonas'] ?? 0);
                $alojamiento = $this->buscarDisponiblePorGrupo(
                    $tipoClave,
                    $capacidad,
                    $entrada,
                    $salida,
                    $personas,
                    $ocupadosTemporales
                );
                if (!$alojamiento) {
                    $elementos[] = [
                        'clave' => (string) $clave,
                        'tipo' => 'Alojamiento sin disponibilidad',
                        'error' => $this->mensajeSinDisponibilidad($tipoClave, $capacidad),
                    ];
                    continue;
                }

                $ocupadosTemporales[] = [
                    'idAlojamiento' => (int) $alojamiento['idAlojamiento'],
                    'fechaIngreso' => $entrada,
                    'fechaEgreso' => $salida,
                ];
                if ((int) ($item['idAlojamiento'] ?? 0) !== (int) $alojamiento['idAlojamiento']
                    || !isset($item['tipoGrupo'], $item['capacidadGrupo'])) {
                    $carrito[$clave]['idAlojamiento'] = (int) $alojamiento['idAlojamiento'];
                    $carrito[$clave]['tipoGrupo'] = $tipoClave;
                    $carrito[$clave]['capacidadGrupo'] = $capacidad;
                    $carritoActualizado = true;
                }

                $precioCentavos = (int) round((float) $alojamiento['precioPorNoche'] * 100);
                $detalle = self::detallesPublicos($alojamiento);
                $elementos[] = [
                    'clave' => (string) $clave,
                    'idAlojamiento' => (int) $alojamiento['idAlojamiento'],
                    'tipo' => (string) $detalle['titulo'],
                    'capacidad' => (int) $alojamiento['capacidad'],
                    'personas' => $personas,
                    'fechaIngreso' => $entrada,
                    'fechaEgreso' => $salida,
                    'noches' => $noches,
                    'precioPorNoche' => number_format($precioCentavos / 100, 2, '.', ''),
                    'subtotal' => number_format(($precioCentavos * $noches) / 100, 2, '.', ''),
                    'error' => null,
                ];
            } catch (RuntimeException $e) {
                $elementos[] = [
                    'clave' => (string) $clave,
                    'tipo' => 'Alojamiento no disponible',
                    'error' => $e->getMessage(),
                ];
            }
        }
        if ($carritoActualizado) Sesion::guardar(self::CLAVE_CARRITO, $carrito);
        return $elementos;
    }

    public function quitar(string $clave): void
    {
        $carrito = $this->carrito();
        unset($carrito[$clave]);
        Sesion::guardar(self::CLAVE_CARRITO, $carrito);
    }

    public function cantidad(): int
    {
        return count($this->carrito());
    }

    public static function totalEstimado(array $elementos): int
    {
        $total = 0;
        foreach ($elementos as $elemento) {
            if (empty($elemento['error'])) $total += (int) round((float) $elemento['subtotal'] * 100);
        }
        return $total;
    }

    public function crearPedido(array $usuario, string $metodoPago): int
    {
        if (!in_array($metodoPago, self::METODOS_PAGO, true)) {
            throw new RuntimeException('Elegí un método de pago de la lista.');
        }
        $carrito = $this->carrito();
        if (!$carrito) throw new RuntimeException('El carrito está vacío.');

        $idCliente = (int) ($usuario['id_cliente'] ?? 0);
        if ($idCliente <= 0) {
            $consultaCliente = $this->conexion->prepare('SELECT idCliente FROM cliente WHERE email = :email LIMIT 1');
            $consultaCliente->execute(['email' => (string) ($usuario['email'] ?? '')]);
            $idCliente = (int) ($consultaCliente->fetchColumn() ?: 0);
        }
        if ($idCliente <= 0) throw new RuntimeException('La cuenta no está vinculada a un perfil de cliente. Contactá al complejo para continuar.');

        uasort($carrito, static function (array $a, array $b): int {
            return strcmp((string) ($a['tipoGrupo'] ?? ''), (string) ($b['tipoGrupo'] ?? ''))
                ?: ((int) ($a['capacidadGrupo'] ?? 0) <=> (int) ($b['capacidadGrupo'] ?? 0))
                ?: strcmp((string) ($a['fechaIngreso'] ?? ''), (string) ($b['fechaIngreso'] ?? ''));
        });
        $this->conexion->beginTransaction();
        try {
            $lineas = [];
            $totalCentavos = 0;
            $ocupadosTemporales = [];
            foreach ($carrito as $item) {
                [$entrada, $salida, $noches] = $this->validarFechas(
                    (string) ($item['fechaIngreso'] ?? ''),
                    (string) ($item['fechaEgreso'] ?? '')
                );
                $personas = (int) ($item['cantidadPersonas'] ?? 0);
                $alojamientoAnterior = $this->buscarAlojamiento((int) ($item['idAlojamiento'] ?? 0));
                $tipoClave = (string) ($item['tipoGrupo'] ?? ($alojamientoAnterior ? self::claveTipo((string) $alojamientoAnterior['tipo']) : ''));
                $capacidad = (int) ($item['capacidadGrupo'] ?? ($alojamientoAnterior['capacidad'] ?? 0));
                $alojamiento = $this->buscarDisponiblePorGrupo(
                    $tipoClave,
                    $capacidad,
                    $entrada,
                    $salida,
                    $personas,
                    $ocupadosTemporales,
                    true
                );
                if (!$alojamiento) throw new RuntimeException($this->mensajeSinDisponibilidad($tipoClave, $capacidad));
                $ocupadosTemporales[] = [
                    'idAlojamiento' => (int) $alojamiento['idAlojamiento'],
                    'fechaIngreso' => $entrada,
                    'fechaEgreso' => $salida,
                ];

                $precioCentavos = (int) round((float) $alojamiento['precioPorNoche'] * 100);
                $subtotalCentavos = $precioCentavos * $noches;
                $totalCentavos += $subtotalCentavos;
                $lineas[] = [
                    'alojamiento' => $alojamiento,
                    'entrada' => $entrada,
                    'salida' => $salida,
                    'noches' => $noches,
                    'personas' => $personas,
                    'precioCentavos' => $precioCentavos,
                    'subtotalCentavos' => $subtotalCentavos,
                ];
            }

            $crearPedido = $this->conexion->prepare(
                'INSERT INTO pedido_alojamiento (idCliente, total, metodoPago, estadoPedido, estadoPago) VALUES (:cliente, :total, :metodo, \'PENDIENTE\', \'PENDIENTE\')'
            );
            $crearPedido->execute([
                'cliente' => $idCliente,
                'total' => number_format($totalCentavos / 100, 2, '.', ''),
                'metodo' => $metodoPago,
            ]);
            $idPedido = (int) $this->conexion->lastInsertId();

            $crearReserva = $this->conexion->prepare(
                'INSERT INTO reservas (idCliente, idAlojamiento, fechaIngreso, fechaEgreso, cantidadPersonas, temporada, estadoReserva, precioTotal) VALUES (:cliente, :alojamiento, :entrada, :salida, :personas, NULL, \'PENDIENTE\', :total)'
            );
            $crearDetalle = $this->conexion->prepare(
                'INSERT INTO detalle_pedido_alojamiento (idPedido, idReserva, alojamientoNumero, alojamientoTipo, fechaIngreso, fechaEgreso, cantidadPersonas, noches, precioPorNoche, subtotal) VALUES (:pedido, :reserva, :numero, :tipo, :entrada, :salida, :personas, :noches, :precio, :subtotal)'
            );

            foreach ($lineas as $linea) {
                $crearReserva->execute([
                    'cliente' => $idCliente,
                    'alojamiento' => $linea['alojamiento']['idAlojamiento'],
                    'entrada' => $linea['entrada'],
                    'salida' => $linea['salida'],
                    'personas' => $linea['personas'],
                    'total' => number_format($linea['subtotalCentavos'] / 100, 2, '.', ''),
                ]);
                $idReserva = (int) $this->conexion->lastInsertId();
                $crearDetalle->execute([
                    'pedido' => $idPedido,
                    'reserva' => $idReserva,
                    'numero' => $linea['alojamiento']['numero'],
                    'tipo' => $linea['alojamiento']['tipo'],
                    'entrada' => $linea['entrada'],
                    'salida' => $linea['salida'],
                    'personas' => $linea['personas'],
                    'noches' => $linea['noches'],
                    'precio' => number_format($linea['precioCentavos'] / 100, 2, '.', ''),
                    'subtotal' => number_format($linea['subtotalCentavos'] / 100, 2, '.', ''),
                ]);
            }
            $this->conexion->commit();
            $this->vaciar();
            return $idPedido;
        } catch (Throwable $error) {
            if ($this->conexion->inTransaction()) $this->conexion->rollBack();
            throw $error;
        }
    }

    public function obtenerPedido(int $idPedido, int $idCliente): ?array
    {
        $consulta = $this->conexion->prepare(
            'SELECT p.idPedido, p.total, p.metodoPago, p.estadoPedido, p.estadoPago, p.creadoEn, c.nombre, c.apellido, c.email FROM pedido_alojamiento p JOIN cliente c ON c.idCliente = p.idCliente WHERE p.idPedido = :pedido AND p.idCliente = :cliente LIMIT 1'
        );
        $consulta->execute(['pedido' => $idPedido, 'cliente' => $idCliente]);
        $pedido = $consulta->fetch();
        if (!$pedido) return null;

        $consultaLineas = $this->conexion->prepare(
            'SELECT alojamientoNumero, alojamientoTipo, fechaIngreso, fechaEgreso, cantidadPersonas, noches, precioPorNoche, subtotal FROM detalle_pedido_alojamiento WHERE idPedido = :pedido ORDER BY idDetalle'
        );
        $consultaLineas->execute(['pedido' => $idPedido]);
        $pedido['lineas'] = $consultaLineas->fetchAll();
        return $pedido;
    }

    public function vaciar(): void
    {
        Sesion::eliminar(self::CLAVE_CARRITO);
    }

    private function carrito(): array
    {
        $carrito = Sesion::obtener(self::CLAVE_CARRITO, []);
        return is_array($carrito) ? $carrito : [];
    }

    private function buscarAlojamiento(int $id): ?array
    {
        $consulta = $this->conexion->prepare(
            'SELECT idAlojamiento, numero, tipo, capacidad, precioPorNoche, estado FROM alojamientos WHERE idAlojamiento = :id LIMIT 1'
        );
        $consulta->execute(['id' => $id]);
        $alojamiento = $consulta->fetch();
        return $alojamiento ?: null;
    }

    private function validarAlojamiento(?array $alojamiento, int $personas): void
    {
        if (!$alojamiento) throw new RuntimeException('El alojamiento elegido ya no está disponible.');
        $informacionEstado = self::informacionEstado($alojamiento);
        if (!$informacionEstado['permiteSeleccionarFechas']) {
            throw new RuntimeException('El alojamiento ' . $alojamiento['numero'] . ' no está disponible para reservas en este momento.');
        }
        if ($personas < 1 || $personas > (int) $alojamiento['capacidad']) {
            throw new RuntimeException('La cantidad de personas supera la capacidad del alojamiento ' . $alojamiento['numero'] . '.');
        }
        if ((float) $alojamiento['precioPorNoche'] <= 0) {
            throw new RuntimeException('El precio de este alojamiento todavía no está configurado.');
        }
    }

    private function buscarDisponiblePorGrupo(
        string $tipoClave,
        int $capacidad,
        string $entrada,
        string $salida,
        int $personas,
        array $ocupadosTemporales = [],
        bool $bloquear = false
    ): ?array {
        if (!in_array($tipoClave, ['cabanas', 'habitaciones', 'hoteles'], true) || $capacidad <= 0) return null;

        $sql = 'SELECT idAlojamiento, numero, tipo, capacidad, precioPorNoche, estado FROM alojamientos WHERE capacidad = :capacidad ORDER BY precioPorNoche, idAlojamiento';
        if ($bloquear) $sql .= ' FOR UPDATE';
        $consulta = $this->conexion->prepare($sql);
        $consulta->execute(['capacidad' => $capacidad]);

        foreach ($consulta->fetchAll() as $alojamiento) {
            if (self::claveTipo((string) $alojamiento['tipo']) !== $tipoClave) continue;
            if ($this->estaOcupadoTemporalmente((int) $alojamiento['idAlojamiento'], $entrada, $salida, $ocupadosTemporales)) continue;
            try {
                $this->validarAlojamiento($alojamiento, $personas);
                $this->validarDisponibilidad((int) $alojamiento['idAlojamiento'], $entrada, $salida);
                return $alojamiento;
            } catch (RuntimeException) {
                continue;
            }
        }
        return null;
    }

    private function ocupacionesTemporales(array $carrito): array
    {
        $ocupaciones = [];
        foreach ($carrito as $item) {
            $id = (int) ($item['idAlojamiento'] ?? 0);
            $entrada = (string) ($item['fechaIngreso'] ?? '');
            $salida = (string) ($item['fechaEgreso'] ?? '');
            if ($id > 0 && $entrada !== '' && $salida !== '') {
                $ocupaciones[] = ['idAlojamiento' => $id, 'fechaIngreso' => $entrada, 'fechaEgreso' => $salida];
            }
        }
        return $ocupaciones;
    }

    private function estaOcupadoTemporalmente(int $idAlojamiento, string $entrada, string $salida, array $ocupaciones): bool
    {
        foreach ($ocupaciones as $ocupacion) {
            if ((int) ($ocupacion['idAlojamiento'] ?? 0) !== $idAlojamiento) continue;
            if ((string) ($ocupacion['fechaIngreso'] ?? '') < $salida
                && (string) ($ocupacion['fechaEgreso'] ?? '') > $entrada) return true;
        }
        return false;
    }

    private function mensajeSinDisponibilidad(string $tipoClave, int $capacidad): string
    {
        $nombres = [
            'cabanas' => 'cabañas',
            'habitaciones' => 'habitaciones',
            'hoteles' => 'habitaciones de hotel',
        ];
        $nombre = $nombres[$tipoClave] ?? 'alojamientos';
        return 'No hay ' . $nombre . ' para ' . $capacidad . ' personas disponibles en esas fechas. Elegí otras fechas u otro tipo de alojamiento.';
    }

    private function validarDisponibilidad(int $idAlojamiento, string $entrada, string $salida): void
    {
        $consulta = $this->conexion->prepare(
            "SELECT COUNT(*) FROM reservas WHERE idAlojamiento = :id AND UPPER(TRIM(estadoReserva)) NOT IN ('CANCELADA', 'CANCELADO') AND fechaIngreso < :salida AND fechaEgreso > :entrada"
        );
        $consulta->execute(['id' => $idAlojamiento, 'salida' => $salida, 'entrada' => $entrada]);
        if ((int) $consulta->fetchColumn() > 0) {
            throw new RuntimeException('El alojamiento ya tiene una reserva para parte de esas fechas. Elegí otras fechas.');
        }
    }

    private function validarFechas(string $entrada, string $salida): array
    {
        $zona = new DateTimeZone(self::ZONA_HORARIA);
        $fechaEntrada = DateTimeImmutable::createFromFormat('!Y-m-d', $entrada, $zona);
        $erroresEntrada = DateTimeImmutable::getLastErrors();
        $fechaSalida = DateTimeImmutable::createFromFormat('!Y-m-d', $salida, $zona);
        $erroresSalida = DateTimeImmutable::getLastErrors();
        $entradaValida = $fechaEntrada && $fechaEntrada->format('Y-m-d') === $entrada
            && ($erroresEntrada === false || ($erroresEntrada['warning_count'] === 0 && $erroresEntrada['error_count'] === 0));
        $salidaValida = $fechaSalida && $fechaSalida->format('Y-m-d') === $salida
            && ($erroresSalida === false || ($erroresSalida['warning_count'] === 0 && $erroresSalida['error_count'] === 0));
        if (!$entradaValida || !$salidaValida || !$fechaEntrada || !$fechaSalida) {
            throw new RuntimeException('Ingresá fechas válidas para la estadía.');
        }
        $hoy = new DateTimeImmutable('today', $zona);
        if ($fechaEntrada < $hoy || $fechaSalida <= $fechaEntrada) {
            throw new RuntimeException('La salida debe ser posterior al ingreso y las fechas no pueden ser pasadas.');
        }
        $noches = (int) $fechaEntrada->diff($fechaSalida)->days;
        return [$fechaEntrada->format('Y-m-d'), $fechaSalida->format('Y-m-d'), $noches];
    }

    private static function normalizarTexto(string $texto): string
    {
        $texto = strtolower(trim($texto));
        $texto = strtr($texto, [
            'á' => 'a', 'é' => 'e', 'í' => 'i', 'ó' => 'o', 'ú' => 'u',
            '_' => ' ', '-' => ' ',
        ]);
        return preg_replace('/\s+/', ' ', $texto) ?: '';
    }
}
