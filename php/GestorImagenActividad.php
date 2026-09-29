<?php
declare(strict_types=1);

final class GestorImagenActividad
{
    private const TIPOS = ['image/jpeg' => 'jpg', 'image/png' => 'png', 'image/webp' => 'webp'];

    public function __construct(private string $directorio, private string $rutaPublica = 'imgs/actividades') {}

    public function guardar(?array $archivo, string $imagenAnterior = ''): string
    {
        if (!$archivo || ($archivo['error'] ?? UPLOAD_ERR_NO_FILE) === UPLOAD_ERR_NO_FILE) return $imagenAnterior;
        if (($archivo['error'] ?? UPLOAD_ERR_OK) !== UPLOAD_ERR_OK) throw new RuntimeException('No se pudo subir la imagen.');
        if ((int) ($archivo['size'] ?? 0) > 5 * 1024 * 1024) throw new RuntimeException('La imagen no puede superar los 5 MB.');

        $tipo = (new finfo(FILEINFO_MIME_TYPE))->file((string) $archivo['tmp_name']);
        if (!isset(self::TIPOS[$tipo])) throw new RuntimeException('La imagen debe ser JPG, PNG o WEBP.');
        if (!is_dir($this->directorio) && !mkdir($this->directorio, 0775, true) && !is_dir($this->directorio)) {
            throw new RuntimeException('No se pudo crear la carpeta de imágenes.');
        }

        $nombre = bin2hex(random_bytes(12)) . '.' . self::TIPOS[$tipo];
        if (!move_uploaded_file((string) $archivo['tmp_name'], $this->directorio . DIRECTORY_SEPARATOR . $nombre)) {
            throw new RuntimeException('No se pudo guardar la imagen.');
        }
        return $this->rutaPublica . '/' . $nombre;
    }
}
