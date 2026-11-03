<?php
/**
 * ARCHIVO EXPLICADO: php/CategoriaActividad.php
 *
 * Modelo de datos que representa una categoría de actividades.
 * Las funciones de este archivo están documentadas junto a su declaración.
 */

declare(strict_types=1);

final class CategoriaActividad
{
    /**
     * Inicializa el objeto y deja preparadas las dependencias que utilizarán sus demás métodos.
     *
     * @param mixed $id Valor de id que recibe la función.
     * @param mixed $slug Valor de slug que recibe la función.
     * @param mixed $nombre Valor de nombre que recibe la función.
     * @param mixed $descripcion Valor de descripcion que recibe la función.
     * @param mixed $imagen Valor de imagen que recibe la función.
     * @param mixed $cantidadActividades Valor de cantidad actividades que recibe la función.
     */
    public function __construct(
        public int $id,
        public string $slug,
        public string $nombre,
        public string $descripcion,
        public string $imagen,
        public int $cantidadActividades = 0
    ) {}

    /**
     * Ejecuta la lógica de “desde fila” en CategoriaActividad.php.
     *
     * @param mixed $fila Valor de fila que recibe la función.
     * @return self Resultado producido por esta operación.
     */
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
