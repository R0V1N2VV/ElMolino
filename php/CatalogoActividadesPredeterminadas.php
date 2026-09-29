<?php
declare(strict_types=1);

final class CatalogoActividadesPredeterminadas implements FuenteActividades
{
    /** @return CategoriaActividad[] */
    public function listarCategorias(): array
    {
        $cantidades = [];
        foreach ($this->actividades() as $actividad) {
            $cantidades[$actividad->categoria] = ($cantidades[$actividad->categoria] ?? 0) + 1;
        }

        return array_map(
            static fn (array $categoria): CategoriaActividad => new CategoriaActividad(
                $categoria['id'],
                $categoria['slug'],
                $categoria['nombre'],
                $categoria['descripcion'],
                $categoria['imagen'],
                $cantidades[$categoria['slug']] ?? 0
            ),
            $this->categorias()
        );
    }

    public function buscarCategoria(string $slug): ?CategoriaActividad
    {
        foreach ($this->listarCategorias() as $categoria) {
            if ($categoria->slug === $slug) return $categoria;
        }
        return null;
    }

    public function listarTendencias(int $limite = 3): array
    {
        $tendencias = array_values(array_filter(
            $this->actividades(),
            static fn (Actividad $actividad): bool => $actividad->tendencia
        ));
        return array_slice($tendencias, 0, max(0, $limite));
    }

    public function filtrarPorCategoria(string $categoria, array $filtros): array
    {
        $actividades = array_values(array_filter(
            $this->actividades(),
            static function (Actividad $actividad) use ($categoria, $filtros): bool {
                if ($actividad->categoria !== $categoria) return false;

                $busqueda = trim((string) ($filtros['busqueda'] ?? ''));
                if ($busqueda !== '') {
                    $coincide = stripos($actividad->nombre, $busqueda) !== false
                        || stripos($actividad->descripcionCorta, $busqueda) !== false
                        || stripos($actividad->sector, $busqueda) !== false;
                    if (!$coincide) return false;
                }

                $edad = (string) ($filtros['edad'] ?? 'todas');
                if ($edad !== 'todas' && $actividad->edad !== $edad && $actividad->edad !== 'todas') return false;

                $momento = (string) ($filtros['momento'] ?? 'todos');
                if ($momento !== 'todos' && $actividad->momento !== $momento) return false;

                $modalidad = (string) ($filtros['modalidad'] ?? 'todas');
                return $modalidad === 'todas' || $actividad->modalidad === $modalidad;
            }
        ));

        $orden = (string) ($filtros['orden'] ?? 'recomendadas');
        usort($actividades, static function (Actividad $a, Actividad $b) use ($orden): int {
            return match ($orden) {
                'nombre' => strcasecmp($a->nombre, $b->nombre),
                'horario' => strcmp($a->horario, $b->horario),
                default => ($b->tendencia <=> $a->tendencia) ?: ($a->id <=> $b->id),
            };
        });
        return $actividades;
    }

    public function buscarPorSlug(string $slug): ?Actividad
    {
        foreach ($this->actividades() as $actividad) {
            if ($actividad->slug === $slug) return $actividad;
        }
        return null;
    }

    public function listarAdministracion(): array
    {
        $actividades = $this->actividades();
        usort($actividades, static fn (Actividad $a, Actividad $b): int => strcasecmp($a->nombre, $b->nombre));
        return $actividades;
    }

    public function guardar(array $datos, ?int $idUsuario): int
    {
        throw new RuntimeException('Las actividades predeterminadas están en modo de solo lectura. Instalá la actualización de la base para guardar cambios.');
    }

    public function eliminar(int $id): void
    {
        throw new RuntimeException('Las actividades predeterminadas están en modo de solo lectura. Instalá la actualización de la base para guardar cambios.');
    }

    /** @return array<int, array{id:int, slug:string, nombre:string, descripcion:string, imagen:string}> */
    private function categorias(): array
    {
        return [
            ['id' => 1, 'slug' => 'caminatas', 'nombre' => 'Caminatas', 'descripcion' => 'Senderos y recorridos guiados', 'imagen' => 'imgs/caminatas.jpg'],
            ['id' => 2, 'slug' => 'deportes', 'nombre' => 'Deportes', 'descripcion' => 'Fútbol, básquet, natación y más', 'imagen' => 'imgs/futb.jpg'],
            ['id' => 3, 'slug' => 'bienestar', 'nombre' => 'Bienestar', 'descripcion' => 'Yoga y propuestas para bajar el ritmo', 'imagen' => 'imgs/bienestar.jpg'],
            ['id' => 4, 'slug' => 'talleres', 'nombre' => 'Talleres', 'descripcion' => 'Experiencias creativas para compartir', 'imagen' => 'imgs/talleres.jpg'],
            ['id' => 5, 'slug' => 'espectaculos', 'nombre' => 'Espectáculos', 'descripcion' => 'Música y propuestas para toda la familia', 'imagen' => 'imgs/espectaculos.jpg'],
            ['id' => 6, 'slug' => 'naturaleza', 'nombre' => 'Naturaleza', 'descripcion' => 'Actividades conectadas con el entorno', 'imagen' => 'imgs/naturaleza.jpg'],
        ];
    }

    /** @return Actividad[] */
    private function actividades(): array
    {
        $categorias = [];
        foreach ($this->categorias() as $categoria) $categorias[$categoria['slug']] = $categoria;

        $datos = [
            [1, 'futbol-recreativo', 'Fútbol recreativo', 'deportes', 'Partidos abiertos para compartir y moverse en equipo.', 'Una propuesta recreativa para disfrutar la cancha, conocer a otras personas y jugar sin exigencias competitivas.', 'Ropa cómoda, calzado deportivo y botella de agua.', 'Los menores participan acompañados por una persona adulta.', 'Todos los días', '17:00', 'tarde', '1 hora y 30 minutos', 'Cancha norte', 'Coordinación deportiva', 20, 'todas', 'incluida', 0, true, 'imgs/futb.jpg'],
            [2, 'yoga-al-aire-libre', 'Yoga al aire libre', 'bienestar', 'Movilidad, respiración y una pausa en contacto con la naturaleza.', 'Una clase suave para relajar el cuerpo, mejorar la movilidad y disfrutar un momento tranquilo al aire libre.', 'Ropa cómoda. Las colchonetas están incluidas.', 'La actividad pasa al salón en caso de lluvia.', 'Martes, jueves y domingos', '09:30', 'manana', '50 minutos', 'Jardín central', 'Equipo de bienestar', 16, 'adultos', 'incluida', 0, true, ''],
            [3, 'recorrido-en-bicicleta', 'Recorrido en bicicleta', 'deportes', 'Senderos, pequeñas paradas y vistas del entorno.', 'La salida combina caminos tranquilos, pequeñas paradas y vistas del entorno. El guía adapta el ritmo al grupo y explica cada tramo antes de comenzar.', 'Ropa cómoda, calzado cerrado y botella de agua. Las bicicletas y los cascos están incluidos; también podés traer tu propio equipo.', 'La actividad puede reprogramarse por condiciones climáticas. Los menores participan acompañados por una persona adulta.', 'Miércoles y sábados', '10:00 y 16:30', 'manana', '1 hora y 15 minutos', 'Recepción del área deportiva', 'Guías de recreación', 12, 'todas', 'inscripcion', 3500, true, ''],
            [4, 'caminata-por-senderos', 'Caminata por senderos', 'caminatas', 'Un recorrido guiado para conocer los rincones verdes del complejo.', 'Caminata de intensidad baja con paradas para observar el paisaje y conocer distintos espacios de El Molino.', 'Calzado cerrado, protector solar y agua.', 'Se suspende en caso de tormenta.', 'Viernes y sábados', '08:30', 'manana', '1 hora', 'Punto de encuentro principal', 'Guías de recreación', 18, 'todas', 'incluida', 0, false, ''],
            [5, 'padel-recreativo', 'Pádel recreativo', 'deportes', 'Cancha disponible para organizar partidos entre amigos.', 'Una propuesta para jugar por turnos en la cancha de pádel del complejo, con opciones para quienes recién empiezan y para grupos con experiencia.', 'Ropa cómoda y calzado deportivo. Se pueden solicitar paletas en recepción.', 'La reserva de cancha depende de la disponibilidad del día.', 'Todos los días', '09:00 a 20:00', 'tarde', 'Turnos de 1 hora', 'Cancha de pádel', 'Coordinación deportiva', 4, 'adolescentes', 'inscripcion', 4000, false, 'imgs/padel.jpg'],
            [6, 'taller-de-pintura', 'Taller de pintura', 'talleres', 'Una propuesta creativa para experimentar con colores y materiales.', 'Un encuentro para crear una obra sencilla inspirada en la naturaleza del complejo. No hace falta experiencia previa.', 'Los materiales están incluidos.', 'Los niños menores de 8 años deben asistir con una persona adulta.', 'Sábados', '15:00', 'tarde', '1 hora y 30 minutos', 'Salón de talleres', 'Equipo cultural', 14, 'todas', 'inscripcion', 2800, false, ''],
            [7, 'espectaculo-familiar', 'Espectáculo familiar', 'espectaculos', 'Música, humor y juegos para cerrar el día en familia.', 'Una presentación pensada para compartir entre grandes y chicos, con artistas invitados y participación del público.', 'Llegar 15 minutos antes para ubicarse.', 'La programación puede cambiar según la fecha de la estadía.', 'Sábados', '20:30', 'noche', '1 hora', 'Salón de eventos', 'Coordinación cultural', 80, 'todas', 'incluida', 0, false, ''],
            [8, 'natacion-recreativa', 'Natación recreativa', 'deportes', 'Actividad guiada en la piscina para moverse y divertirse.', 'Encuentro recreativo en el agua con ejercicios sencillos y juegos adaptados al grupo.', 'Traje de baño, toalla, ojotas y gorro de natación.', 'Sujeta a condiciones climáticas y disponibilidad de la piscina.', 'Martes y jueves', '16:00', 'tarde', '45 minutos', 'Piscina principal', 'Equipo de guardavidas', 12, 'todas', 'inscripcion', 2000, false, ''],
            [9, 'basquet-abierto', 'Básquet abierto', 'deportes', 'Partidos y práctica libre en equipo.', 'La cancha queda disponible para encuentros recreativos y ejercicios básicos coordinados.', 'Ropa cómoda, calzado deportivo y agua.', 'La actividad se organiza según la cantidad de participantes.', 'Lunes, miércoles y viernes', '18:00', 'tarde', '1 hora', 'Playón deportivo', 'Coordinación deportiva', 16, 'adolescentes', 'incluida', 0, false, ''],
            [10, 'avistaje-de-aves', 'Avistaje de aves', 'naturaleza', 'Una salida tranquila para observar las especies del entorno.', 'Recorrido breve con guía por sectores de vegetación y descanso, pensado para reconocer aves sin alterar su ambiente.', 'Calzado cómodo, protector solar y, si tenés, binoculares.', 'Se recomienda mantener silencio durante algunos tramos del recorrido.', 'Domingos', '07:30', 'manana', '1 hora', 'Sendero del bosque', 'Guías de naturaleza', 10, 'todas', 'incluida', 0, false, ''],
        ];

        return array_map(static function (array $fila) use ($categorias): Actividad {
            [$id, $slug, $nombre, $categoria, $descripcionCorta, $descripcion, $requisitos, $importante,
                $dias, $horario, $momento, $duracion, $sector, $responsable, $cupo, $edad,
                $modalidad, $precio, $tendencia, $imagen] = $fila;
            $categoriaDatos = $categorias[$categoria];
            return new Actividad(
                $id, $slug, $nombre, $categoria, $categoriaDatos['nombre'], $descripcionCorta,
                $descripcion, $requisitos, $importante, $dias, $horario, $momento, $duracion,
                $sector, $responsable, $cupo, $edad, $modalidad, (float) $precio, $tendencia,
                $imagen !== '' ? $imagen : $categoriaDatos['imagen']
            );
        }, $datos);
    }
}
