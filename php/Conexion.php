<?php
/**
 * ARCHIVO EXPLICADO: php/Conexion.php
 *
 * Centraliza la conexión PDO compartida con la base de datos MySQL.
 * Las funciones de este archivo están documentadas junto a su declaración.
 */

declare(strict_types=1);

final class Conexion
{
    private static ?PDO $instancia = null;

    /**
     * Inicializa el objeto y deja preparadas las dependencias que utilizarán sus demás métodos.
     */
    private function __construct() {}

    /**
     * Devuelve el dato solicitado; si corresponde, lo recupera primero de su origen persistente.
     * @return PDO Resultado producido por esta operación.
     */
    public static function obtener(): PDO
    {
        if (self::$instancia instanceof PDO) return self::$instancia;
        $host = getenv('EL_MOLINO_DB_HOST') ?: 'pma.torga.com.ar';
        $puerto = getenv('EL_MOLINO_DB_PUERTO') ?: '3306';
        $base = getenv('EL_MOLINO_DB_NOMBRE') ?: 's27_El Molino';
        $usuario = getenv('EL_MOLINO_DB_USUARIO') ?: 'u27_xm6WlzIBcR';
        $contrasena = getenv('EL_MOLINO_DB_CONTRASENA') ?: 'A7fexyyjPsWaEvahL^m0J+0B';
        self::$instancia = new PDO("mysql:host={$host};port={$puerto};dbname={$base};charset=utf8mb4", $usuario, $contrasena, [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES => false,
        ]);
        return self::$instancia;
    }
}
