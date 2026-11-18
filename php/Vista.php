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

    /** Devuelve los canales de contacto públicos que comparten los pies de página. */
    public static function contactoPie(): string
    {
        return '<div class="contacto-pie"><strong>Contacto</strong>'
            . '<p>Gmail: <a href="mailto:centrorecreativoelmolino403@gmail.com">centrorecreativoelmolino403@gmail.com</a></p>'
            . '<p>Teléfono: <a href="tel:+541178900880">11 7890 0880</a></p></div>';
    }

    /** Traduce estados internos de pedidos y pagos a frases para clientes. */
    public static function estadoPedidoCliente(?string $estado): string
    {
        return match (strtoupper(trim((string) $estado))) {
            'ESPERANDO_SENA', 'PENDIENTE_SENA' => 'Esperando la seña',
            'CONFIRMADO', 'CONFIRMADA' => 'Confirmada',
            'PAGADO' => 'Pago completo',
            'CANCELADO', 'CANCELADA' => 'Cancelada',
            'PARCIALMENTE_CANCELADO' => 'Modificada',
            'VENCIDO' => 'Vencida',
            'FINALIZADA' => 'Finalizada',
            default => self::estadoLegible($estado),
        };
    }

    /** Traduce estados internos del pago sin mostrar códigos de la base. */
    public static function estadoPagoCliente(?string $estado): string
    {
        return match (strtoupper(trim((string) $estado))) {
            'SENA_PENDIENTE', 'SENA_RESERVADA' => 'Esperando la seña',
            'SENA_INFORMADA' => 'Transferencia en revisión',
            'SENA_PAGADA' => 'Seña confirmada',
            'PAGADO' => 'Pago completo',
            'PAGO_RECHAZADO' => 'Transferencia no confirmada',
            'REEMBOLSO_PENDIENTE' => 'Reintegro en revisión',
            'CANCELADO', 'VENCIDO' => 'Reserva vencida o cancelada',
            default => self::estadoLegible($estado),
        };
    }

    /** Convierte un código eventual a texto humano sin guiones bajos ni mayúsculas técnicas. */
    private static function estadoLegible(?string $estado): string
    {
        $texto = trim(str_replace(['_', '-'], ' ', (string) $estado));
        return $texto === '' ? 'Sin información' : ucwords(strtolower($texto));
    }

    /** Construye el mismo encabezado principal en todas las secciones públicas. */
    public static function encabezadoPrincipal(?array $usuario, string $prefijo = '', string $seccion = ''): string
    {
        // En Inicio los enlaces son anclas locales. En las demás páginas deben
        // volver primero a index.php; de otro modo intentarían buscar, por
        // ejemplo, «#nosotros» dentro de una ficha de actividad.
        $esInicio = $seccion === 'inicio';
        $inicio = $prefijo === ''
            ? ($esInicio ? '#inicio' : 'index.php#inicio')
            : $prefijo . 'index.php#inicio';
        $ancla = static fn(string $nombre): string => $prefijo === ''
            ? ($esInicio ? '#' . $nombre : 'index.php#' . $nombre)
            : $prefijo . 'index.php#' . $nombre;
        $activo = static fn(string $nombre): string => $seccion === $nombre ? ' class="enlace-activo" aria-current="page"' : '';
        $carrito = class_exists('Sesion') ? Sesion::obtener('carrito_alojamiento', []) : [];
        $cantidad = is_array($carrito) ? count($carrito) : 0;

        return '<header class="encabezado"><div class="contenedor barra-navegacion">'
            . self::marca($prefijo, $prefijo === '' ? '#inicio' : 'index.php')
            . '<button class="boton-menu" type="button" aria-expanded="false" aria-controls="navegacion-principal"><span></span><span></span><span></span><span class="solo-lector">Abrir menú</span></button>'
            . '<nav class="navegacion-principal" id="navegacion-principal" aria-label="Navegación principal">'
            . '<a' . $activo('inicio') . ' href="' . self::escapar($inicio) . '">Inicio</a>'
            . '<a' . $activo('nosotros') . ' href="' . self::escapar($ancla('nosotros')) . '">Quiénes somos</a>'
            . '<a' . $activo('alojamientos') . ' href="' . self::escapar($ancla('alojamientos')) . '">Alojamientos</a>'
            . '<a' . $activo('actividades') . ' href="' . self::escapar($prefijo . 'actividades.php') . '">Actividades</a>'
            . '<a' . $activo('espacios') . ' href="' . self::escapar($ancla('espacios')) . '">Espacios</a>'
            . '<a class="enlace-estadia' . ($seccion === 'estadía' ? ' enlace-activo' : '') . '" href="' . self::escapar($prefijo . 'carrito.php') . '">Mi estadía <span>' . $cantidad . '</span></a>'
            . '</nav><div class="acciones-encabezado">' . self::accesoCuenta($usuario, $prefijo) . '</div></div></header>';
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
        $enlaceEstadia = $usuario
            ? '<a class="enlace-estadia-panel" href="' . self::escapar($prefijo . 'registro/estadia.php') . '">Mi estadía</a>'
            : '';
        $enlaceCoordinacion = $usuario && class_exists('Autorizacion') && Autorizacion::esCoordinador($usuario)
            ? '<a href="' . self::escapar($prefijo . 'administrar-actividades.php') . '">Gestionar actividades</a>'
            : '';
        $enlaceDemostracion = $usuario && class_exists('Autorizacion') && Autorizacion::esSuperAdmin($usuario)
            ? '<a href="' . self::escapar($prefijo . 'modo_demostracion.php') . '">Preparar demostración</a>'
            : '';
        $cerrarSesion = $usuario && class_exists('Sesion')
            ? '<form class="formulario-cerrar-sesion-panel" method="post" action="'
                . self::escapar($prefijo . 'registro/cerrar_sesion.php') . '">'
                . '<input type="hidden" name="token_csrf" value="' . self::escapar(Sesion::tokenCsrf()) . '">'
                . '<button type="submit">Cerrar sesión</button></form>'
            : '';

        return '<div class="menu-usuario">'
            . '<button class="boton-usuario" type="button" aria-expanded="false" aria-controls="panel-cuenta">'
            . '<span>' . self::escapar($texto) . '</span><span class="flecha-menu-usuario" aria-hidden="true"></span>'
            . '</button>'
            . '<div class="panel-usuario" id="panel-cuenta" hidden>'
            . '<a class="enlace-cuenta-panel" href="' . self::escapar($destino) . '">' . ($usuario ? 'Ver mi cuenta' : 'Iniciar sesión') . '</a>'
            . $enlaceEstadia
            . $enlaceCoordinacion
            . $enlaceDemostracion
            . '<div class="seccion-accesibilidad"><span>Opciones de accesibilidad</span>'
            . '<button class="interruptor-dislexia" type="button" role="switch" aria-checked="false" aria-label="Activar modo dislexia con tipografía Sarakanda">'
            . '<span>Modo dislexia</span><span class="pista-interruptor" aria-hidden="true"><span class="circulo-interruptor"></span></span>'
            . '</button>'
            . '<div class="control-idioma"><span>Idioma</span><div class="opciones-idioma" role="group" aria-label="Seleccionar idioma">'
            . '<button type="button" data-idioma="es" aria-pressed="true">ES</button>'
            . '<button type="button" data-idioma="en" aria-pressed="false">EN</button>'
            . '</div></div></div>'
            . $cerrarSesion
            . '</div></div>';
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
