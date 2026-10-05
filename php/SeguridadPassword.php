<?php
declare(strict_types=1);

final class SeguridadPassword
{
    private const PREFIJO = 'pbkdf2_sha256';
    private const ITERACIONES = 120000;
    private const BYTES_SALT = 16;
    private const BYTES_HASH = 32;

    private function __construct() {}

    public static function crearHash(string $contrasena): string
    {
        $salt = random_bytes(self::BYTES_SALT);
        $hash = hash_pbkdf2(
            'sha256',
            $contrasena,
            $salt,
            self::ITERACIONES,
            self::BYTES_HASH,
            true
        );

        return self::PREFIJO . '$' . self::ITERACIONES . '$'
            . base64_encode($salt) . '$' . base64_encode($hash);
    }

    public static function verificar(string $contrasena, string $guardada): bool
    {
        if (str_starts_with($guardada, self::PREFIJO . '$')) {
            return self::verificarPbkdf2($contrasena, $guardada);
        }

        if (password_get_info($guardada)['algoName'] !== 'unknown') {
            return password_verify($contrasena, $guardada);
        }

        return hash_equals($guardada, $contrasena);
    }

    public static function necesitaMigracion(string $guardada): bool
    {
        return !str_starts_with($guardada, self::PREFIJO . '$');
    }

    private static function verificarPbkdf2(string $contrasena, string $guardada): bool
    {
        $partes = explode('$', $guardada);
        if (count($partes) !== 4 || $partes[0] !== self::PREFIJO) return false;

        $iteraciones = filter_var($partes[1], FILTER_VALIDATE_INT);
        $salt = base64_decode($partes[2], true);
        $hashGuardado = base64_decode($partes[3], true);
        if ($iteraciones === false || $iteraciones < 1 || $iteraciones > 2000000
            || $salt === false || $hashGuardado === false) {
            return false;
        }

        $hashIngresado = hash_pbkdf2(
            'sha256',
            $contrasena,
            $salt,
            $iteraciones,
            strlen($hashGuardado),
            true
        );
        return hash_equals($hashGuardado, $hashIngresado);
    }
}
