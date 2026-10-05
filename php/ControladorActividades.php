<?php
declare(strict_types=1);

final class ControladorActividades
{
    public function __construct(
        private FuenteActividades $repositorio,
        private GestorImagenActividad $imagenes
    ) {}

    public function portada(): array
    {
        return [
            'categorias' => $this->repositorio->listarCategorias(),
            'tendencias' => $this->repositorio->listarTendencias(),
        ];
    }

    public function categoria(array $entrada): array
    {
        $categorias = $this->repositorio->listarCategorias();
        $slug = trim((string) ($entrada['categoria'] ?? ($categorias[0]->slug ?? '')));
        $categoria = $this->repositorio->buscarCategoria($slug) ?? ($categorias[0] ?? null);
        if (!$categoria) throw new RuntimeException('No hay categorías disponibles.');

        $filtros = [
            'busqueda' => trim((string) ($entrada['buscar'] ?? '')),
            'edad' => $this->valorPermitido((string) ($entrada['edad'] ?? 'todas'), ['todas', 'ninos', 'adolescentes', 'adultos'], 'todas'),
            'momento' => $this->valorPermitido((string) ($entrada['momento'] ?? 'todos'), ['todos', 'manana', 'tarde', 'noche'], 'todos'),
            'modalidad' => $this->valorPermitido((string) ($entrada['modalidad'] ?? 'todas'), ['todas', 'incluida', 'inscripcion'], 'todas'),
            'orden' => $this->valorPermitido((string) ($entrada['orden'] ?? 'recomendadas'), ['recomendadas', 'nombre', 'horario'], 'recomendadas'),
        ];

        return [
            'categoria' => $categoria,
            'filtros' => $filtros,
            'actividades' => $this->repositorio->filtrarPorCategoria($categoria->slug, $filtros),
        ];
    }

    public function detalle(string $slug): Actividad
    {
        $actividad = $this->repositorio->buscarPorSlug(trim($slug));
        if (!$actividad) throw new RuntimeException('La actividad solicitada no existe.');
        return $actividad;
    }

    public function administracion(): array
    {
        return [
            'categorias' => $this->repositorio->listarCategorias(),
            'actividades' => $this->repositorio->listarAdministracion(),
        ];
    }

    public function procesarGestion(array $entrada, array $archivos, array $usuario): array
    {
        if (!Sesion::validarCsrf($entrada['token_csrf'] ?? null)) {
            throw new RuntimeException('La sesión del formulario venció. Volvé a intentarlo.');
        }

        $accion = (string) ($entrada['accion'] ?? 'guardar');
        if ($accion === 'guardar_categoria') {
            $datos = $this->validarCategoria($entrada);
            $datos['imagen'] = $this->imagenes->guardar($archivos['imagen_categoria'] ?? null);
            if ($datos['imagen'] === '') {
                throw new InvalidArgumentException('Seleccioná una imagen para la categoría.');
            }
            $this->repositorio->guardarCategoria($datos);
            return ['accion' => 'guardar_categoria', 'mensaje' => 'Categoría agregada.'];
        }

        if ($accion === 'eliminar') {
            $id = (int) ($entrada['id'] ?? 0);
            if ($id <= 0) throw new InvalidArgumentException('La actividad seleccionada no es válida.');
            $this->repositorio->eliminar($id);
            return ['accion' => 'eliminar', 'mensaje' => 'Actividad eliminada.'];
        }

        $datos = $this->validarActividad($entrada);
        if ($datos['id'] === 0) {
            $datos['slug'] = $this->crearSlugDisponible($datos['slug']);
        }
        $datos['imagen'] = $this->imagenes->guardar($archivos['imagen'] ?? null, (string) ($entrada['imagen_actual'] ?? ''));
        try {
            $idGuardado = $this->repositorio->guardar($datos, isset($usuario['id_usuario']) ? (int) $usuario['id_usuario'] : null);
        } catch (PDOException $excepcion) {
            error_log('No se pudo guardar la actividad: ' . $excepcion->getMessage());
            throw new RuntimeException('No se pudo guardar la actividad en la base de datos. Verificá que esté instalada la actualización de actividades.');
        }
        if ($idGuardado <= 0) {
            throw new RuntimeException('La actividad no pudo guardarse. Volvé a intentarlo.');
        }
        if (!$this->repositorio->buscarPorSlug($datos['slug'])) {
            throw new RuntimeException('La actividad no pudo publicarse. Volvé a intentarlo.');
        }
        return [
            'accion' => 'guardar',
            'mensaje' => $datos['id'] > 0 ? 'Actividad actualizada.' : 'Actividad agregada.',
            'slug' => $datos['slug'],
            'categoria' => $datos['categoria'],
        ];
    }

    private function validarCategoria(array $entrada): array
    {
        $nombre = trim((string) ($entrada['nombre_categoria'] ?? ''));
        $descripcion = trim((string) ($entrada['descripcion_categoria'] ?? ''));
        $slug = $this->crearSlug($nombre);

        if ($nombre === '' || $descripcion === '' || $slug === '') {
            throw new InvalidArgumentException('Completá el nombre y la descripción de la categoría.');
        }
        if (strlen($nombre) > 60 || strlen($descripcion) > 160) {
            throw new InvalidArgumentException('El nombre o la descripción de la categoría son demasiado largos.');
        }

        return ['slug' => $slug, 'nombre' => $nombre, 'descripcion' => $descripcion];
    }

    private function validarActividad(array $entrada): array
    {
        $nombre = trim((string) ($entrada['nombre'] ?? ''));
        $slugGuardado = trim((string) ($entrada['slug'] ?? ''));
        $horaInicio = trim((string) ($entrada['hora_inicio'] ?? ''));
        $horaFin = trim((string) ($entrada['hora_fin'] ?? ''));

        if (!$this->esHoraValida($horaInicio) || !$this->esHoraValida($horaFin)) {
            throw new InvalidArgumentException('Seleccioná una hora de inicio y una hora de finalización válidas.');
        }
        if ($horaFin <= $horaInicio) {
            throw new InvalidArgumentException('La hora de finalización debe ser posterior a la hora de inicio.');
        }

        $datos = [
            'id' => (int) ($entrada['id'] ?? 0),
            'slug' => $this->crearSlug($slugGuardado !== '' ? $slugGuardado : $nombre),
            'nombre' => $nombre,
            'categoria' => trim((string) ($entrada['categoria'] ?? '')),
            'descripcionCorta' => trim((string) ($entrada['descripcionCorta'] ?? '')),
            'descripcion' => trim((string) ($entrada['descripcion'] ?? '')),
            'requisitos' => trim((string) ($entrada['requisitos'] ?? '')),
            'importante' => trim((string) ($entrada['importante'] ?? '')),
            'dias' => trim((string) ($entrada['dias'] ?? '')),
            'horario' => $horaInicio . ' a ' . $horaFin,
            'momento' => $this->momentoDesdeHora($horaInicio),
            'duracion' => '',
            'sector' => trim((string) ($entrada['sector'] ?? '')),
            'responsable' => trim((string) ($entrada['responsable'] ?? '')),
            'cupo' => (int) ($entrada['cupo'] ?? 0),
            'edad' => $this->valorPermitido((string) ($entrada['edad'] ?? ''), ['todas', 'ninos', 'adolescentes', 'adultos'], ''),
            'modalidad' => $this->valorPermitido((string) ($entrada['modalidad'] ?? ''), ['incluida', 'inscripcion'], ''),
            'precio' => max(0, (float) ($entrada['precio'] ?? 0)),
            'tendencia' => isset($entrada['tendencia']) ? 1 : 0,
        ];

        foreach (['slug', 'nombre', 'categoria', 'descripcionCorta', 'descripcion', 'dias', 'horario', 'momento', 'sector', 'responsable', 'edad', 'modalidad'] as $campo) {
            if ($datos[$campo] === '') throw new InvalidArgumentException('Completá todos los campos obligatorios.');
        }
        if ($datos['modalidad'] === 'incluida') {
            $datos['precio'] = 0;
        } elseif ($datos['precio'] <= 0) {
            throw new InvalidArgumentException('Ingresá el precio de la actividad paga.');
        }
        if ($datos['cupo'] < 1 || $datos['cupo'] > 500) throw new InvalidArgumentException('El cupo debe estar entre 1 y 500 personas.');
        return $datos;
    }

    private function esHoraValida(string $hora): bool
    {
        return preg_match('/^(?:[01]\d|2[0-3]):[0-5]\d$/', $hora) === 1;
    }

    private function momentoDesdeHora(string $hora): string
    {
        $horaNumerica = (int) substr($hora, 0, 2);
        if ($horaNumerica < 12) return 'manana';
        if ($horaNumerica < 19) return 'tarde';
        return 'noche';
    }

    private function crearSlugDisponible(string $base): string
    {
        $slug = $base;
        $sufijo = 2;
        while ($this->repositorio->buscarPorSlug($slug)) {
            $slug = $base . '-' . $sufijo;
            $sufijo++;
        }
        return $slug;
    }

    private function crearSlug(string $texto): string
    {
        $texto = iconv('UTF-8', 'ASCII//TRANSLIT//IGNORE', trim($texto)) ?: trim($texto);
        $texto = strtolower((string) preg_replace('/[^a-zA-Z0-9]+/', '-', $texto));
        return trim($texto, '-');
    }

    private function valorPermitido(string $valor, array $permitidos, string $predeterminado): string
    {
        return in_array($valor, $permitidos, true) ? $valor : $predeterminado;
    }
}
