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

    public static function marca(string $prefijo = '', string $destino = 'index.php', bool $pie = false): string
    {
        $clase = $pie ? 'marca marca-logo marca-pie' : 'marca marca-logo';
        return '<a class="' . $clase . '" href="' . self::escapar($prefijo . $destino) . '" aria-label="El Molino, inicio">'
            . '<img class="logo-molino" src="' . self::escapar($prefijo . 'imgs/logo-el-molino.png') . '" alt="">'
            . '<span class="texto-marca-simple">El Molino</span>'
            . '</a>';
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
        $destino = $usuario ? $prefijo . 'registro/cuenta.php' : $prefijo . 'registro/login.php';
        $texto = $usuario ? 'Mi cuenta' : 'Ingresar';
        $enlaceCoordinacion = $usuario && class_exists('Autorizacion') && Autorizacion::esCoordinador($usuario)
            ? '<a href="' . self::escapar($prefijo . 'administrar-actividades.php') . '">Gestionar actividades</a>'
            : '';

        return '<div class="menu-usuario">'
            . '<button class="boton-usuario" type="button" aria-expanded="false" aria-controls="panel-cuenta">'
            . '<span>' . self::escapar($texto) . '</span><span class="flecha-menu-usuario" aria-hidden="true"></span>'
            . '</button>'
            . '<div class="panel-usuario" id="panel-cuenta" hidden>'
            . '<a class="enlace-cuenta-panel" href="' . self::escapar($destino) . '">' . ($usuario ? 'Ver mi cuenta' : 'Iniciar sesión') . '</a>'
            . $enlaceCoordinacion
            . '<div class="seccion-accesibilidad"><span>Opciones de accesibilidad</span>'
            . '<button class="interruptor-dislexia" type="button" role="switch" aria-checked="false" aria-label="Activar modo dislexia con tipografía Sarakanda">'
            . '<span>Modo dislexia</span><span class="pista-interruptor" aria-hidden="true"><span class="circulo-interruptor"></span></span>'
            . '</button></div></div></div>';
    }

    public static function botonModoEdicion(?array $usuario, string $destino = 'administrar-actividades.php', string $texto = 'Modo edición'): string
    {
        if (!Autorizacion::esCoordinador($usuario)) return '';
        return '<a class="boton boton-chico boton-modo-edicion" href="' . self::escapar($destino) . '">' . self::escapar($texto) . '</a>';
    }
}
