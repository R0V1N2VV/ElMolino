<?php
/**
 * ARCHIVO EXPLICADO: registro/inicio.php
 *
 * Forma parte del flujo “inicio” del sitio El Molino.
 * Las funciones de este archivo están documentadas junto a su declaración.
 */

declare(strict_types=1);

require_once __DIR__ . '/../vendor/autoload.php';
require_once __DIR__ . '/../php/Conexion.php';
require_once __DIR__ . '/../php/Sesion.php';
require_once __DIR__ . '/../php/Utilidades.php';
require_once __DIR__ . '/../php/SeguridadPassword.php';
require_once __DIR__ . '/../php/Autenticacion.php';
require_once __DIR__ . '/../php/Autorizacion.php';
require_once __DIR__ . '/mailer.php';
require_once __DIR__ . '/gestor_registro.php';
