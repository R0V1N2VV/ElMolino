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
    /** Evita registrar dos veces los mismos manejadores durante una solicitud. */
    private static bool $manejadorDeErroresInstalado = false;

    /**
     * Inicializa el objeto y deja preparadas las dependencias que utilizarán sus demás métodos.
     */
    private function __construct() {}

    /**
     * Oculta los detalles internos de una excepción y deja el detalle técnico
     * únicamente en el registro del servidor. Así un fallo de base de datos no
     * expone consultas SQL, rutas ni credenciales a quien usa la página.
     */
    public static function instalarManejadorErrores(): void
    {
        if (self::$manejadorDeErroresInstalado) return;
        self::$manejadorDeErroresInstalado = true;

        // En producción nunca se deben imprimir excepciones de PHP en pantalla.
        ini_set('display_errors', '0');
        ini_set('display_startup_errors', '0');
        ini_set('log_errors', '1');

        set_exception_handler(static function (Throwable $error): void {
            self::mostrarErrorSeguro($error);
        });

        register_shutdown_function(static function (): void {
            $ultimoError = error_get_last();
            if (!$ultimoError || !in_array($ultimoError['type'], [E_ERROR, E_PARSE, E_CORE_ERROR, E_COMPILE_ERROR], true)) {
                return;
            }
            self::mostrarErrorSeguro(new ErrorException(
                (string) $ultimoError['message'],
                0,
                (int) $ultimoError['type'],
                (string) $ultimoError['file'],
                (int) $ultimoError['line']
            ));
        });
    }

    /** Muestra una respuesta amable y registra el detalle técnico en el servidor. */
    private static function mostrarErrorSeguro(Throwable $error): never
    {
        error_log(sprintf(
            'El Molino: %s en %s:%d',
            $error->getMessage(),
            $error->getFile(),
            $error->getLine()
        ));
        if (!headers_sent()) {
            http_response_code(500);
            header('Content-Type: text/html; charset=UTF-8');
        }
        echo '<!doctype html><html lang="es-AR"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>No pudimos completar la acción | El Molino</title><style>body{margin:0;background:#f4f8f6;color:#064e45;font:16px/1.55 Arial,sans-serif}.aviso{max-width:640px;margin:10vh auto;padding:36px;background:#fff;border:1px solid #b8d8d0;border-radius:16px;box-shadow:0 12px 30px #064e4518}h1{margin-top:0;font-size:28px}a{display:inline-block;margin-top:12px;padding:11px 18px;border-radius:8px;background:#00796b;color:#fff;text-decoration:none;font-weight:700}</style></head><body><main class="aviso"><h1>No pudimos completar esta acción</h1><p>Ocurrió un problema temporal. Volvé a intentarlo o regresá a tu cuenta. El detalle técnico quedó registrado para revisarlo.</p><a href="/index.php">Volver al inicio</a></main></body></html>';
        exit;
    }
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
