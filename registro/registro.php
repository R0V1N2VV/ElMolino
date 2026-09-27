<?php
declare(strict_types=1);

require_once __DIR__ . '/inicio.php';

Sesion::iniciar();
if (Sesion::usuario()) Utilidades::redirigir('cuenta.php');

$paso = (int) ($_GET['paso'] ?? 1);
$paso = in_array($paso, [1, 2], true) ? $paso : 1;
$errores = [];
$datos = Sesion::obtener('datos_registro', []);
$registro = new Registro(Conexion::obtener(), new Correo());

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if (!Sesion::validarCsrf($_POST['token_csrf'] ?? null)) {
        $errores[] = 'La sesión del formulario venció. Volvé a intentarlo.';
    } elseif (($_POST['accion'] ?? '') === 'guardar_datos') {
        try {
            $resultado = $registro->validarDatos($_POST);
            $datos = $resultado['datos'];
            $errores = $resultado['errores'];
            if (!$errores) {
                Sesion::guardar('datos_registro', $datos);
                Utilidades::redirigir('registro.php?paso=2');
            }
        } catch (PDOException) { $errores[] = 'No se pudo consultar la base de datos. Verificá la conexión.'; }
        $paso = 1;
    } elseif (($_POST['accion'] ?? '') === 'enviar_codigo') {
        if (!$datos) {
            Utilidades::redirigir('registro.php');
        }

        $resultado = $registro->crearPendiente($datos, (string) ($_POST['contrasena'] ?? ''), (string) ($_POST['repetir_contrasena'] ?? ''));
        $errores = $resultado['errores'];
        if (!$errores) {
            Sesion::guardar('registro_pendiente_id', $resultado['id']);
            Sesion::eliminar('datos_registro');
            Utilidades::redirigir('verificar.php');
        }
        $paso = 2;
    }
}
?>
<!DOCTYPE html>
<html lang="es-AR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Registro | El Molino</title>
    <link rel="stylesheet" href="../estilos/estilos.css">
</head>
<body class="pagina-registro">
    <main class="tarjeta-registro tarjeta-pasos">
        <a class="enlace-volver" href="../index.html">← Volver al inicio</a>
        <p class="texto-destacado">CENTRO RECREATIVO</p>
        <h1>Crear cuenta</h1>
        <p>Completá los datos para consultar reservas y actividades.</p>

        <ol class="pasos-registro" aria-label="Progreso del registro">
            <li class="<?= $paso === 1 ? 'paso-activo' : 'paso-completo' ?>"><span>1</span> Tus datos</li>
            <li class="<?= $paso === 2 ? 'paso-activo' : '' ?>"><span>2</span> Contraseña</li>
            <li><span>3</span> Verificación</li>
        </ol>

        <?php if ($errores): ?>
            <div class="mensaje mensaje-error" role="alert"><?= Utilidades::escapar(implode(' ', $errores)) ?></div>
        <?php endif; ?>

        <?php if ($paso === 1): ?>
            <form method="post" class="formulario-registro">
                <input type="hidden" name="token_csrf" value="<?= Utilidades::escapar(Sesion::tokenCsrf()) ?>">
                <input type="hidden" name="accion" value="guardar_datos">
                <div class="fila-formulario">
                    <div><label for="nombre">Nombre</label><input id="nombre" name="nombre" value="<?= Utilidades::escapar($datos['nombre'] ?? '') ?>" autocomplete="given-name" required></div>
                    <div><label for="apellido">Apellido</label><input id="apellido" name="apellido" value="<?= Utilidades::escapar($datos['apellido'] ?? '') ?>" autocomplete="family-name" required></div>
                </div>
                <label for="dni">DNI</label><input id="dni" name="dni" inputmode="numeric" value="<?= Utilidades::escapar($datos['dni'] ?? '') ?>" required>
                <label for="telefono">Teléfono</label><input id="telefono" name="telefono" type="tel" inputmode="tel" value="<?= Utilidades::escapar($datos['telefono'] ?? '') ?>" autocomplete="tel" required>
                <label for="email">Correo electrónico</label><input id="email" name="email" type="email" value="<?= Utilidades::escapar($datos['email'] ?? '') ?>" autocomplete="email" required>
                <label for="direccion">Domicilio</label><input id="direccion" name="direccion" value="<?= Utilidades::escapar($datos['direccion'] ?? '') ?>" autocomplete="street-address" required>
                <button type="submit">Continuar</button>
            </form>
        <?php else: ?>
            <form method="post" class="formulario-registro">
                <input type="hidden" name="token_csrf" value="<?= Utilidades::escapar(Sesion::tokenCsrf()) ?>">
                <input type="hidden" name="accion" value="enviar_codigo">
                <p class="resumen-registro">El código se enviará a <strong><?= Utilidades::escapar($datos['email'] ?? '') ?></strong>.</p>
                <label for="contrasena">Contraseña</label><input id="contrasena" name="contrasena" type="password" minlength="8" autocomplete="new-password" required>
                <label for="repetir_contrasena">Repetir contraseña</label><input id="repetir_contrasena" name="repetir_contrasena" type="password" minlength="8" autocomplete="new-password" required>
                <button type="submit">Enviar código de verificación</button>
                <a class="enlace-secundario" href="registro.php?paso=1">Editar mis datos</a>
            </form>
        <?php endif; ?>
        <p class="enlace-inicio-sesion">¿Ya tenés una cuenta? <a href="login.php">Iniciá sesión</a></p>
    </main>
</body>
</html>
