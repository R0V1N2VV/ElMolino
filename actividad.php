<?php
declare(strict_types=1);

$actividad = null;
$error = null;
try {
    require_once __DIR__ . '/php/inicio_actividades.php';
    $actividad = $controladorActividades->detalle((string) ($_GET['id'] ?? ''));
} catch (Throwable $excepcion) {
    http_response_code(404);
    $error = 'No encontramos la actividad solicitada.';
}
$usuario = class_exists('Sesion') ? Sesion::usuario() : null;
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
        <?php if ($error): ?>
            <div class="sin-resultados"><h1><?= htmlspecialchars($error, ENT_QUOTES, 'UTF-8') ?></h1><p><a href="actividades.php">Volver a actividades</a></p></div>
        <?php elseif ($actividad): ?>
            <a class="volver-actividades" href="categoria-actividades.php?categoria=<?= rawurlencode($actividad->categoria) ?>">← Volver a <?= Vista::escapar($actividad->categoriaNombre) ?></a>
            <div class="grilla-detalle-actividad">
                <div class="contenido-detalle-actividad">
                    <div class="visual-actividad visual-detalle-actividad"><img src="<?= Vista::escapar($actividad->imagen) ?>" alt="" loading="lazy"></div>
                    <p class="etiqueta-actividad"><?= Vista::escapar($actividad->categoriaNombre) ?></p><h1><?= Vista::escapar($actividad->nombre) ?></h1><p class="bajada-detalle"><?= Vista::escapar($actividad->descripcionCorta) ?></p>
                    <section class="bloque-texto-actividad"><h2>Un plan para disfrutar a tu ritmo</h2><p><?= Vista::escapar($actividad->descripcion) ?></p></section>
                    <section class="bloque-texto-actividad"><h2>¿Qué necesitás?</h2><p><?= Vista::escapar($actividad->requisitos) ?></p></section>
                    <section class="bloque-texto-actividad"><h2>Importante</h2><p><?= Vista::escapar($actividad->importante) ?></p></section>
                </div>
                <aside class="panel-informacion-actividad" aria-label="Información práctica"><p class="texto-destacado">Información práctica</p><dl><div><dt>Días y horarios</dt><dd><?= Vista::escapar($actividad->dias) ?> · <?= Vista::escapar($actividad->horario) ?></dd></div><div><dt>Sector</dt><dd><?= Vista::escapar($actividad->sector) ?></dd></div><div><dt>Duración</dt><dd><?= Vista::escapar($actividad->duracion) ?></dd></div><div><dt>Responsable</dt><dd><?= Vista::escapar($actividad->responsable) ?></dd></div><div><dt>Cupos</dt><dd>Hasta <?= $actividad->cupo ?> personas</dd></div><div><dt>Modalidad</dt><dd><?= Vista::escapar(Vista::precio($actividad)) ?></dd></div></dl><button class="boton boton-principal boton-ancho" type="button" disabled>Consultar lugar</button><p class="mensaje-inscripcion">La inscripción en línea se habilitará junto con el sistema de reservas.</p></aside>
            </div>
        <?php endif; ?>
    </div></main>
    <footer class="pie-pagina pie-pagina-simple"><div class="contenedor pie-inferior"><span>El Molino · Complejo recreativo</span><a href="actividades.php">Volver a actividades</a></div></footer>
</body>
</html>
