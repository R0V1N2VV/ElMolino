<?php
declare(strict_types=1);

require_once __DIR__ . '/inicio.php';

Sesion::iniciar();
if ($_SERVER['REQUEST_METHOD'] === 'POST' && Sesion::validarCsrf($_POST['token_csrf'] ?? null)) {
    Sesion::cerrar();
    Sesion::iniciar();
    Sesion::guardarMensaje('exito', 'La sesión se cerró correctamente.');
}
Utilidades::redirigir('login.php');
