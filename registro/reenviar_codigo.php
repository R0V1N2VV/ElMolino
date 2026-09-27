<?php
declare(strict_types=1);

require_once __DIR__ . '/inicio.php';

Sesion::iniciar();
if ($_SERVER['REQUEST_METHOD'] !== 'POST' || !Sesion::validarCsrf($_POST['token_csrf'] ?? null)) Utilidades::redirigir('registro.php');
$idPendiente = (int) Sesion::obtener('registro_pendiente_id', 0);
if ($idPendiente <= 0) {
    Utilidades::redirigir('registro.php');
}

try {
    $mensaje = (new Registro(Conexion::obtener(), new Correo()))->reenviarCodigo($idPendiente);
    Sesion::guardarMensaje('exito', $mensaje);
} catch (Throwable $error) {
    Sesion::guardarMensaje('error', $error->getMessage());
}
Utilidades::redirigir('verificar.php');
