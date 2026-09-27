<?php
declare(strict_types=1);

require_once __DIR__ . '/inicio.php';

Sesion::iniciar();
if (Sesion::usuario()) Utilidades::redirigir('cuenta.php');

$errores = [];
$correo = '';
$mensaje = Sesion::tomarMensaje();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $correo = strtolower(trim((string) ($_POST['correo'] ?? '')));
    $contrasena = (string) ($_POST['contrasena'] ?? '');

    if (!Sesion::validarCsrf($_POST['token_csrf'] ?? null)) {
        $errores[] = 'La sesión del formulario venció. Volvé a intentarlo.';
    } elseif (!filter_var($correo, FILTER_VALIDATE_EMAIL) || $contrasena === '') {
        $errores[] = 'Ingresá tu correo y contraseña.';
    } else {
        try {
            $usuario = (new Autenticacion(Conexion::obtener()))->iniciarSesion($correo, $contrasena);
            if (!$usuario) {
                $errores[] = 'El correo o la contraseña no son correctos.';
            } else {
                Sesion::iniciarUsuario($usuario);
                Utilidades::redirigir('cuenta.php');
            }
        } catch (PDOException $e) {
            $errores[] = 'No se pudo consultar la base de datos. Verificá la conexión.';
        }
    }
}
?>
<!DOCTYPE html>
<html lang="es-AR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Iniciar sesión | El Molino</title>
    <link rel="stylesheet" href="../estilos/estilos.css">
</head>
<body class="pagina-inicio-sesion">
    <main class="tarjeta-inicio-sesion">
        <a class="enlace-volver" href="../index.html">← Volver al inicio</a>
        <div class="simbolo-marca" aria-hidden="true">EM</div>
        <p class="texto-destacado">CENTRO RECREATIVO</p>
        <h1>Bienvenido</h1>
        <p class="subtitulo">Ingresá con tu cuenta para continuar.</p>
        <?php if ($mensaje): ?>
            <div class="mensaje mensaje-<?= Utilidades::escapar($mensaje['tipo']) ?>" role="status"><?= Utilidades::escapar($mensaje['texto']) ?></div>
        <?php endif; ?>
        <?php if ($errores): ?>
            <div class="mensaje mensaje-error" role="alert"><?= Utilidades::escapar(implode(' ', $errores)) ?></div>
        <?php endif; ?>
        <form class="formulario-inicio-sesion" method="post">
            <input type="hidden" name="token_csrf" value="<?= Utilidades::escapar(Sesion::tokenCsrf()) ?>">
            <label for="correo">Correo electrónico</label>
            <input id="correo" name="correo" type="email" value="<?= Utilidades::escapar($correo) ?>" autocomplete="email" required>
            <label for="contrasena">Contraseña</label>
            <input id="contrasena" name="contrasena" type="password" autocomplete="current-password" required>
            <button type="submit">Iniciar sesión</button>
        </form>
        <p class="texto-ayuda">¿No tenés cuenta? <a href="registro.php">Registrate</a></p>
    </main>
</body>
</html>
