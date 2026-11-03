<?php
/**
 * ARCHIVO EXPLICADO: php/Utilidades.php
 *
 * Forma parte del flujo “utilidades” del sitio El Molino.
 * Las funciones de este archivo están documentadas junto a su declaración.
 */

declare(strict_types=1);

final class Utilidades
{
    /**
     * Inicializa el objeto y deja preparadas las dependencias que utilizarán sus demás métodos.
     */
    private function __construct() {}
    /**
     * Convierte caracteres especiales antes de imprimir texto en HTML y así evita inyectar código.
     *
     * @param mixed $texto Valor de texto que recibe la función.
     * @return string Resultado producido por esta operación.
     */
    public static function escapar(?string $texto): string { return htmlspecialchars((string) $texto, ENT_QUOTES, 'UTF-8'); }
    /**
     * Envía al navegador a otra página y detiene la ejecución actual.
     *
     * @param mixed $destino Valor de destino que recibe la función.
     * @return never Resultado producido por esta operación.
     */
    public static function redirigir(string $destino): never { header("Location: {$destino}"); exit; }
    /**
     * Ejecuta la lógica de “codigo verificacion” en Utilidades.php.
     * @return string Resultado producido por esta operación.
     */
    public static function codigoVerificacion(): string { return (string) random_int(100000, 999999); }
}
