<?php
/**
 * ARCHIVO EXPLICADO: php/Actividad.php
 *
 * Modelo de datos que representa una actividad dentro del sistema.
 * Las funciones de este archivo están documentadas junto a su declaración.
 */

declare(strict_types=1);

final class Actividad
{
    /**
     * Inicializa el objeto y deja preparadas las dependencias que utilizarán sus demás métodos.
     *
     * @param mixed $id Valor de id que recibe la función.
     * @param mixed $slug Valor de slug que recibe la función.
     * @param mixed $nombre Valor de nombre que recibe la función.
     * @param mixed $categoria Valor de categoria que recibe la función.
     * @param mixed $categoriaNombre Valor de categoria nombre que recibe la función.
     * @param mixed $descripcionCorta Valor de descripcion corta que recibe la función.
     * @param mixed $descripcion Valor de descripcion que recibe la función.
     * @param mixed $requisitos Valor de requisitos que recibe la función.
     * @param mixed $importante Valor de importante que recibe la función.
     * @param mixed $dias Valor de dias que recibe la función.
     * @param mixed $horario Valor de horario que recibe la función.
     * @param mixed $momento Valor de momento que recibe la función.
     * @param mixed $duracion Valor de duracion que recibe la función.
     * @param mixed $sector Valor de sector que recibe la función.
     * @param mixed $responsable Valor de responsable que recibe la función.
     * @param mixed $cupo Valor de cupo que recibe la función.
     * @param mixed $edad Valor de edad que recibe la función.
     * @param mixed $modalidad Valor de modalidad que recibe la función.
     * @param mixed $precio Valor de precio que recibe la función.
     * @param mixed $tendencia Valor de tendencia que recibe la función.
     * @param mixed $imagen Valor de imagen que recibe la función.
     * @param mixed $cantidadInscriptos Valor de cantidad inscriptos que recibe la función.
     */
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
        public string $imagen,
        public int $cantidadInscriptos = 0
    ) {}

    /**
     * Ejecuta la lógica de “cupos disponibles” en Actividad.php.
     * @return int Resultado producido por esta operación.
     */
    public function cuposDisponibles(): int
    {
        return max(0, $this->cupo - $this->cantidadInscriptos);
    }

    /**
     * Comprueba esta completa y devuelve verdadero o falso según se cumpla la condición.
     * @return bool Resultado producido por esta operación.
     */
    public function estaCompleta(): bool
    {
        return $this->cuposDisponibles() === 0;
    }

    /**
     * Indica si una fecha concreta coincide con los días semanales configurados
     * para la actividad y está dentro de los próximos noventa días.
     */
    public function admiteFecha(string $fecha): bool
    {
        $seleccionada = DateTimeImmutable::createFromFormat('!Y-m-d', $fecha);
        if (!$seleccionada || $seleccionada->format('Y-m-d') !== $fecha) return false;
        $hoy = new DateTimeImmutable('today');
        if ($seleccionada < $hoy || $seleccionada > $hoy->modify('+90 days')) return false;

        $diasNormalizados = strtolower(strtr($this->dias, [
            'Á' => 'a', 'É' => 'e', 'Í' => 'i', 'Ó' => 'o', 'Ú' => 'u',
            'á' => 'a', 'é' => 'e', 'í' => 'i', 'ó' => 'o', 'ú' => 'u',
        ]));
        if (str_contains($diasNormalizados, 'todos los dias')) return true;
        $nombres = [1 => 'lunes', 2 => 'martes', 3 => 'miercoles', 4 => 'jueves', 5 => 'viernes', 6 => 'sabado', 7 => 'domingo'];
        return str_contains($diasNormalizados, $nombres[(int) $seleccionada->format('N')]);
    }

    /**
     * Genera las próximas fechas válidas que se muestran en el selector de inscripción.
     *
     * @return array<int, array{valor: string, etiqueta: string}>
     */
    public function fechasDisponibles(int $limite = 60): array
    {
        $fechas = [];
        $cursor = new DateTimeImmutable('today');
        for ($i = 0; $i <= 90 && count($fechas) < $limite; $i++, $cursor = $cursor->modify('+1 day')) {
            $valor = $cursor->format('Y-m-d');
            if (!$this->admiteFecha($valor)) continue;
            $nombres = [1 => 'Lunes', 2 => 'Martes', 3 => 'Miércoles', 4 => 'Jueves', 5 => 'Viernes', 6 => 'Sábado', 7 => 'Domingo'];
            $fechas[] = [
                'valor' => $valor,
                'etiqueta' => $nombres[(int) $cursor->format('N')] . ' ' . $cursor->format('d/m/Y'),
            ];
        }
        return $fechas;
    }

    /**
     * Ejecuta la lógica de “desde fila” en Actividad.php.
     *
     * @param mixed $fila Valor de fila que recibe la función.
     * @return self Resultado producido por esta operación.
     */
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
            (string) ($fila['imagenFinal'] ?? $fila['imagen'] ?? ''),
            (int) ($fila['cantidadInscriptos'] ?? 0)
        );
    }
}
