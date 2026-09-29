<?php
declare(strict_types=1);

require_once __DIR__ . '/inicio.php';

Sesion::iniciar();
$idPendiente = (int) Sesion::obtener('registro_pendiente_id', 0);
if ($idPendiente <= 0) {
    Utilidades::redirigir('registro.php');
}

$errores = [];
$mensaje = Sesion::tomarMensaje();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if (!Sesion::validarCsrf($_POST['token_csrf'] ?? null)) {
        $errores[] = 'La sesión del formulario venció. Volvé a intentarlo.';
    } else {
        $codigo = preg_replace('/\D+/', '', (string) ($_POST['codigo'] ?? ''));
        if (strlen($codigo) !== 6) {
            $errores[] = 'Ingresá los seis números del código.';
        } else {
            $resultado = (new Registro(Conexion::obtener(), new Correo()))->verificar($idPendiente, $codigo);
            $errores = $resultado['errores'];
            if ($resultado['usuario']) {
                Sesion::iniciarUsuario($resultado['usuario']);
                Sesion::eliminar('registro_pendiente_id');
                Sesion::guardarMensaje('exito', 'Tu correo fue verificado. ¡La cuenta ya está activa!');
                Utilidades::redirigir('cuenta.php');
            }
        }
    }
}
?>
<!DOCTYPE html>
<html lang="es-AR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Verificar correo | El Molino</title>
    <link rel="stylesheet" href="../estilos/estilos.css?v=<?= (int) filemtime(__DIR__ . '/../estilos/estilos.css') ?>">
</head>
<body class="pagina-registro">
    <main class="tarjeta-registro tarjeta-pasos">
        <p class="texto-destacado">PASO 3 DE 3</p>
        <h1>Verificá tu correo</h1>
        <p>Te enviamos un código de seis números. Tiene una vigencia de 15 minutos.</p>
        <ol class="pasos-registro" aria-label="Progreso del registro">
            <li class="paso-completo"><span>1</span> Tus datos</li>
            <li class="paso-completo"><span>2</span> Contraseña</li>
            <li class="paso-activo"><span>3</span> Verificación</li>
        </ol>
        <?php if ($errores): ?>
            <div class="mensaje mensaje-error" role="alert"><?= Utilidades::escapar(implode(' ', $errores)) ?></div>
        <?php endif; ?>
        <?php if ($mensaje): ?>
            <div class="mensaje mensaje-<?= Utilidades::escapar($mensaje['tipo']) ?>" role="status"><?= Utilidades::escapar($mensaje['texto']) ?></div>
        <?php endif; ?>
        <form method="post" class="formulario-registro formulario-codigo">
            <input type="hidden" name="token_csrf" value="<?= Utilidades::escapar(Sesion::tokenCsrf()) ?>">
            <label for="codigo">Código de verificación</label>
            <input id="codigo" name="codigo" inputmode="numeric" autocomplete="one-time-code" maxlength="6" pattern="[0-9]{6}" required autofocus>
            <button type="submit">Verificar y crear cuenta</button>
        </form>
        <form method="post" action="reenviar_codigo.php" class="formulario-reenvio">
            <input type="hidden" name="token_csrf" value="<?= Utilidades::escapar(Sesion::tokenCsrf()) ?>">
            <button class="boton-enlace" type="submit">Reenviar código</button>
        </form>
        <p class="enlace-inicio-sesion"><a href="registro.php">Cancelar el registro</a></p>
    </main>
</body>
</html>
git 
