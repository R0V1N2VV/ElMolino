<?php
declare(strict_types=1);

final class Autorizacion
{
    private const ROL_COORDINADOR = 'coordinador_actividades';

    private function __construct() {}

    public static function esCoordinador(?array $usuario): bool
    {
        return is_array($usuario) && ($usuario['rol'] ?? '') === self::ROL_COORDINADOR;
    }

    public static function exigirCoordinador(?array $usuario): void
    {
        if (!$usuario) {
            Sesion::guardarMensaje('error', 'Iniciá sesión para gestionar actividades.');
            Utilidades::redirigir('registro/login.php');
        }
        if (!self::esCoordinador($usuario)) {
            http_response_code(403);
            exit('No tenés permisos para gestionar actividades.');
        }
    }

    public static function destinoInicial(array $usuario): string
    {
        return self::esCoordinador($usuario) ? '../administrar-actividades.php' : 'cuenta.php';
    }
}
