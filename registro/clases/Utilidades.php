<?php
declare(strict_types=1);

final class Utilidades
{
    private function __construct()
    {
    }

    public static function escapar(?string $texto): string
    {
        return htmlspecialchars((string) $texto, ENT_QUOTES, 'UTF-8');
    }

    public static function redirigir(string $destino): never
    {
        header("Location: {$destino}");
        exit;
    }

    public static function codigoVerificacion(): string
    {
        return (string) random_int(100000, 999999);
    }
}
