<?php
declare(strict_types=1);

/**
 * Limita intentos fallidos de inicio de sesión dentro de una misma sesión web.
 * No guarda contraseñas ni datos sensibles: solo un contador temporal asociado
 * a una versión hash del correo escrito.
 */
final class LimiteIntentosLogin
{
    private const MAXIMO_INTENTOS = 5;
    private const BLOQUEO_SEGUNDOS = 900;
    private const CLAVE = 'limite_intentos_login';

    private function __construct() {}

    public static function puedeIntentar(string $correo): bool
    {
        $estado = self::estado($correo);
        return (int) ($estado['bloqueado_hasta'] ?? 0) <= time();
    }

    public static function segundosRestantes(string $correo): int
    {
        return max(0, (int) (self::estado($correo)['bloqueado_hasta'] ?? 0) - time());
    }

    public static function registrarFallo(string $correo): void
    {
        $clave = self::claveCorreo($correo);
        $estados = Sesion::obtener(self::CLAVE, []);
        if (!is_array($estados)) $estados = [];
        $estado = is_array($estados[$clave] ?? null) ? $estados[$clave] : [];
        $intentos = (int) ($estado['intentos'] ?? 0) + 1;
        $estados[$clave] = [
            'intentos' => $intentos,
            'bloqueado_hasta' => $intentos >= self::MAXIMO_INTENTOS ? time() + self::BLOQUEO_SEGUNDOS : 0,
        ];
        Sesion::guardar(self::CLAVE, $estados);
    }

    public static function limpiar(string $correo): void
    {
        $clave = self::claveCorreo($correo);
        $estados = Sesion::obtener(self::CLAVE, []);
        if (!is_array($estados)) return;
        unset($estados[$clave]);
        Sesion::guardar(self::CLAVE, $estados);
    }

    private static function estado(string $correo): array
    {
        $estados = Sesion::obtener(self::CLAVE, []);
        if (!is_array($estados)) return [];
        $estado = $estados[self::claveCorreo($correo)] ?? [];
        return is_array($estado) ? $estado : [];
    }

    private static function claveCorreo(string $correo): string
    {
        return hash('sha256', strtolower(trim($correo)));
    }
}
