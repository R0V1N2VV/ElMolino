<?php
/**
 * Panel privado para fijar el reloj compartido de una exposición.
 * No crea datos falsos: solamente define la fecha y hora que la app Java
 * utilizará para validar llegada, egreso y cobro final.
 */

declare(strict_types=1);

require_once __DIR__ . '/registro/inicio.php';
require_once __DIR__ . '/php/Vista.php';
require_once __DIR__ . '/php/RelojDemostracion.php';

Sesion::iniciar();
$usuario = Sesion::usuario();
Autorizacion::exigirSuperAdmin($usuario);

$conexion = Conexion::obtener();
$errores = [];
$mensaje = Sesion::tomarMensaje();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if (!Sesion::validarCsrf($_POST['token_csrf'] ?? null)) {
        $errores[] = 'La sesión del formulario venció. Volvé a intentarlo.';
    } else {
        try {
            $accion = (string) ($_POST['accion'] ?? '');
            if ($accion === 'activar') {
                RelojDemostracion::activar(
                    $conexion,
                    (string) ($_POST['fecha'] ?? ''),
                    (string) ($_POST['hora'] ?? ''),
                    (int) ($usuario['id_usuario'] ?? 0) ?: null
                );
                Sesion::guardarMensaje('exito', 'El reloj de demostración quedó actualizado. La app Java lo tomará al realizar la próxima acción.');
                Utilidades::redirigir('modo_demostracion.php');
            }
            if ($accion === 'desactivar') {
                RelojDemostracion::desactivar($conexion, (int) ($usuario['id_usuario'] ?? 0) ?: null);
                Sesion::guardarMensaje('exito', 'El modo demostración fue desactivado. El sistema vuelve a usar la fecha y hora reales.');
                Utilidades::redirigir('modo_demostracion.php');
            }
            throw new InvalidArgumentException('La acción solicitada no es válida.');
        } catch (InvalidArgumentException|PDOException $error) {
            if ($error instanceof PDOException) error_log('No se pudo guardar el reloj de demostración: ' . $error->getMessage());
            $errores[] = $error instanceof InvalidArgumentException
                ? $error->getMessage()
                : 'No pudimos guardar el reloj de demostración en este momento. Volvé a intentarlo.';
        }
    }
}

$estado = RelojDemostracion::estado($conexion);
$fechaSugerida = $estado['activo'] && $estado['fecha']
    ? $estado['fecha']
    : (new DateTimeImmutable('tomorrow', new DateTimeZone('America/Argentina/Buenos_Aires')))->format('Y-m-d');
$horaSugerida = $estado['activo'] && $estado['hora'] ? $estado['hora'] : '14:05';
?>
<!DOCTYPE html>
<html lang="es-AR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Modo demostración | El Molino</title>
    <link rel="stylesheet" href="estilos/estilos.css?v=<?= (int) filemtime(__DIR__ . '/estilos/estilos.css') ?>">
    <script src="script.js?v=<?= (int) filemtime(__DIR__ . '/script.js') ?>" defer></script>
</head>
<body class="pagina-demostracion">
    <?= Vista::encabezadoPrincipal($usuario, '', '') ?>
    <main class="contenedor contenido-demostracion">
        <a class="enlace-volver" href="registro/cuenta.php">← Volver a mi cuenta</a>
        <section class="cabecera-demostracion">
            <p class="texto-destacado">SOLO SUPERADMIN</p>
            <h1>Preparar demostración</h1>
            <p>Elegí el momento del recorrido. La web y la aplicación Java compartirán esta referencia para validar los horarios de ingreso y egreso.</p>
        </section>

        <?php if ($mensaje): ?><p class="aviso-demostracion aviso-demostracion-exito" role="status"><?= Vista::escapar($mensaje['texto']) ?></p><?php endif; ?>
        <?php if ($errores): ?><p class="aviso-demostracion aviso-demostracion-error" role="alert"><?= Vista::escapar(implode(' ', $errores)) ?></p><?php endif; ?>

        <?php if (!$estado['instalado']): ?>
            <section class="panel-demostracion panel-demostracion-alerta">
                <h2>El reloj de demostración no está disponible</h2>
                <p>Por ahora la web y la aplicación usan la fecha y la hora reales. Consultá con quien administra el sistema para habilitar este modo.</p>
            </section>
        <?php else: ?>
            <section class="panel-demostracion">
                <div class="estado-reloj-demostracion">
                    <div><span>Estado actual</span><strong><?= $estado['activo'] ? 'Modo demostración activo' : 'Usando fecha y hora reales' ?></strong></div>
                    <?php if ($estado['activo']): ?><p><?= Vista::escapar((string) $estado['fecha']) ?> · <?= Vista::escapar((string) $estado['hora']) ?></p><?php endif; ?>
                </div>
                <form method="post" class="formulario-demostracion">
                    <input type="hidden" name="token_csrf" value="<?= Vista::escapar(Sesion::tokenCsrf()) ?>">
                    <input type="hidden" name="accion" value="activar">
                    <label>Fecha de demostración<input type="date" name="fecha" value="<?= Vista::escapar($fechaSugerida) ?>" required></label>
                    <label>Hora de demostración<input type="time" name="hora" value="<?= Vista::escapar($horaSugerida) ?>" required></label>
                    <button class="boton boton-principal" type="submit">Usar este momento</button>
                </form>
                <?php if ($estado['activo']): ?>
                    <form method="post" class="acciones-demostracion" onsubmit="return confirm('¿Volver a usar la fecha y hora reales?')">
                        <input type="hidden" name="token_csrf" value="<?= Vista::escapar(Sesion::tokenCsrf()) ?>">
                        <input type="hidden" name="accion" value="desactivar">
                        <button class="boton boton-neutral" type="submit">Finalizar demostración</button>
                    </form>
                <?php endif; ?>
            </section>

            <section class="guia-demostracion" aria-label="Guía del recorrido">
                <article><span>1</span><div><h2>Reservá la estadía</h2><p>Desde la web creá una reserva para la fecha elegida y confirmá su seña desde Recepción.</p></div></article>
                <article><span>2</span><div><h2>Probá la llegada</h2><p>Elegí el día de ingreso y una hora desde las 14:00. En la app, Recepción podrá marcar la llegada.</p></div></article>
                <article><span>3</span><div><h2>Probá el egreso</h2><p>Elegí el día de salida y una hora desde las 10:00. Podrás cobrar el saldo, finalizar la estadía y enviarla a limpieza.</p></div></article>
            </section>
        <?php endif; ?>
    </main>
    <footer class="pie-pagina pie-pagina-simple"><div class="contenedor pie-inferior"><span>El Molino · Panel interno</span><?= Vista::contactoPie() ?></div></footer>
</body>
</html>
