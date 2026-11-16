<?php
declare(strict_types=1);

/** Guarda de forma controlada el comprobante que adjunta el cliente. */
final class GestorComprobantePago
{
    /** @param array<string,mixed> $archivo */
    public static function guardar(array $archivo): string
    {
        if (($archivo['error'] ?? UPLOAD_ERR_NO_FILE) !== UPLOAD_ERR_OK) {
            throw new InvalidArgumentException('Adjuntá la captura o PDF del comprobante de transferencia.');
        }
        if ((int) ($archivo['size'] ?? 0) <= 0 || (int) $archivo['size'] > 5 * 1024 * 1024) {
            throw new InvalidArgumentException('El comprobante debe pesar como máximo 5 MB.');
        }
        $tipos = ['image/jpeg' => 'jpg', 'image/png' => 'png', 'image/webp' => 'webp', 'application/pdf' => 'pdf'];
        $tipo = (new finfo(FILEINFO_MIME_TYPE))->file((string) ($archivo['tmp_name'] ?? ''));
        if (!isset($tipos[$tipo])) throw new InvalidArgumentException('Adjuntá una imagen JPG, PNG, WEBP o un PDF.');

        $carpeta = dirname(__DIR__) . '/uploads/comprobantes';
        if (!is_dir($carpeta) && !mkdir($carpeta, 0755, true) && !is_dir($carpeta)) {
            throw new RuntimeException('No se pudo preparar la carpeta de comprobantes.');
        }
        $nombre = bin2hex(random_bytes(16)) . '.' . $tipos[$tipo];
        if (!move_uploaded_file((string) $archivo['tmp_name'], $carpeta . '/' . $nombre)) {
            throw new RuntimeException('No se pudo guardar el comprobante. Volvé a intentarlo.');
        }
        return 'uploads/comprobantes/' . $nombre;
    }
}
