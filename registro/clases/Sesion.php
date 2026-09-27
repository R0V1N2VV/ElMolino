<?php
declare(strict_types=1);

final class Sesion
{
    private function __construct()
    {
    }

    public static function iniciar(): void
    {
        if (session_status() === PHP_SESSION_NONE) {
            session_name('el_molino_sesion');
            session_start();
        }
    }

    public static function tokenCsrf(): string
    {
        self::iniciar();
        $_SESSION['token_csrf'] ??= bin2hex(random_bytes(32));
        return $_SESSION['token_csrf'];
    }

    public static function validarCsrf(?string $token): bool
    {
        self::iniciar();
        return is_string($token) && isset($_SESSION['token_csrf']) && hash_equals($_SESSION['token_csrf'], $token);
    }

    public static function guardar(string $clave, mixed $valor): void
    {
        self::iniciar();
        $_SESSION[$clave] = $valor;
    }

    public static function obtener(string $clave, mixed $predeterminado = null): mixed
    {
        self::iniciar();
        return $_SESSION[$clave] ?? $predeterminado;
    }

    public static function eliminar(string $clave): void
    {
        self::iniciar();
        unset($_SESSION[$clave]);
    }

    public static function guardarMensaje(string $tipo, string $texto): void
    {
        self::guardar('mensaje', ['tipo' => $tipo, 'texto' => $texto]);
    }

    public static function tomarMensaje(): ?array
    {
        $mensaje = self::obtener('mensaje');
        self::eliminar('mensaje');
        return is_array($mensaje) ? $mensaje : null;
    }

    public static function iniciarUsuario(array $usuario): void
    {
        self::iniciar();
        session_regenerate_id(true);
        $_SESSION['usuario'] = $usuario;
    }

    public static function usuario(): ?array
    {
        $usuario = self::obtener('usuario');
        return is_array($usuario) ? $usuario : null;
    }

    public static function cerrar(): void
    {
        self::iniciar();
        $_SESSION = [];
        session_destroy();
    }
}
