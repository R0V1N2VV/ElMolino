<?php
/**
 * ARCHIVO EXPLICADO: php/FuenteActividades.php
 *
 * Contrato que define las operaciones que debe ofrecer una fuente de actividades.
 * Las funciones de este archivo están documentadas junto a su declaración.
 */

declare(strict_types=1);

interface FuenteActividades
{
    /** @return CategoriaActividad[] */
    public function listarCategorias(): array;

    /**
     * Busca categoria y devuelve el resultado encontrado.
     *
     * @param mixed $slug Valor de slug que recibe la función.
     * @return ?CategoriaActividad Resultado producido por esta operación.
     */
    public function buscarCategoria(string $slug): ?CategoriaActividad;

    /** @return Actividad[] */
    public function listarTendencias(int $limite = 3): array;

    /** @return Actividad[] */
    public function filtrarPorCategoria(string $categoria, array $filtros): array;

    /**
     * Busca por slug y devuelve el resultado encontrado.
     *
     * @param mixed $slug Valor de slug que recibe la función.
     * @return ?Actividad Resultado producido por esta operación.
     */
    public function buscarPorSlug(string $slug): ?Actividad;

    /** @return Actividad[] */
    public function listarAdministracion(): array;

    /**
     * Valida y guarda categoria en el destino correspondiente.
     *
     * @param mixed $datos Valor de datos que recibe la función.
     * @return int Resultado producido por esta operación.
     */
    public function guardarCategoria(array $datos): int;

    /**
     * Elimina categoria del sistema.
     *
     * @param mixed $id Valor de id que recibe la función.
     */
    public function eliminarCategoria(int $id): void;

    /**
     * Valida y guarda la información en el destino correspondiente.
     *
     * @param mixed $datos Valor de datos que recibe la función.
     * @param mixed $idUsuario Valor de id usuario que recibe la función.
     * @return int Resultado producido por esta operación.
     */
    public function guardar(array $datos, ?int $idUsuario): int;

    /**
     * Elimina el elemento indicado del sistema.
     *
     * @param mixed $id Valor de id que recibe la función.
     */
    public function eliminar(int $id): void;
}
