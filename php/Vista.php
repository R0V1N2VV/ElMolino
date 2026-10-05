<?php
declare(strict_types=1);

final class Vista
{
    private function __construct() {}

    public static function escapar(string|int|float|null $valor): string
    {
        return htmlspecialchars((string) $valor, ENT_QUOTES, 'UTF-8');
    }

    public static function precio(Actividad $actividad): string
    {
        if ($actividad->modalidad === 'incluida' || $actividad->precio <= 0) return 'Incluida en la estadía';
        return '$' . number_format($actividad->precio, 0, ',', '.');
    }

    public static function seleccionado(string $actual, string $opcion): string
    {
        return $actual === $opcion ? ' selected' : '';
    }

    public static function marcado(bool $valor): string
    {
        return $valor ? ' checked' : '';
    }

    public static function tarjetaActividad(Actividad $actividad, bool $destacada = false): string
    {
        $clase = $destacada ? ' tarjeta-actividad-destacada' : '';
        $imagen = self::escapar($actividad->imagen ?: 'imgs/futb.jpg');
        $categoria = self::escapar($actividad->categoriaNombre);
        $nombre = self::escapar($actividad->nombre);
        $descripcion = self::escapar($actividad->descripcionCorta);
        $dias = self::escapar($actividad->dias);
        $horario = self::escapar($actividad->horario);
        $slug = rawurlencode($actividad->slug);
        $precio = self::escapar(self::precio($actividad));
        $disponibles = $actividad->cuposDisponibles();
        $estadoCupo = self::escapar(
            $disponibles === 0
                ? 'Sin cupos disponibles'
                : $disponibles . ($disponibles === 1 ? ' lugar disponible' : ' lugares disponibles')
        );
        $claseCupo = $disponibles === 0 ? ' sin-cupo' : '';

        return <<<HTML
            <article class="tarjeta-actividad{$clase}">
                <div class="visual-actividad"><img src="{$imagen}" alt="" loading="lazy"></div>
                <div class="contenido-tarjeta-actividad">
                    <p class="etiqueta-actividad">{$categoria}</p>
                    <h3>{$nombre}</h3>
                    <p>{$descripcion}</p>
                    <div class="datos-breves-actividad"><span>{$dias}</span><span>{$horario}</span></div>
                    <div class="estado-tarjeta-actividad"><span>{$precio}</span><span class="{$claseCupo}">{$estadoCupo}</span></div>
                    <a class="enlace-actividad" href="actividad.php?id={$slug}">Ver actividad <span aria-hidden="true">→</span></a>
                </div>
            </article>
        HTML;
    }

    public static function accesoCuenta(?array $usuario, string $prefijo = ''): string
    {
        if ($usuario) {
            return '<a class="boton boton-chico boton-encabezado" href="' . self::escapar($prefijo . 'registro/cuenta.php') . '">Mi cuenta</a>';
        }
        return '<a class="boton boton-chico boton-encabezado" href="' . self::escapar($prefijo . 'registro/login.php') . '">Iniciar sesión</a>';
    }

    public static function botonModoEdicion(?array $usuario, string $destino = 'administrar-actividades.php', string $texto = 'Modo edición'): string
    {
        if (!Autorizacion::esCoordinador($usuario)) return '';
        return '<a class="boton boton-chico boton-modo-edicion" href="' . self::escapar($destino) . '">' . self::escapar($texto) . '</a>';
    }
}
