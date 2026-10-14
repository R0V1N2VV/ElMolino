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

    public function catalogo(): array
    {
        return $this->conexion->query(
            'SELECT idAlojamiento, numero, tipo, capacidad, precioPorNoche, estado FROM alojamientos ORDER BY tipo, numero'
        )->fetchAll();
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

    public function elementos(): array
    {
        $elementos = [];
        foreach ($this->carrito() as $clave => $item) {
            try {
                [$entrada, $salida, $noches] = $this->validarFechas(
                    (string) ($item['fechaIngreso'] ?? ''),
                    (string) ($item['fechaEgreso'] ?? '')
                );
                $alojamiento = $this->buscarAlojamiento((int) ($item['idAlojamiento'] ?? 0));
                if (!$alojamiento) {
                    $elementos[] = [
                        'clave' => (string) $clave,
                        'tipo' => 'Alojamiento no disponible',
                        'error' => 'El alojamiento ya no existe en el catálogo. Quitalo del carrito para continuar.',
                    ];
                    continue;
                }
                $personas = (int) ($item['cantidadPersonas'] ?? 0);
                $error = null;
                try {
                    $this->validarAlojamiento($alojamiento, $personas);
                    $this->validarDisponibilidad((int) $alojamiento['idAlojamiento'], $entrada, $salida);
                } catch (RuntimeException $e) {
                    $error = $e->getMessage();
                }
                $precioCentavos = (int) round((float) $alojamiento['precioPorNoche'] * 100);
                $elementos[] = [
                    'clave' => (string) $clave,
                    'idAlojamiento' => (int) $alojamiento['idAlojamiento'],
                    'numero' => (string) $alojamiento['numero'],
                    'tipo' => (string) $alojamiento['tipo'],
                    'capacidad' => (int) $alojamiento['capacidad'],
                    'personas' => $personas,
                    'fechaIngreso' => $entrada,
                    'fechaEgreso' => $salida,
                    'noches' => $noches,
                    'precioPorNoche' => number_format($precioCentavos / 100, 2, '.', ''),
                    'subtotal' => number_format(($precioCentavos * $noches) / 100, 2, '.', ''),
                    'error' => $error,
                ];
            } catch (RuntimeException $e) {
                $elementos[] = [
                    'clave' => (string) $clave,
                    'tipo' => 'Alojamiento no disponible',
                    'error' => $e->getMessage(),
                ];
            }
        }
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

        uasort($carrito, static fn(array $a, array $b): int => ((int) $a['idAlojamiento'] <=> (int) $b['idAlojamiento']) ?: strcmp((string) $a['fechaIngreso'], (string) $b['fechaIngreso']));
        $this->conexion->beginTransaction();
        try {
            $lineas = [];
            $totalCentavos = 0;
            foreach ($carrito as $item) {
                [$entrada, $salida, $noches] = $this->validarFechas(
                    (string) ($item['fechaIngreso'] ?? ''),
                    (string) ($item['fechaEgreso'] ?? '')
                );
                $idAlojamiento = (int) ($item['idAlojamiento'] ?? 0);
                $consulta = $this->conexion->prepare(
                    'SELECT idAlojamiento, numero, tipo, capacidad, precioPorNoche, estado FROM alojamientos WHERE idAlojamiento = :id FOR UPDATE'
                );
                $consulta->execute(['id' => $idAlojamiento]);
                $alojamiento = $consulta->fetch();
                $personas = (int) ($item['cantidadPersonas'] ?? 0);
                $this->validarAlojamiento($alojamiento ?: null, $personas);
                $this->validarDisponibilidad($idAlojamiento, $entrada, $salida);

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
        if (strtolower(trim((string) $alojamiento['estado'])) !== 'disponible') {
            throw new RuntimeException('El alojamiento ' . $alojamiento['numero'] . ' no está habilitado para reservas.');
        }
        if ($personas < 1 || $personas > (int) $alojamiento['capacidad']) {
            throw new RuntimeException('La cantidad de personas supera la capacidad del alojamiento ' . $alojamiento['numero'] . '.');
        }
        if ((float) $alojamiento['precioPorNoche'] <= 0) {
            throw new RuntimeException('El precio de este alojamiento todavía no está configurado.');
        }
    }

    private function validarDisponibilidad(int $idAlojamiento, string $entrada, string $salida): void
    {
        $consulta = $this->conexion->prepare(
            "SELECT COUNT(*) FROM reservas WHERE idAlojamiento = :id AND estadoReserva NOT IN ('CANCELADA', 'CANCELADO') AND fechaIngreso < :salida AND fechaEgreso > :entrada"
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
}
