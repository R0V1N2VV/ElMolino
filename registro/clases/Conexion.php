<?php
declare(strict_types=1);

final class Conexion
{
    private static ?PDO $instancia = null;

    private function __construct()
    {
    }

    public static function obtener(): PDO
    {
        if (self::$instancia instanceof PDO) {
            return self::$instancia;
        }

        $host = getenv('EL_MOLINO_DB_HOST') ?: '127.0.0.1';
        $puerto = getenv('EL_MOLINO_DB_PUERTO') ?: '3306';
        $base = getenv('EL_MOLINO_DB_NOMBRE') ?: 's27_El Molino';
        $usuario = getenv('EL_MOLINO_DB_USUARIO') ?: 'root';
        $contrasena = getenv('EL_MOLINO_DB_CONTRASENA') ?: '';

        self::$instancia = new PDO("mysql:host={$host};port={$puerto};dbname={$base};charset=utf8mb4", $usuario, $contrasena, [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES => false,
        ]);

        return self::$instancia;
    }
}
