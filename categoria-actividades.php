<?php
declare(strict_types=1);

$error = null;
$categoria = null;
$actividades = [];
$filtros = ['busqueda' => '', 'edad' => 'todas', 'momento' => 'todos', 'modalidad' => 'todas', 'orden' => 'recomendadas'];
try {
    require_once __DIR__ . '/php/inicio_actividades.php';
    ['categoria' => $categoria, 'filtros' => $filtros, 'actividades' => $actividades] = $controladorActividades->categoria($_GET);
} catch (Throwable $excepcion) {
    $error = 'No se pudo cargar esta categoría. Verificá la conexión y la base de datos.';
}
$usuario = class_exists('Sesion') ? Sesion::usuario() : null;
$mensaje = class_exists('Sesion') ? Sesion::tomarMensaje() : null;
$esCoordinador = class_exists('Autorizacion') && Autorizacion::esCoordinador($usuario);
$tituloCategoria = $categoria?->nombre ?? 'Actividades';
?>
<!DOCTYPE html>
<html lang="es-AR">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><meta name="description" content="Consultá las actividades de El Molino por categoría, horario y modalidad."><meta name="theme-color" content="#004D40">
    <link rel="stylesheet" href="estilos/estilos.css?v=<?= (int) filemtime(__DIR__ . '/estilos/estilos.css') ?>"><script src="script.js?v=<?= (int) filemtime(__DIR__ . '/script.js') ?>" defer></script><title><?= htmlspecialchars($tituloCategoria, ENT_QUOTES, 'UTF-8') ?> | Actividades de El Molino</title>
</head>
<body>
    <a class="enlace-saltar" href="#contenido">Saltar al contenido</a>
    <header class="encabezado"><div class="contenedor barra-navegacion"><a class="marca" href="index.php" aria-label="El Molino, inicio"><span class="simbolo-marca" aria-hidden="true">EM</span><span class="nombre-marca">El Molino<small>Complejo recreativo</small></span></a><button class="boton-menu" type="button" aria-expanded="false" aria-controls="navegacion-principal"><span></span><span></span><span></span><span class="solo-lector">Abrir menú</span></button><nav class="navegacion-principal" id="navegacion-principal" aria-label="Navegación principal"><a href="index.php">Inicio</a><a href="index.php#nosotros">Quiénes somos</a><a href="index.php#alojamientos">Alojamientos</a><a class="enlace-activo" href="actividades.php" aria-current="page">Actividades</a><a href="index.php#espacios">Espacios</a><button class="interruptor-dislexia" type="button" role="switch" aria-checked="false" aria-label="Activar modo dislexia con tipografía Sarakanda"><span>Modo dislexia</span><span class="pista-interruptor" aria-hidden="true"><span class="circulo-interruptor"></span></span></button></nav><?= class_exists('Vista') ? Vista::accesoCuenta($usuario) : '<a class="boton boton-chico boton-encabezado" href="registro/login.php">Iniciar sesión</a>' ?></div></header>

    <main id="contenido" class="pagina-categoria-actividades">
        <?php if ($mensaje): ?><div class="aviso-administracion mensaje-<?= Vista::escapar($mensaje['tipo']) ?>" role="status"><?= Vista::escapar($mensaje['texto']) ?></div><?php endif; ?>
        <?php if ($error): ?><div class="aviso-administracion" role="alert"><?= htmlspecialchars($error, ENT_QUOTES, 'UTF-8') ?></div><?php endif; ?>
        <?php if ($categoria): ?>
        <section class="cabecera-categoria-actividades"><div class="contenedor"><a class="volver-actividades" href="actividades.php#categorias">← Volver a todas las categorías</a><div class="titulo-categoria-actividades"><figure class="imagen-cabecera-categoria"><img src="<?= Vista::escapar($categoria->imagen) ?>" alt="Imagen de la categoría <?= Vista::escapar($categoria->nombre) ?>"></figure><div><p class="texto-destacado">Categoría</p><h1><?= Vista::escapar($categoria->nombre) ?></h1><p><?= Vista::escapar($categoria->descripcion) ?></p><?php if ($esCoordinador): ?><div class="acciones-categoria-coordinador"><?= Vista::botonModoEdicion($usuario) ?><a class="boton boton-principal" href="administrar-actividades.php?nuevo=1&amp;categoria=<?= rawurlencode($categoria->slug) ?>#formulario">Agregar actividad</a></div><?php endif; ?></div></div></div></section>

        <section class="seccion seccion-listado-actividades"><div class="contenedor">
            <form method="get" class="formulario-filtros-actividades">
                <input type="hidden" name="categoria" value="<?= Vista::escapar($categoria->slug) ?>">
                <div class="encabezado-listado"><div><p class="texto-destacado">Propuestas disponibles</p><h2>Elegí tu próxima actividad</h2><p aria-live="polite"><?= count($actividades) ?> <?= count($actividades) === 1 ? 'propuesta' : 'propuestas' ?></p></div><label class="campo-ordenar">Ordenar<select name="orden" onchange="this.form.submit()"><option value="recomendadas"<?= Vista::seleccionado($filtros['orden'], 'recomendadas') ?>>Recomendadas</option><option value="nombre"<?= Vista::seleccionado($filtros['orden'], 'nombre') ?>>Nombre</option><option value="horario"<?= Vista::seleccionado($filtros['orden'], 'horario') ?>>Horario</option></select></label></div>
                <div class="buscador-actividades"><label for="buscar-actividad">Buscar dentro de esta categoría</label><div class="campo-busqueda"><input id="buscar-actividad" name="buscar" type="search" value="<?= Vista::escapar($filtros['busqueda']) ?>" placeholder="Buscar por nombre o sector"><button class="boton boton-principal" type="submit">Buscar</button></div></div>
                <div class="estructura-listado-actividades">
                    <aside class="panel-filtros" aria-label="Filtros de actividades"><div class="titulo-filtros"><h3>Filtrar actividades</h3><a href="categoria-actividades.php?categoria=<?= rawurlencode($categoria->slug) ?>">Limpiar</a></div><label>Edad<select name="edad"><option value="todas"<?= Vista::seleccionado($filtros['edad'], 'todas') ?>>Todas las edades</option><option value="ninos"<?= Vista::seleccionado($filtros['edad'], 'ninos') ?>>Niños</option><option value="adolescentes"<?= Vista::seleccionado($filtros['edad'], 'adolescentes') ?>>Adolescentes</option><option value="adultos"<?= Vista::seleccionado($filtros['edad'], 'adultos') ?>>Adultos</option></select></label><label>Momento del día<select name="momento"><option value="todos"<?= Vista::seleccionado($filtros['momento'], 'todos') ?>>Cualquier horario</option><option value="manana"<?= Vista::seleccionado($filtros['momento'], 'manana') ?>>Mañana</option><option value="tarde"<?= Vista::seleccionado($filtros['momento'], 'tarde') ?>>Tarde</option><option value="noche"<?= Vista::seleccionado($filtros['momento'], 'noche') ?>>Noche</option></select></label><label>Modalidad<select name="modalidad"><option value="todas"<?= Vista::seleccionado($filtros['modalidad'], 'todas') ?>>Todas</option><option value="incluida"<?= Vista::seleccionado($filtros['modalidad'], 'incluida') ?>>Incluida en la estadía</option><option value="inscripcion"<?= Vista::seleccionado($filtros['modalidad'], 'inscripcion') ?>>Actividad paga</option></select></label><button class="boton boton-principal" type="submit">Aplicar filtros</button></aside>
                    <div class="resultados-actividades"><?php if ($actividades): foreach ($actividades as $actividad) echo Vista::tarjetaActividad($actividad); else: ?><div class="sin-resultados"><h3>No encontramos actividades</h3><p>Probá con otra búsqueda o limpiá los filtros.</p></div><?php endif; ?></div>
                </div>
            </form>
        </div></section>
        <?php endif; ?>
    </main>
    <footer class="pie-pagina"><div class="contenedor grilla-pie"><div><a class="marca marca-pie" href="index.php"><span class="simbolo-marca" aria-hidden="true">EM</span><span class="nombre-marca">El Molino<small>Complejo recreativo</small></span></a><p>Deporte, descanso y recreación en un mismo lugar.</p></div><nav aria-label="Enlaces del pie de página"><a href="index.php#nosotros">Quiénes somos</a><a href="index.php#alojamientos">Alojamientos</a><a href="actividades.php">Actividades</a><a href="index.php#espacios">Espacios</a><a href="registro/registro.php">Crear cuenta</a></nav></div><div class="contenedor pie-inferior"><span>El Molino · Complejo recreativo</span><span>Información sujeta a confirmación</span></div></footer>
</body>
</html>
