<?php
declare(strict_types=1);

require_once __DIR__ . '/registro/inicio.php';
Sesion::iniciar();
$usuario = Sesion::usuario();
Autorizacion::exigirCoordinador($usuario);

$error = null;
$mensaje = Sesion::tomarMensaje();
$categorias = [];
$actividades = [];
try {
    require_once __DIR__ . '/php/inicio_actividades.php';
    if ($modoCatalogoPredeterminado && $_SERVER['REQUEST_METHOD'] === 'POST') {
        throw new RuntimeException('La base de actividades todavía no está instalada. Ejecutá BD/actualizacion_actividades.sql para habilitar los cambios.');
    }
    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        $texto = $controladorActividades->procesarGestion($_POST, $_FILES, $usuario);
        Sesion::guardarMensaje('exito', $texto);
        Utilidades::redirigir('administrar-actividades.php');
    }
    ['categorias' => $categorias, 'actividades' => $actividades] = $controladorActividades->administracion();
    if ($modoCatalogoPredeterminado) {
        $error = 'Se está mostrando el catálogo predeterminado. Ejecutá BD/actualizacion_actividades.sql para poder agregar, editar o eliminar actividades.';
    }
} catch (Throwable $excepcion) {
    $error = $excepcion instanceof InvalidArgumentException || $excepcion instanceof RuntimeException
        ? $excepcion->getMessage()
        : 'No se pudo completar la operación en la base de datos.';
}

$editarId = (int) ($_GET['editar'] ?? 0);
$editando = null;
foreach ($actividades as $item) if ($item->id === $editarId) $editando = $item;
$mostrarFormulario = isset($_GET['nuevo']) || $editando !== null || $_SERVER['REQUEST_METHOD'] === 'POST';
?>
<!DOCTYPE html>
<html lang="es-AR">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><meta name="theme-color" content="#004D40"><link rel="stylesheet" href="estilos/estilos.css?v=<?= (int) filemtime(__DIR__ . '/estilos/estilos.css') ?>"><script src="script.js?v=<?= (int) filemtime(__DIR__ . '/script.js') ?>" defer></script><title>Coordinación de actividades | El Molino</title></head>
<body>
    <a class="enlace-saltar" href="#contenido">Saltar al contenido</a>
    <header class="encabezado"><div class="contenedor barra-navegacion"><a class="marca" href="index.php" aria-label="El Molino, inicio"><span class="simbolo-marca" aria-hidden="true">EM</span><span class="nombre-marca">El Molino<small>Coordinación de actividades</small></span></a><button class="boton-menu" type="button" aria-expanded="false" aria-controls="navegacion-principal"><span></span><span></span><span></span><span class="solo-lector">Abrir menú</span></button><nav class="navegacion-principal" id="navegacion-principal" aria-label="Navegación de administración"><a href="index.php">Ver sitio</a><a href="actividades.php">Ver actividades</a><a class="enlace-activo" href="administrar-actividades.php" aria-current="page">Gestionar</a><a href="registro/cuenta.php">Mi cuenta</a><button class="interruptor-dislexia" type="button" role="switch" aria-checked="false" aria-label="Activar modo dislexia con tipografía Sarakanda"><span>Modo dislexia</span><span class="pista-interruptor" aria-hidden="true"><span class="circulo-interruptor"></span></span></button></nav></div></header>

    <main id="contenido" class="pagina-administracion">
        <section class="cabecera-administracion"><div class="contenedor encabezado-admin"><div><p class="texto-destacado">Coordinador de actividades</p><h1>Gestión de actividades</h1><p>Creá, editá y organizá la información que se muestra en la página.</p></div><a class="boton boton-principal" href="administrar-actividades.php?nuevo=1#formulario">+ Agregar actividad</a></div></section>
        <?php if ($mensaje): ?><div class="aviso-administracion mensaje-<?= Vista::escapar($mensaje['tipo']) ?>" role="status"><?= Vista::escapar($mensaje['texto']) ?></div><?php endif; ?>
        <?php if ($error): ?><div class="aviso-administracion mensaje-error" role="alert"><?= Vista::escapar($error) ?></div><?php endif; ?>

        <?php if ($mostrarFormulario): ?>
        <section class="seccion panel-formulario-actividad" id="formulario"><div class="contenedor">
            <div class="encabezado-formulario-admin"><div><p class="texto-destacado">Formulario</p><h2><?= $editando ? 'Editar ' . Vista::escapar($editando->nombre) : 'Agregar actividad' ?></h2></div><a class="boton-cerrar-panel" href="administrar-actividades.php" aria-label="Cerrar formulario">×</a></div>
            <form class="formulario-actividad" method="post" enctype="multipart/form-data">
                <input type="hidden" name="token_csrf" value="<?= Vista::escapar(Sesion::tokenCsrf()) ?>"><input type="hidden" name="accion" value="guardar"><input type="hidden" name="id" value="<?= $editando?->id ?? 0 ?>"><input type="hidden" name="slug" value="<?= Vista::escapar($editando?->slug ?? '') ?>"><input type="hidden" name="imagen_actual" value="<?= Vista::escapar($editando?->imagen ?? '') ?>">
                <div class="bloque-formulario-admin"><h3>Información principal</h3><div class="grilla-campos-admin">
                    <label class="campo-doble">Nombre de la actividad<input name="nombre" required maxlength="80" value="<?= Vista::escapar($editando?->nombre ?? '') ?>"></label>
                    <label>Categoría<select name="categoria" required><?php foreach ($categorias as $categoria): ?><option value="<?= Vista::escapar($categoria->slug) ?>"<?= Vista::seleccionado($editando?->categoria ?? '', $categoria->slug) ?>><?= Vista::escapar($categoria->nombre) ?></option><?php endforeach; ?></select></label>
                    <label class="campo-doble">Descripción breve<input name="descripcionCorta" required maxlength="160" value="<?= Vista::escapar($editando?->descripcionCorta ?? '') ?>"></label>
                    <label class="campo-doble">Descripción completa<textarea name="descripcion" required rows="4"><?= Vista::escapar($editando?->descripcion ?? '') ?></textarea></label>
                    <label>Imagen<input name="imagen" type="file" accept="image/png,image/jpeg,image/webp"></label>
                    <div class="vista-imagen"><?php if ($editando?->imagen): ?><img src="<?= Vista::escapar($editando->imagen) ?>" alt="Vista previa"><?php else: ?><span>La imagen aparecerá acá</span><?php endif; ?></div>
                </div></div>
                <div class="bloque-formulario-admin"><h3>Horarios y ubicación</h3><div class="grilla-campos-admin">
                    <label>Días<input name="dias" required value="<?= Vista::escapar($editando?->dias ?? '') ?>"></label><label>Horario<input name="horario" required value="<?= Vista::escapar($editando?->horario ?? '') ?>"></label>
                    <label>Momento del día<select name="momento"><option value="manana"<?= Vista::seleccionado($editando?->momento ?? 'manana', 'manana') ?>>Mañana</option><option value="tarde"<?= Vista::seleccionado($editando?->momento ?? '', 'tarde') ?>>Tarde</option><option value="noche"<?= Vista::seleccionado($editando?->momento ?? '', 'noche') ?>>Noche</option></select></label>
                    <label>Duración<input name="duracion" required value="<?= Vista::escapar($editando?->duracion ?? '') ?>"></label><label>Sector<input name="sector" required value="<?= Vista::escapar($editando?->sector ?? '') ?>"></label><label>Responsable<input name="responsable" required value="<?= Vista::escapar($editando?->responsable ?? '') ?>"></label>
                </div></div>
                <div class="bloque-formulario-admin"><h3>Cupos y modalidad</h3><div class="grilla-campos-admin">
                    <label>Cupo máximo<input name="cupo" type="number" min="1" max="500" required value="<?= $editando?->cupo ?? 1 ?>"></label>
                    <label>Edad<select name="edad"><option value="todas"<?= Vista::seleccionado($editando?->edad ?? 'todas', 'todas') ?>>Todas las edades</option><option value="ninos"<?= Vista::seleccionado($editando?->edad ?? '', 'ninos') ?>>Niños</option><option value="adolescentes"<?= Vista::seleccionado($editando?->edad ?? '', 'adolescentes') ?>>Adolescentes</option><option value="adultos"<?= Vista::seleccionado($editando?->edad ?? '', 'adultos') ?>>Adultos</option></select></label>
                    <label>Modalidad<select name="modalidad"><option value="incluida"<?= Vista::seleccionado($editando?->modalidad ?? 'incluida', 'incluida') ?>>Incluida en la estadía</option><option value="inscripcion"<?= Vista::seleccionado($editando?->modalidad ?? '', 'inscripcion') ?>>Con inscripción</option></select></label>
                    <label>Precio<input name="precio" type="number" min="0" step="100" value="<?= Vista::escapar($editando?->precio ?? 0) ?>"></label>
                    <label class="campo-doble">¿Qué necesitan?<textarea name="requisitos" required rows="3"><?= Vista::escapar($editando?->requisitos ?? '') ?></textarea></label><label class="campo-doble">Información importante<textarea name="importante" required rows="3"><?= Vista::escapar($editando?->importante ?? '') ?></textarea></label>
                    <label class="opcion-tendencia campo-doble"><input name="tendencia" type="checkbox" value="1"<?= Vista::marcado($editando?->tendencia ?? false) ?>><span>Mostrar entre las actividades en tendencia</span></label>
                </div></div>
                <div class="acciones-formulario-admin"><button class="boton boton-principal" type="submit">Guardar actividad</button><a class="boton boton-neutral" href="administrar-actividades.php">Cancelar</a></div>
            </form>
        </div></section>
        <?php endif; ?>

        <section class="seccion listado-administracion"><div class="contenedor"><div class="encabezado-listado-admin"><div><p class="texto-destacado">Contenido publicado</p><h2>Actividades cargadas</h2></div></div><div class="lista-administracion">
            <?php foreach ($actividades as $actividad): ?><article class="fila-actividad-admin"><span class="miniatura-admin"><img src="<?= Vista::escapar($actividad->imagen) ?>" alt=""></span><span><strong><?= Vista::escapar($actividad->nombre) ?></strong><small><?= Vista::escapar($actividad->categoriaNombre) ?> · <?= Vista::escapar($actividad->dias) ?> · <?= Vista::escapar($actividad->horario) ?></small></span><span class="acciones-admin"><a href="administrar-actividades.php?editar=<?= $actividad->id ?>#formulario">Editar</a><form method="post" onsubmit="return confirm('¿Eliminar esta actividad?')"><input type="hidden" name="token_csrf" value="<?= Vista::escapar(Sesion::tokenCsrf()) ?>"><input type="hidden" name="accion" value="eliminar"><input type="hidden" name="id" value="<?= $actividad->id ?>"><button type="submit" class="boton-eliminar">Eliminar</button></form></span></article><?php endforeach; ?>
        </div><p class="nota-administracion"><strong>Acceso del coordinador de actividades.</strong> Los cambios se guardan en la base de datos y se muestran en la sección pública.</p></div></section>
    </main>
</body>
</html>
