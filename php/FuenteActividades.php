<?php
declare(strict_types=1);

interface FuenteActividades
{
    /** @return CategoriaActividad[] */
    public function listarCategorias(): array;

    public function buscarCategoria(string $slug): ?CategoriaActividad;

    /** @return Actividad[] */
    public function listarTendencias(int $limite = 3): array;

    /** @return Actividad[] */
    public function filtrarPorCategoria(string $categoria, array $filtros): array;

    public function buscarPorSlug(string $slug): ?Actividad;

    /** @return Actividad[] */
    public function listarAdministracion(): array;

    public function guardarCategoria(array $datos): int;

    public function eliminarCategoria(int $id): void;

    public function guardar(array $datos, ?int $idUsuario): int;

    public function eliminar(int $id): void;
}
