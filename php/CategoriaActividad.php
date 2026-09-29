<?php
declare(strict_types=1);

final class CategoriaActividad
{
    public function __construct(
        public int $id,
        public string $slug,
        public string $nombre,
        public string $descripcion,
        public string $imagen,
        public int $cantidadActividades = 0
    ) {}

    public static function desdeFila(array $fila): self
    {
        return new self(
            (int) $fila['idCategoria'],
            (string) $fila['slug'],
            (string) $fila['nombre'],
            (string) $fila['descripcion'],
            (string) ($fila['imagen'] ?? ''),
            (int) ($fila['cantidadActividades'] ?? 0)
        );
    }
}
