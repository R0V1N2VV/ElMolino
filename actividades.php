<?php
declare(strict_types=1);

$error = null;
$categorias = [];
$tendencias = [];
try {
    require_once __DIR__ . '/php/inicio_actividades.php';
    ['categorias' => $categorias, 'tendencias' => $tendencias] = $controladorActividades->portada();
} catch (Throwable $excepcion) {
    $error = 'No se pudieron cargar las actividades. Verificá la conexión y la base de datos.';
}
$usuario = class_exists('Sesion') ? Sesion::usuario() : null;
$esCoordinador = class_exists('Autorizacion') && Autorizacion::esCoordinador($usuario);
?>
<!DOCTYPE html>
<html lang="es-AR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="Conocé las actividades deportivas, recreativas y culturales de El Molino.">
    <meta name="theme-color" content="#004D40">
    <link rel="stylesheet" href="estilos/estilos.css?v=<?= (int) filemtime(__DIR__ . '/estilos/estilos.css') ?>">
    <script src="script.js?v=<?= (int) filemtime(__DIR__ . '/script.js') ?>" defer></script>
    <title>Actividades | El Molino</title>
</head>
<body>
    <a class="enlace-saltar" href="#contenido">Saltar al contenido</a>
    <header class="encabezado">
        <div class="contenedor barra-navegacion">
            <a class="marca" href="index.php" aria-label="El Molino, inicio"><span class="simbolo-marca" aria-hidden="true">EM</span><span class="nombre-marca">El Molino<small>Complejo recreativo</small></span></a>
            <button class="boton-menu" type="button" aria-expanded="false" aria-controls="navegacion-principal"><span></span><span></span><span></span><span class="solo-lector">Abrir menú</span></button>
            <nav class="navegacion-principal" id="navegacion-principal" aria-label="Navegación principal">
                <a href="index.php">Inicio</a><a href="index.php#nosotros">Quiénes somos</a><a href="index.php#alojamientos">Alojamientos</a><a class="enlace-activo" href="actividades.php" aria-current="page">Actividades</a><a href="index.php#espacios">Espacios</a>
                <button class="interruptor-dislexia" type="button" role="switch" aria-checked="false" aria-label="Activar modo dislexia con tipografía Sarakanda"><span>Modo dislexia</span><span class="pista-interruptor" aria-hidden="true"><span class="circulo-interruptor"></span></span></button>
            </nav>
            <?= class_exists('Vista') ? Vista::accesoCuenta($usuario) : '<a class="boton boton-chico boton-encabezado" href="registro/login.php">Iniciar sesión</a>' ?>
        </div>
    </header>

    <main id="contenido" class="pagina-actividades">
        <?php if ($error): ?><div class="aviso-administracion" role="alert"><?= htmlspecialchars($error, ENT_QUOTES, 'UTF-8') ?></div><?php endif; ?>
        <section class="portada-actividades">
            <div class="contenedor">
                <p class="texto-destacado">Actividades</p>
                <div class="titulo-portada-actividades"><div><h1>Elegí cómo querés disfrutar tu día.</h1><p>Deporte, naturaleza, talleres y propuestas para compartir durante tu estadía.</p></div><a class="boton boton-secundario" href="#categorias">Ver todas las categorías</a></div>
                <div class="carrusel-categorias" aria-label="Categorías destacadas">
                    <button class="control-carrusel" id="carrusel-anterior" type="button" aria-label="Ver categorías anteriores">←</button>
                    <div class="ventana-carrusel" id="pista-carrusel"><div class="pista-carrusel">
                        <?php foreach ($categorias as $categoria): ?>
                            <a class="item-carrusel-categoria" href="categoria-actividades.php?categoria=<?= rawurlencode($categoria->slug) ?>">
                                <img class="imagen-categoria" src="<?= Vista::escapar($categoria->imagen) ?>" alt="" loading="lazy">
                                <span class="contenido-categoria"><strong><?= Vista::escapar($categoria->nombre) ?></strong><small><?= Vista::escapar($categoria->descripcion) ?></small></span>
                            </a>
                        <?php endforeach; ?>
                    </div></div>
                    <button class="control-carrusel" id="carrusel-siguiente" type="button" aria-label="Ver categorías siguientes">→</button>
                </div>
            </div>
        </section>

        <section class="seccion seccion-tendencias" id="tendencias"><div class="contenedor">
            <div class="encabezado-seccion encabezado-con-accion"><div><p class="texto-destacado">Tendencias</p><h2>Los planes más elegidos.</h2><p>Una selección de las propuestas favoritas de quienes visitan El Molino.</p></div><a class="enlace-texto" href="#categorias">Elegir categoría <span aria-hidden="true">→</span></a></div>
            <div class="grilla-tendencias"><?php foreach ($tendencias as $actividad) echo Vista::tarjetaActividad($actividad, true); ?></div>
        </div></section>

        <section class="seccion seccion-categorias" id="categorias"><div class="contenedor">
            <div class="encabezado-seccion encabezado-con-accion"><div><p class="texto-destacado">Todas las categorías</p><h2>Encontrá una actividad para vos.</h2><p>Elegí una categoría para ver todas las propuestas, horarios y modalidades disponibles.</p></div><?php if ($esCoordinador): ?><a class="boton boton-principal" href="administrar-actividades.php?nueva_categoria=1#formulario-categoria">Agregar categoría</a><?php endif; ?></div>
            <div class="grilla-categorias">
                <?php foreach ($categorias as $categoria): ?>
                    <a class="tarjeta-categoria" href="categoria-actividades.php?categoria=<?= rawurlencode($categoria->slug) ?>">
                        <img class="imagen-categoria" src="<?= Vista::escapar($categoria->imagen) ?>" alt="" loading="lazy">
                        <span class="contenido-tarjeta-categoria"><strong><?= Vista::escapar($categoria->nombre) ?></strong><small><?= Vista::escapar($categoria->descripcion) ?></small><em><?= $categoria->cantidadActividades ?> <?= $categoria->cantidadActividades === 1 ? 'actividad' : 'actividades' ?></em></span>
                        <span class="flecha-categoria" aria-hidden="true">→</span>
                    </a>
                <?php endforeach; ?>
            </div>
        </div></section>
    </main>

    <footer class="pie-pagina"><div class="contenedor grilla-pie"><div><a class="marca marca-pie" href="index.php"><span class="simbolo-marca" aria-hidden="true">EM</span><span class="nombre-marca">El Molino<small>Complejo recreativo</small></span></a><p>Deporte, descanso y recreación en un mismo lugar.</p></div><nav aria-label="Enlaces del pie de página"><a href="index.php#nosotros">Quiénes somos</a><a href="index.php#alojamientos">Alojamientos</a><a href="actividades.php">Actividades</a><a href="index.php#espacios">Espacios</a><a href="registro/registro.php">Crear cuenta</a></nav></div><div class="contenedor pie-inferior"><span>El Molino · Complejo recreativo</span><span>Información sujeta a confirmación</span></div></footer>
    <script>
        document.addEventListener('DOMContentLoaded', () => {
            const pista = document.querySelector('#pista-carrusel');
            document.querySelector('#carrusel-anterior')?.addEventListener('click', () => pista.scrollBy({left: -340, behavior: 'smooth'}));
            document.querySelector('#carrusel-siguiente')?.addEventListener('click', () => pista.scrollBy({left: 340, behavior: 'smooth'}));
        });
    </script>
</body>
</html>
