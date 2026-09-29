<?php
declare(strict_types=1);

final class Actividad
{
    public function __construct(
        public int $id,
        public string $slug,
        public string $nombre,
        public string $categoria,
        public string $categoriaNombre,
        public string $descripcionCorta,
        public string $descripcion,
        public string $requisitos,
        public string $importante,
        public string $dias,
        public string $horario,
        public string $momento,
        public string $duracion,
        public string $sector,
        public string $responsable,
        public int $cupo,
        public string $edad,
        public string $modalidad,
        public float $precio,
        public bool $tendencia,
        public string $imagen
    ) {}

    public static function desdeFila(array $fila): self
    {
        return new self(
            (int) $fila['idActividad'],
            (string) $fila['slug'],
            (string) $fila['nombre'],
            (string) $fila['categoria'],
            (string) $fila['categoriaNombre'],
            (string) ($fila['descripcionCorta'] ?? ''),
            (string) ($fila['descripcion'] ?? ''),
            (string) ($fila['requisitos'] ?? ''),
            (string) ($fila['informacionImportante'] ?? ''),
            (string) ($fila['dias'] ?? ''),
            (string) ($fila['horario'] ?? ''),
            (string) ($fila['momentoDia'] ?? ''),
            (string) ($fila['Duracion'] ?? ''),
            (string) ($fila['sector'] ?? ''),
            (string) ($fila['responsable'] ?? ''),
            (int) ($fila['cupoMax'] ?? 0),
            (string) ($fila['edadRecomendada'] ?? 'todas'),
            (string) ($fila['modalidad'] ?? 'incluida'),
            (float) ($fila['precio'] ?? 0),
            (bool) ($fila['tendencia'] ?? false),
            (string) ($fila['imagenFinal'] ?? $fila['imagen'] ?? '')
        );
    }
}
