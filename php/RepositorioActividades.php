<?php
declare(strict_types=1);

final class RepositorioActividades implements FuenteActividades
{
    private const CAMPOS_ACTIVIDAD = <<<'SQL'
        SELECT a.idActividad, a.slug, a.nombre, a.descripcionCorta, a.descripcion,
               a.requisitos, a.informacionImportante, a.dias, a.horario,
               a.momentoDia, a.Duracion, a.sector, a.responsable, a.cupoMax,
               a.edadRecomendada, a.modalidad, a.precio, a.tendencia, a.imagen,
               c.slug AS categoria, c.nombre AS categoriaNombre,
               COALESCE(NULLIF(a.imagen, ''), c.imagen) AS imagenFinal
          FROM Actividad a
          JOIN CategoriaActividad c ON c.idCategoria = a.idCategoria
    SQL;

    public function __construct(private PDO $conexion) {}

    /** @return CategoriaActividad[] */
    public function listarCategorias(): array
    {
        $sql = 'SELECT c.idCategoria, c.slug, c.nombre, c.descripcion, c.imagen,
                       COUNT(a.idActividad) AS cantidadActividades
                  FROM CategoriaActividad c
             LEFT JOIN Actividad a ON a.idCategoria = c.idCategoria AND a.activa = 1
                 WHERE c.activa = 1
              GROUP BY c.idCategoria, c.slug, c.nombre, c.descripcion, c.imagen
              ORDER BY c.idCategoria';
        return array_map([CategoriaActividad::class, 'desdeFila'], $this->conexion->query($sql)->fetchAll());
    }

    public function buscarCategoria(string $slug): ?CategoriaActividad
    {
        $consulta = $this->conexion->prepare(
            'SELECT c.idCategoria, c.slug, c.nombre, c.descripcion, c.imagen,
                    COUNT(a.idActividad) AS cantidadActividades
               FROM CategoriaActividad c
          LEFT JOIN Actividad a ON a.idCategoria = c.idCategoria AND a.activa = 1
              WHERE c.slug = :slug AND c.activa = 1
           GROUP BY c.idCategoria, c.slug, c.nombre, c.descripcion, c.imagen
              LIMIT 1'
        );
        $consulta->execute(['slug' => $slug]);
        $fila = $consulta->fetch();
        return $fila ? CategoriaActividad::desdeFila($fila) : null;
    }

    /** @return Actividad[] */
    public function listarTendencias(int $limite = 3): array
    {
        $sql = self::CAMPOS_ACTIVIDAD . ' WHERE a.activa = 1 AND c.activa = 1 AND a.tendencia = 1 ORDER BY a.idActividad LIMIT :limite';
        $consulta = $this->conexion->prepare($sql);
        $consulta->bindValue(':limite', $limite, PDO::PARAM_INT);
        $consulta->execute();
        return $this->convertirActividades($consulta->fetchAll());
    }

    /** @return Actividad[] */
    public function filtrarPorCategoria(string $categoria, array $filtros): array
    {
        $condiciones = ['a.activa = 1', 'c.activa = 1', 'c.slug = :categoria'];
        $parametros = ['categoria' => $categoria];

        if ($filtros['busqueda'] !== '') {
            $condiciones[] = '(a.nombre LIKE :busquedaNombre OR a.descripcionCorta LIKE :busquedaDescripcion OR a.sector LIKE :busquedaSector)';
            $termino = '%' . $filtros['busqueda'] . '%';
            $parametros['busquedaNombre'] = $termino;
            $parametros['busquedaDescripcion'] = $termino;
            $parametros['busquedaSector'] = $termino;
        }
        if ($filtros['edad'] !== 'todas') {
            $condiciones[] = "(a.edadRecomendada = :edad OR a.edadRecomendada = 'todas')";
            $parametros['edad'] = $filtros['edad'];
        }
        if ($filtros['momento'] !== 'todos') {
            $condiciones[] = 'a.momentoDia = :momento';
            $parametros['momento'] = $filtros['momento'];
        }
        if ($filtros['modalidad'] !== 'todas') {
            $condiciones[] = 'a.modalidad = :modalidad';
            $parametros['modalidad'] = $filtros['modalidad'];
        }

        $orden = match ($filtros['orden']) {
            'nombre' => 'a.nombre ASC',
            'horario' => 'a.horario ASC',
            default => 'a.tendencia DESC, a.idActividad ASC',
        };
        $sql = self::CAMPOS_ACTIVIDAD . ' WHERE ' . implode(' AND ', $condiciones) . ' ORDER BY ' . $orden;
        $consulta = $this->conexion->prepare($sql);
        $consulta->execute($parametros);
        return $this->convertirActividades($consulta->fetchAll());
    }

    public function buscarPorSlug(string $slug): ?Actividad
    {
        $consulta = $this->conexion->prepare(self::CAMPOS_ACTIVIDAD . ' WHERE a.slug = :slug AND a.activa = 1 AND c.activa = 1 LIMIT 1');
        $consulta->execute(['slug' => $slug]);
        $fila = $consulta->fetch();
        return $fila ? Actividad::desdeFila($fila) : null;
    }

    /** @return Actividad[] */
    public function listarAdministracion(): array
    {
        $consulta = $this->conexion->query(self::CAMPOS_ACTIVIDAD . ' WHERE a.activa = 1 ORDER BY a.nombre');
        return $this->convertirActividades($consulta->fetchAll());
    }

    public function guardar(array $datos, ?int $idUsuario): int
    {
        $idCategoria = $this->idCategoria((string) $datos['categoria']);
        $parametros = [
            'slug' => $datos['slug'], 'nombre' => $datos['nombre'], 'idCategoria' => $idCategoria,
            'descripcionCorta' => $datos['descripcionCorta'], 'descripcion' => $datos['descripcion'],
            'requisitos' => $datos['requisitos'], 'informacionImportante' => $datos['importante'],
            'dias' => $datos['dias'], 'horario' => $datos['horario'], 'momentoDia' => $datos['momento'],
            'Duracion' => $datos['duracion'], 'sector' => $datos['sector'], 'responsable' => $datos['responsable'],
            'cupoMax' => $datos['cupo'], 'edadRecomendada' => $datos['edad'], 'modalidad' => $datos['modalidad'],
            'precio' => $datos['precio'], 'pago' => $datos['modalidad'] === 'incluida' ? 'Incluida' : 'Con inscripción',
            'imagen' => $datos['imagen'], 'tendencia' => $datos['tendencia'], 'idUsuario' => $idUsuario,
        ];

        if ($datos['id'] > 0) {
            $parametros['idActividad'] = $datos['id'];
            $sql = 'UPDATE Actividad SET slug=:slug, nombre=:nombre, idCategoria=:idCategoria,
                    descripcionCorta=:descripcionCorta, descripcion=:descripcion, requisitos=:requisitos,
                    informacionImportante=:informacionImportante, dias=:dias, horario=:horario,
                    momentoDia=:momentoDia, Duracion=:Duracion, sector=:sector, responsable=:responsable,
                    cupoMax=:cupoMax, edadRecomendada=:edadRecomendada, modalidad=:modalidad,
                    precio=:precio, pago=:pago, imagen=:imagen, tendencia=:tendencia, idUsuario=:idUsuario
                    WHERE idActividad=:idActividad';
            $this->conexion->prepare($sql)->execute($parametros);
            return $datos['id'];
        }

        $sql = 'INSERT INTO Actividad
                (slug, nombre, idCategoria, descripcionCorta, descripcion, requisitos, informacionImportante,
                 dias, horario, momentoDia, Duracion, sector, responsable, cupoMax, edadRecomendada,
                 modalidad, precio, pago, imagen, tendencia, activa, idUsuario)
                VALUES
                (:slug, :nombre, :idCategoria, :descripcionCorta, :descripcion, :requisitos, :informacionImportante,
                 :dias, :horario, :momentoDia, :Duracion, :sector, :responsable, :cupoMax, :edadRecomendada,
                 :modalidad, :precio, :pago, :imagen, :tendencia, 1, :idUsuario)';
        $this->conexion->prepare($sql)->execute($parametros);
        return (int) $this->conexion->lastInsertId();
    }

    public function eliminar(int $id): void
    {
        $consulta = $this->conexion->prepare('UPDATE Actividad SET activa = 0 WHERE idActividad = :id');
        $consulta->execute(['id' => $id]);
    }

    private function idCategoria(string $slug): int
    {
        $consulta = $this->conexion->prepare('SELECT idCategoria FROM CategoriaActividad WHERE slug = :slug AND activa = 1 LIMIT 1');
        $consulta->execute(['slug' => $slug]);
        $id = $consulta->fetchColumn();
        if (!$id) throw new InvalidArgumentException('La categoría seleccionada no existe.');
        return (int) $id;
    }

    /** @return Actividad[] */
    private function convertirActividades(array $filas): array
    {
        return array_map([Actividad::class, 'desdeFila'], $filas);
    }
}
