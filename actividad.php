<?php
declare(strict_types=1);

$actividad = null;
$error = null;
$errorInscripcion = null;
$estaInscripto = false;
try {
    require_once __DIR__ . '/php/inicio_actividades.php';
    $actividad = $controladorActividades->detalle((string) ($_GET['id'] ?? ''));
    $usuario = Sesion::usuario();

    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        if (!$controladorInscripciones) {
            throw new RuntimeException('Las inscripciones todavía no están habilitadas. Ejecutá la actualización de actividades en la base de datos.');
        }
        $mensajeInscripcion = $controladorInscripciones->procesar($_POST, $usuario, $actividad);
        Sesion::guardarMensaje('exito', $mensajeInscripcion);
        Utilidades::redirigir('actividad.php?id=' . rawurlencode($actividad->slug));
    }

    if ($repositorioInscripciones && (int) ($usuario['id_cliente'] ?? 0) > 0) {
        $estaInscripto = $repositorioInscripciones->estaInscripto($actividad->id, (int) $usuario['id_cliente']);
    }
} catch (Throwable $excepcion) {
    if ($_SERVER['REQUEST_METHOD'] === 'POST' && $actividad) {
        $errorInscripcion = $excepcion instanceof InvalidArgumentException || $excepcion instanceof RuntimeException
            ? $excepcion->getMessage()
            : 'No se pudo completar la inscripción. Volvé a intentarlo.';
    } else {
        http_response_code(404);
        $error = 'No encontramos la actividad solicitada.';
    }
}
$usuario = class_exists('Sesion') ? Sesion::usuario() : null;
if ($actividad && isset($repositorioInscripciones) && $repositorioInscripciones && (int) ($usuario['id_cliente'] ?? 0) > 0) {
    try {
        $estaInscripto = $repositorioInscripciones->estaInscripto($actividad->id, (int) $usuario['id_cliente']);
    } catch (Throwable) {
    }
}
$mensaje = class_exists('Sesion') ? Sesion::tomarMensaje() : null;
$esCoordinador = class_exists('Autorizacion') && Autorizacion::esCoordinador($usuario);
?>
<!DOCTYPE html>
<html lang="es-AR">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><meta name="theme-color" content="#004D40">
    <link rel="stylesheet" href="estilos/estilos.css?v=<?= (int) filemtime(__DIR__ . '/estilos/estilos.css') ?>"><script src="script.js?v=<?= (int) filemtime(__DIR__ . '/script.js') ?>" defer></script><title><?= $actividad ? Vista::escapar($actividad->nombre) : 'Actividad' ?> | El Molino</title>
</head>
<body>
    <a class="enlace-saltar" href="#contenido">Saltar al contenido</a>
    <header class="encabezado"><div class="contenedor barra-navegacion"><a class="marca" href="index.php" aria-label="El Molino, inicio"><span class="simbolo-marca" aria-hidden="true">EM</span><span class="nombre-marca">El Molino<small>Complejo recreativo</small></span></a><button class="boton-menu" type="button" aria-expanded="false" aria-controls="navegacion-principal"><span></span><span></span><span></span><span class="solo-lector">Abrir menú</span></button><nav class="navegacion-principal" id="navegacion-principal" aria-label="Navegación principal"><a href="index.php">Inicio</a><a href="index.php#nosotros">Quiénes somos</a><a href="index.php#alojamientos">Alojamientos</a><a class="enlace-activo" href="actividades.php">Actividades</a><a href="index.php#espacios">Espacios</a><button class="interruptor-dislexia" type="button" role="switch" aria-checked="false" aria-label="Activar modo dislexia con tipografía Sarakanda"><span>Modo dislexia</span><span class="pista-interruptor" aria-hidden="true"><span class="circulo-interruptor"></span></span></button></nav><?= class_exists('Vista') ? Vista::accesoCuenta($usuario) : '<a class="boton boton-chico boton-encabezado" href="registro/login.php">Iniciar sesión</a>' ?></div></header>

    <main id="contenido" class="pagina-detalle-actividad"><div class="contenedor">
        <?php if ($mensaje): ?><div class="aviso-administracion mensaje-<?= Vista::escapar($mensaje['tipo']) ?>" role="status"><strong>Listo.</strong><span><?= Vista::escapar($mensaje['texto']) ?></span></div><?php endif; ?>
        <?php if ($errorInscripcion): ?><div class="aviso-administracion mensaje-error" role="alert"><strong>No se pudo completar.</strong><span><?= Vista::escapar($errorInscripcion) ?></span></div><?php endif; ?>
        <?php if ($error): ?>
            <div class="sin-resultados"><h1><?= htmlspecialchars($error, ENT_QUOTES, 'UTF-8') ?></h1><p><a href="actividades.php">Volver a actividades</a></p></div>
        <?php elseif ($actividad): ?>
            <div class="barra-detalle-acciones"><a class="volver-actividades" href="categoria-actividades.php?categoria=<?= rawurlencode($actividad->categoria) ?>">← Volver a <?= Vista::escapar($actividad->categoriaNombre) ?></a><?= Vista::botonModoEdicion($usuario, 'administrar-actividades.php?editar=' . $actividad->id . '#formulario', 'Editar actividad') ?></div>
            <div class="grilla-detalle-actividad">
                <div class="contenido-detalle-actividad">
                    <div class="visual-actividad visual-detalle-actividad"><img src="<?= Vista::escapar($actividad->imagen) ?>" alt="" loading="lazy"></div>
                    <p class="etiqueta-actividad"><?= Vista::escapar($actividad->categoriaNombre) ?></p><h1><?= Vista::escapar($actividad->nombre) ?></h1><p class="bajada-detalle"><?= Vista::escapar($actividad->descripcionCorta) ?></p>
                    <section class="bloque-texto-actividad"><h2>Un plan para disfrutar a tu ritmo</h2><p><?= Vista::escapar($actividad->descripcion) ?></p></section>
                    <?php if ($actividad->requisitos !== ''): ?><section class="bloque-texto-actividad"><h2>¿Qué necesitás?</h2><p><?= Vista::escapar($actividad->requisitos) ?></p></section><?php endif; ?>
                    <?php if ($actividad->importante !== ''): ?><section class="bloque-texto-actividad"><h2>Importante</h2><p><?= Vista::escapar($actividad->importante) ?></p></section><?php endif; ?>
                </div>
                <aside class="panel-informacion-actividad" aria-label="Información práctica">
                    <p class="texto-destacado">Información práctica</p>
                    <dl>
                        <div><dt>Días y horarios</dt><dd><?= Vista::escapar($actividad->dias) ?> · <?= Vista::escapar($actividad->horario) ?></dd></div>
                        <div><dt>Sector</dt><dd><?= Vista::escapar($actividad->sector) ?></dd></div>
                        <div><dt>Responsable</dt><dd><?= Vista::escapar($actividad->responsable) ?></dd></div>
                        <div><dt>Cupos</dt><dd><strong><?= $actividad->cuposDisponibles() ?></strong> de <?= $actividad->cupo ?> lugares disponibles</dd></div>
                        <div><dt>Valor</dt><dd><?= Vista::escapar(Vista::precio($actividad)) ?></dd></div>
                    </dl>
                    <?php if ($actividad->estaCompleta() && !$estaInscripto): ?>
                        <button class="boton boton-neutral boton-ancho" type="button" disabled>Actividad completa</button>
                        <p class="mensaje-inscripcion">No quedan lugares disponibles.</p>
                    <?php elseif ($estaInscripto): ?>
                        <div class="estado-inscripcion-activa"><strong>Ya estás inscripto</strong><span>Tu lugar está reservado.</span></div>
                        <form method="post" class="formulario-inscripcion" onsubmit="return confirm('¿Querés cancelar tu inscripción?')">
                            <input type="hidden" name="token_csrf" value="<?= Vista::escapar(Sesion::tokenCsrf()) ?>">
                            <input type="hidden" name="accion" value="cancelar_inscripcion">
                            <button class="boton boton-neutral boton-ancho" type="submit">Cancelar inscripción</button>
                        </form>
                    <?php elseif ($usuario && (int) ($usuario['id_cliente'] ?? 0) > 0): ?>
                        <form method="post" class="formulario-inscripcion">
                            <input type="hidden" name="token_csrf" value="<?= Vista::escapar(Sesion::tokenCsrf()) ?>">
                            <input type="hidden" name="accion" value="inscribirse">
                            <button class="boton boton-principal boton-ancho" type="submit">Inscribirme</button>
                        </form>
                        <?php if ($actividad->modalidad === 'inscripcion'): ?><p class="mensaje-inscripcion">Actividad paga: <?= Vista::escapar(Vista::precio($actividad)) ?> por persona.</p><?php else: ?><p class="mensaje-inscripcion">Actividad incluida, sin costo adicional.</p><?php endif; ?>
                    <?php elseif ($usuario): ?>
                        <button class="boton boton-neutral boton-ancho" type="button" disabled>Inscripción no disponible</button>
                        <p class="mensaje-inscripcion">Esta cuenta no tiene un perfil de cliente asociado.</p>
                    <?php else: ?>
                        <a class="boton boton-principal boton-ancho" href="registro/login.php?actividad=<?= rawurlencode($actividad->slug) ?>">Iniciar sesión para inscribirme</a>
                        <p class="mensaje-inscripcion">Necesitás una cuenta para reservar tu lugar.</p>
                    <?php endif; ?>
                </aside>
            </div>
        <?php endif; ?>
    </div></main>
    <footer class="pie-pagina pie-pagina-simple"><div class="contenedor pie-inferior"><span>El Molino · Complejo recreativo</span><a href="actividades.php">Volver a actividades</a></div></footer>
</body>
</html>
