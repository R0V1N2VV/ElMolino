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

    public function procesarGestion(array $entrada, array $archivos, array $usuario): string
    {
        if (!Sesion::validarCsrf($entrada['token_csrf'] ?? null)) {
            throw new RuntimeException('La sesión del formulario venció. Volvé a intentarlo.');
        }

        $accion = (string) ($entrada['accion'] ?? 'guardar');
        if ($accion === 'eliminar') {
            $id = (int) ($entrada['id'] ?? 0);
            if ($id <= 0) throw new InvalidArgumentException('La actividad seleccionada no es válida.');
            $this->repositorio->eliminar($id);
            return 'Actividad eliminada.';
        }

        $datos = $this->validarActividad($entrada);
        $datos['imagen'] = $this->imagenes->guardar($archivos['imagen'] ?? null, (string) ($entrada['imagen_actual'] ?? ''));
        $this->repositorio->guardar($datos, isset($usuario['id_usuario']) ? (int) $usuario['id_usuario'] : null);
        return $datos['id'] > 0 ? 'Actividad actualizada.' : 'Actividad agregada.';
    }

    private function validarActividad(array $entrada): array
    {
        $datos = [
            'id' => (int) ($entrada['id'] ?? 0),
            'slug' => $this->crearSlug((string) ($entrada['slug'] ?? $entrada['nombre'] ?? '')),
            'nombre' => trim((string) ($entrada['nombre'] ?? '')),
            'categoria' => trim((string) ($entrada['categoria'] ?? '')),
            'descripcionCorta' => trim((string) ($entrada['descripcionCorta'] ?? '')),
            'descripcion' => trim((string) ($entrada['descripcion'] ?? '')),
            'requisitos' => trim((string) ($entrada['requisitos'] ?? '')),
            'importante' => trim((string) ($entrada['importante'] ?? '')),
            'dias' => trim((string) ($entrada['dias'] ?? '')),
            'horario' => trim((string) ($entrada['horario'] ?? '')),
            'momento' => $this->valorPermitido((string) ($entrada['momento'] ?? ''), ['manana', 'tarde', 'noche'], ''),
            'duracion' => trim((string) ($entrada['duracion'] ?? '')),
            'sector' => trim((string) ($entrada['sector'] ?? '')),
            'responsable' => trim((string) ($entrada['responsable'] ?? '')),
            'cupo' => (int) ($entrada['cupo'] ?? 0),
            'edad' => $this->valorPermitido((string) ($entrada['edad'] ?? ''), ['todas', 'ninos', 'adolescentes', 'adultos'], ''),
            'modalidad' => $this->valorPermitido((string) ($entrada['modalidad'] ?? ''), ['incluida', 'inscripcion'], ''),
            'precio' => max(0, (float) ($entrada['precio'] ?? 0)),
            'tendencia' => isset($entrada['tendencia']) ? 1 : 0,
        ];

        foreach (['slug', 'nombre', 'categoria', 'descripcionCorta', 'descripcion', 'requisitos', 'importante', 'dias', 'horario', 'momento', 'duracion', 'sector', 'responsable', 'edad', 'modalidad'] as $campo) {
            if ($datos[$campo] === '') throw new InvalidArgumentException('Completá todos los campos obligatorios.');
        }
        if ($datos['cupo'] < 1 || $datos['cupo'] > 500) throw new InvalidArgumentException('El cupo debe estar entre 1 y 500 personas.');
        return $datos;
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
