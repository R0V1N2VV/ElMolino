<?php
/**
 * Ejecutar desde una tarea programada del hosting cada 5 minutos.
 * Ejemplo: php /ruta/al/proyecto/php/tarea_vencimientos.php
 */
declare(strict_types=1);
require_once __DIR__ . '/Conexion.php';
require_once __DIR__ . '/GestorEstadoFinanciero.php';

try {
    $cantidad = GestorEstadoFinanciero::vencerPedidosPendientes(Conexion::obtener());
    echo 'Pedidos vencidos: ' . $cantidad . PHP_EOL;
} catch (Throwable $error) {
    fwrite(STDERR, 'Error en vencimientos: ' . $error->getMessage() . PHP_EOL);
    exit(1);
}
