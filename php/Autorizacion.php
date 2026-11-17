<?php
/**
 * ARCHIVO EXPLICADO: php/Autorizacion.php
 *
 * Decide qué puede hacer un usuario según el rol guardado en su sesión.
 * Las funciones de este archivo están documentadas junto a su declaración.
 */

declare(strict_types=1);

final class Autorizacion
{
    private const ROL_COORDINADOR = 'coordinador_actividades';
    private const ROL_COORDINADOR_APP = 'COORDINADOR';

    /**
     * Inicializa el objeto y deja preparadas las dependencias que utilizarán sus demás métodos.
     */
    private function __construct() {}

    /**
     * Comprueba es coordinador y devuelve verdadero o falso según se cumpla la condición.
     *
     * @param mixed $usuario Valor de usuario que recibe la función.
     * @return bool Resultado producido por esta operación.
     */
    public static function esCoordinador(?array $usuario): bool
    {
        if (!is_array($usuario)) return false;
        $rol = (string) ($usuario['rol'] ?? '');
        return $rol === self::ROL_COORDINADOR || $rol === self::ROL_COORDINADOR_APP;
    }

    /**
     * Ejecuta la lógica de “exigir coordinador” en Autorizacion.php.
     *
     * @param mixed $usuario Valor de usuario que recibe la función.
     */
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

    /**
     * Ejecuta la lógica de “destino inicial” en Autorizacion.php.
     *
     * @param mixed $usuario Valor de usuario que recibe la función.
     * @return string Resultado producido por esta operación.
     */
    public static function destinoInicial(array $usuario): string
    {
        return self::esCoordinador($usuario) ? '../administrar-actividades.php' : 'cuenta.php';
    }
}
