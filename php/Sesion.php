<?php
/**
 * ARCHIVO EXPLICADO: php/Sesion.php
 *
 * Centraliza el inicio de sesión, el usuario autenticado, mensajes breves y protección CSRF.
 * Las funciones de este archivo están documentadas junto a su declaración.
 */

declare(strict_types=1);

final class Sesion
{
    /**
     * Inicializa el objeto y deja preparadas las dependencias que utilizarán sus demás métodos.
     */
    private function __construct() {}
    /**
     * Prepara e inicia el proceso o recurso necesario para continuar.
     */
    public static function iniciar(): void { if (session_status() === PHP_SESSION_NONE) { session_name('el_molino_sesion'); session_start(); } }
    /**
     * Obtiene o crea el token CSRF usado para comprobar que un formulario salió de esta sesión.
     *
     * @param mixed $_SESSION Valor de  session que recibe la función.
     * @return string Resultado producido por esta operación.
     */
    public static function tokenCsrf(): string { self::iniciar(); $_SESSION['token_csrf'] ??= bin2hex(random_bytes(32)); return $_SESSION['token_csrf']; }
    /**
     * Compara el token recibido con el de la sesión para bloquear envíos de formularios falsificados.
     *
     * @param mixed $token Valor de token que recibe la función.
     * @param mixed $_SESSION Valor de  session que recibe la función.
     * @return bool Resultado producido por esta operación.
     */
    public static function validarCsrf(?string $token): bool { self::iniciar(); return is_string($token) && isset($_SESSION['token_csrf']) && hash_equals($_SESSION['token_csrf'], $token); }
    /**
     * Valida y guarda la información en el destino correspondiente.
     *
     * @param mixed $clave Valor de clave que recibe la función.
     * @param mixed $valor Valor de valor que recibe la función.
     * @param mixed $_SESSION Valor de  session que recibe la función.
     */
    public static function guardar(string $clave, mixed $valor): void { self::iniciar(); $_SESSION[$clave] = $valor; }
    /**
     * Devuelve el dato solicitado; si corresponde, lo recupera primero de su origen persistente.
     *
     * @param mixed $clave Valor de clave que recibe la función.
     * @param mixed $predeterminado Valor de predeterminado que recibe la función.
     * @param mixed $_SESSION Valor de  session que recibe la función.
     * @return mixed Resultado producido por esta operación.
     */
    public static function obtener(string $clave, mixed $predeterminado = null): mixed { self::iniciar(); return $_SESSION[$clave] ?? $predeterminado; }
    /**
     * Elimina el elemento indicado del sistema.
     *
     * @param mixed $clave Valor de clave que recibe la función.
     * @param mixed $_SESSION Valor de  session que recibe la función.
     */
    public static function eliminar(string $clave): void { self::iniciar(); unset($_SESSION[$clave]); }
    /**
     * Valida y guarda mensaje en el destino correspondiente.
     *
     * @param mixed $tipo Valor de tipo que recibe la función.
     * @param mixed $texto Valor de texto que recibe la función.
     */
    public static function guardarMensaje(string $tipo, string $texto): void { self::guardar('mensaje', ['tipo' => $tipo, 'texto' => $texto]); }
    /**
     * Ejecuta la lógica de “tomar mensaje” en Sesion.php.
     *
     * @param mixed $mensaje Valor de mensaje que recibe la función.
     * @return ?array Resultado producido por esta operación.
     */
    public static function tomarMensaje(): ?array { $mensaje = self::obtener('mensaje'); self::eliminar('mensaje'); return is_array($mensaje) ? $mensaje : null; }
    /**
     * Inicia usuario con los datos recibidos.
     *
     * @param mixed $usuario Valor de usuario que recibe la función.
     * @param mixed $_SESSION Valor de  session que recibe la función.
     */
    public static function iniciarUsuario(array $usuario): void { self::iniciar(); session_regenerate_id(true); $_SESSION['usuario'] = $usuario; }
    /**
     * Ejecuta la lógica de “usuario” en Sesion.php.
     *
     * @param mixed $usuario Valor de usuario que recibe la función.
     * @return ?array Resultado producido por esta operación.
     */
    public static function usuario(): ?array { $usuario = self::obtener('usuario'); return is_array($usuario) ? $usuario : null; }
    /**
     * Cierra el recurso o la sesión actual y libera su estado asociado.
     *
     * @param mixed $_SESSION Valor de  session que recibe la función.
     */
    public static function cerrar(): void { self::iniciar(); $_SESSION = []; session_destroy(); }
}
