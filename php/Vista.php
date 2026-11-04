<?php
/**
 * ARCHIVO EXPLICADO: php/Vista.php
 *
 * Contiene ayudantes para generar HTML reutilizable de forma segura.
 * Las funciones de este archivo están documentadas junto a su declaración.
 */

declare(strict_types=1);

final class Vista
{
    /**
     * Inicializa el objeto y deja preparadas las dependencias que utilizarán sus demás métodos.
     */
    private function __construct() {}

    /**
     * Convierte caracteres especiales antes de imprimir texto en HTML y así evita inyectar código.
     *
     * @param mixed $valor Valor de valor que recibe la función.
     * @return string Resultado producido por esta operación.
     */
    public static function escapar(string|int|float|null $valor): string
    {
        return htmlspecialchars((string) $valor, ENT_QUOTES, 'UTF-8');
    }

    /**
     * Ejecuta la lógica de “precio” en Vista.php.
     *
     * @param mixed $actividad Valor de actividad que recibe la función.
     * @return string Resultado producido por esta operación.
     */
    public static function precio(Actividad $actividad): string
    {
        if ($actividad->modalidad === 'incluida' || $actividad->precio <= 0) return 'Incluida en la estadía';
        return '$' . number_format($actividad->precio, 0, ',', '.');
    }

    /**
     * Ejecuta la lógica de “seleccionado” en Vista.php.
     *
     * @param mixed $actual Valor de actual que recibe la función.
     * @param mixed $opcion Valor de opcion que recibe la función.
     * @return string Resultado producido por esta operación.
     */
    public static function seleccionado(string $actual, string $opcion): string
    {
        return $actual === $opcion ? ' selected' : '';
    }

    /**
     * Ejecuta la lógica de “marcado” en Vista.php.
     *
     * @param mixed $valor Valor de valor que recibe la función.
     * @return string Resultado producido por esta operación.
     */
    public static function marcado(bool $valor): string
    {
        return $valor ? ' checked' : '';
    }

    /**
     * Ejecuta la lógica de “marca” en Vista.php.
     *
     * @param mixed $prefijo Valor de prefijo que recibe la función.
     * @param mixed $destino Valor de destino que recibe la función.
     * @param mixed $pie Valor de pie que recibe la función.
     * @return string Resultado producido por esta operación.
     */
    public static function marca(string $prefijo = '', string $destino = 'index.php', bool $pie = false): string
    {
        $clase = $pie ? 'marca marca-logo marca-pie' : 'marca marca-logo';
        return '<a class="' . $clase . '" href="' . self::escapar($prefijo . $destino) . '" aria-label="El Molino, inicio">'
            . '<img class="logo-molino" src="' . self::escapar($prefijo . 'imgs/logo-el-molino.png') . '" alt="">'
            . '<span class="texto-marca-simple">El Molino</span>'
            . '</a>';
    }

    /**
     * Ejecuta la lógica de “tarjeta actividad” en Vista.php.
     *
     * @param mixed $actividad Valor de actividad que recibe la función.
     * @param mixed $destacada Valor de destacada que recibe la función.
     * @return string Resultado producido por esta operación.
     */
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
        $estadoCupo = self::escapar('Cupo de ' . $actividad->cupo . ' por fecha');

        return <<<HTML
            <article class="tarjeta-actividad{$clase}">
                <div class="visual-actividad"><img src="{$imagen}" alt="" loading="lazy"></div>
                <div class="contenido-tarjeta-actividad">
                    <p class="etiqueta-actividad">{$categoria}</p>
                    <h3>{$nombre}</h3>
                    <p>{$descripcion}</p>
                    <div class="datos-breves-actividad"><span>{$dias}</span><span>{$horario}</span></div>
                    <div class="estado-tarjeta-actividad"><span>{$precio}</span><span>{$estadoCupo}</span></div>
                    <a class="enlace-actividad" href="actividad.php?id={$slug}">Ver actividad <span aria-hidden="true">→</span></a>
                </div>
            </article>
        HTML;
    }

    /**
     * Ejecuta la lógica de “acceso cuenta” en Vista.php.
     *
     * @param mixed $usuario Valor de usuario que recibe la función.
     * @param mixed $prefijo Valor de prefijo que recibe la función.
     * @return string Resultado producido por esta operación.
     */
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
            . '</button>'
            . '<div class="control-idioma"><span>Idioma</span><div class="opciones-idioma" role="group" aria-label="Seleccionar idioma">'
            . '<button type="button" data-idioma="es" aria-pressed="true">ES</button>'
            . '<button type="button" data-idioma="en" aria-pressed="false">EN</button>'
            . '</div></div></div></div></div>';
    }

    /**
     * Ejecuta la lógica de “boton modo edicion” en Vista.php.
     *
     * @param mixed $usuario Valor de usuario que recibe la función.
     * @param mixed $destino Valor de destino que recibe la función.
     * @param mixed $texto Valor de texto que recibe la función.
     * @return string Resultado producido por esta operación.
     */
    public static function botonModoEdicion(?array $usuario, string $destino = 'administrar-actividades.php', string $texto = 'Modo edición'): string
    {
        if (!Autorizacion::esCoordinador($usuario)) return '';
        return '<a class="boton boton-chico boton-modo-edicion" href="' . self::escapar($destino) . '">' . self::escapar($texto) . '</a>';
    }
}
