<?php
/**
 * Centraliza los datos bancarios que se muestran al cliente.
 *
 * Los valores reales se configuran como variables de entorno para no guardar
 * información bancaria dentro del repositorio.
 */
declare(strict_types=1);

final class ConfiguracionPagos
{
    /** @return array{titular:string, alias:string, cbu:string, cuit:string, configurada:bool} */
    public static function transferencia(): array
    {
        $archivo = __DIR__ . '/config_pago.php';
        $local = file_exists($archivo) ? (array) require $archivo : [];
        $titular = trim((string) (getenv('EL_MOLINO_PAGO_TITULAR') ?: ($local['titular'] ?? '')));
        $alias = trim((string) (getenv('EL_MOLINO_PAGO_ALIAS') ?: ($local['alias'] ?? '')));
        $cbu = preg_replace('/\D+/', '', (string) (getenv('EL_MOLINO_PAGO_CBU') ?: ($local['cbu'] ?? '')));
        $cuit = preg_replace('/\D+/', '', (string) (getenv('EL_MOLINO_PAGO_CUIT') ?: ($local['cuit'] ?? '')));

        return [
            'titular' => $titular,
            'alias' => $alias,
            'cbu' => $cbu,
            'cuit' => $cuit,
            'configurada' => $titular !== '' && ($alias !== '' || $cbu !== ''),
        ];
    }
}
