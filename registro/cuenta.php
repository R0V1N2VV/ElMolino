<?php
declare(strict_types=1);

require_once __DIR__ . '/inicio.php';

Sesion::iniciar();
$usuario = Sesion::usuario();
if (!$usuario) {
    Sesion::guardarMensaje('error', 'Iniciá sesión para acceder a tu cuenta.');
    Utilidades::redirigir('login.php');
}
$mensaje = Sesion::tomarMensaje();
?>
<!DOCTYPE html>
<html lang="es-AR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Mi cuenta | El Molino</title>
    <link rel="stylesheet" href="../estilos/estilos.css?v=<?= (int) filemtime(__DIR__ . '/../estilos/estilos.css') ?>">
</head>
<body class="pagina-inicio-sesion">
    <main class="tarjeta-inicio-sesion">
        <div class="simbolo-marca" aria-hidden="true">EM</div>
        <p class="texto-destacado">MI CUENTA</p>
        <h1>Hola, <?= Utilidades::escapar($usuario['nombre']) ?></h1>
        <p class="subtitulo">Tu sesión está activa como <?= Utilidades::escapar($usuario['rol']) ?>.</p>
        <?php if ($mensaje): ?>
            <div class="mensaje mensaje-<?= Utilidades::escapar($mensaje['tipo']) ?>" role="status"><?= Utilidades::escapar($mensaje['texto']) ?></div>
        <?php endif; ?>
        <div class="datos-cuenta">
            <span>Correo</span>
            <strong><?= Utilidades::escapar($usuario['email']) ?></strong>
        </div>
        <?php if (Autorizacion::esCoordinador($usuario)): ?>
            <a class="boton boton-principal" href="../administrar-actividades.php">Gestionar actividades</a>
        <?php endif; ?>
        <a class="boton boton-principal" href="../index.php">Ir al complejo</a>
        <form method="post" action="cerrar_sesion.php" class="formulario-reenvio">
            <input type="hidden" name="token_csrf" value="<?= Utilidades::escapar(Sesion::tokenCsrf()) ?>">
            <button class="boton-enlace" type="submit">Cerrar sesión</button>
        </form>
    </main>
</body>
</html>
