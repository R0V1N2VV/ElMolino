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
$accionSolicitada = (string) ($_POST['accion'] ?? '');
try {
    require_once __DIR__ . '/php/inicio_actividades.php';
    if ($modoCatalogoPredeterminado && $_SERVER['REQUEST_METHOD'] === 'POST') {
        throw new RuntimeException('La base de actividades todavía no está instalada. Ejecutá BD/actualizacion_actividades.sql para habilitar los cambios.');
    }
    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        $resultadoGestion = $controladorActividades->procesarGestion($_POST, $_FILES, $usuario);
        Sesion::guardarMensaje('exito', (string) $resultadoGestion['mensaje']);
        $destino = match ((string) $resultadoGestion['accion']) {
            'guardar_categoria' => 'administrar-actividades.php#categorias-administracion',
            'guardar' => 'actividad.php?id=' . rawurlencode((string) $resultadoGestion['slug']),
            default => 'administrar-actividades.php',
        };
        Utilidades::redirigir($destino);
    }
    ['categorias' => $categorias, 'actividades' => $actividades] = $controladorActividades->administracion();
    if ($modoCatalogoPredeterminado) {
        $error = 'Se está mostrando el catálogo predeterminado. Ejecutá BD/actualizacion_actividades.sql para poder agregar, editar o eliminar actividades.';
    }
} catch (Throwable $excepcion) {
    $error = $excepcion instanceof InvalidArgumentException || $excepcion instanceof RuntimeException
        ? $excepcion->getMessage()
        : 'No se pudo completar la operación en la base de datos.';
    if (isset($controladorActividades)) {
        try {
            ['categorias' => $categorias, 'actividades' => $actividades] = $controladorActividades->administracion();
        } catch (Throwable) {
        }
    }
}

$editarId = (int) ($_GET['editar'] ?? 0);
$editando = null;
foreach ($actividades as $item) if ($item->id === $editarId) $editando = $item;
$mostrarFormularioCategoria = isset($_GET['nueva_categoria']) || $accionSolicitada === 'guardar_categoria';
$accionesSinFormulario = ['guardar_categoria', 'eliminar', 'eliminar_categoria'];
$mostrarFormulario = isset($_GET['nuevo']) || $editando !== null || ($accionSolicitada !== '' && !in_array($accionSolicitada, $accionesSinFormulario, true));
$categoriaPreseleccionada = trim((string) ($_GET['categoria'] ?? $_POST['categoria'] ?? ''));
$horasEncontradas = [];
if ($editando) preg_match_all('/(?:[01]\d|2[0-3]):[0-5]\d/', $editando->horario, $horasEncontradas);
$horaInicioBase = (string) ($horasEncontradas[0][0] ?? '09:00');
$horaFinBase = (string) ($horasEncontradas[0][1] ?? date('H:i', strtotime($horaInicioBase . ' +1 hour')));
$horaInicioFormulario = (string) ($_POST['hora_inicio'] ?? $horaInicioBase);
$horaFinFormulario = (string) ($_POST['hora_fin'] ?? $horaFinBase);
$opcionesHorario = [];
for ($minutos = 0; $minutos < 24 * 60; $minutos += 15) {
    $opcionesHorario[] = sprintf('%02d:%02d', intdiv($minutos, 60), $minutos % 60);
}
$valorFormulario = static fn (string $campo, string|int|float $actual = ''): string => (string) ($_POST[$campo] ?? $actual);
$categoriaFormulario = $categoriaPreseleccionada !== '' ? $categoriaPreseleccionada : (string) ($editando?->categoria ?? '');
$edadFormulario = (string) ($_POST['edad'] ?? ($editando?->edad ?? 'todas'));
$modalidadFormulario = (string) ($_POST['modalidad'] ?? ($editando?->modalidad ?? 'incluida'));
$tendenciaFormulario = $_SERVER['REQUEST_METHOD'] === 'POST' ? isset($_POST['tendencia']) : ($editando?->tendencia ?? false);
?>
<!DOCTYPE html>
<html lang="es-AR">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><meta name="theme-color" content="#004D40"><link rel="stylesheet" href="estilos/estilos.css?v=<?= (int) filemtime(__DIR__ . '/estilos/estilos.css') ?>"><script src="script.js?v=<?= (int) filemtime(__DIR__ . '/script.js') ?>" defer></script><title>Coordinación de actividades | El Molino</title></head>
<body>
    <a class="enlace-saltar" href="#contenido">Saltar al contenido</a>
    <header class="encabezado"><div class="contenedor barra-navegacion"><a class="marca" href="index.php" aria-label="El Molino, inicio"><span class="simbolo-marca" aria-hidden="true">EM</span><span class="nombre-marca">El Molino<small>Coordinación de actividades</small></span></a><button class="boton-menu" type="button" aria-expanded="false" aria-controls="navegacion-principal"><span></span><span></span><span></span><span class="solo-lector">Abrir menú</span></button><nav class="navegacion-principal" id="navegacion-principal" aria-label="Navegación de administración"><a href="index.php">Ver sitio</a><a href="actividades.php">Ver actividades</a><a class="enlace-activo" href="administrar-actividades.php" aria-current="page">Gestionar</a><a href="registro/cuenta.php">Mi cuenta</a><button class="interruptor-dislexia" type="button" role="switch" aria-checked="false" aria-label="Activar modo dislexia con tipografía Sarakanda"><span>Modo dislexia</span><span class="pista-interruptor" aria-hidden="true"><span class="circulo-interruptor"></span></span></button></nav></div></header>

    <main id="contenido" class="pagina-administracion">
        <section class="cabecera-administracion"><div class="contenedor encabezado-admin"><div><p class="texto-destacado">Coordinador de actividades</p><h1>Gestión de actividades</h1><p>Creá, editá y organizá la información que se muestra en la página.</p></div><?php if (!$mostrarFormularioCategoria && !$mostrarFormulario): ?><div class="acciones-cabecera-admin"><a class="boton boton-neutral" href="administrar-actividades.php?nueva_categoria=1#formulario-categoria">Agregar categoría</a><a class="boton boton-principal" href="administrar-actividades.php?nuevo=1#formulario">Agregar actividad</a></div><?php endif; ?></div></section>
        <?php if ($mensaje): ?><div class="aviso-administracion mensaje-<?= Vista::escapar($mensaje['tipo']) ?>" role="status"><strong>Listo.</strong><span><?= Vista::escapar($mensaje['texto']) ?></span></div><?php endif; ?>
        <?php if ($error): ?><div class="aviso-administracion mensaje-error" id="mensaje-gestion" role="alert"><strong>No se pudo completar.</strong><span><?= Vista::escapar($error) ?></span></div><?php endif; ?>

        <?php if ($mostrarFormularioCategoria): ?>
        <section class="seccion panel-formulario-actividad" id="formulario-categoria"><div class="contenedor">
            <div class="encabezado-formulario-admin"><div><p class="texto-destacado">Nueva categoría</p><h2>Agregar categoría</h2><p>Creá el grupo donde después se publicarán sus actividades.</p></div><a class="boton-cerrar-panel" href="administrar-actividades.php" aria-label="Cerrar formulario">×</a></div>
            <form class="formulario-actividad formulario-categoria" action="administrar-actividades.php#mensaje-gestion" method="post" enctype="multipart/form-data">
                <input type="hidden" name="token_csrf" value="<?= Vista::escapar(Sesion::tokenCsrf()) ?>">
                <input type="hidden" name="accion" value="guardar_categoria">
                <div class="bloque-formulario-admin"><h3>Información de la categoría</h3><div class="grilla-campos-admin">
                    <label>Nombre de la categoría<input name="nombre_categoria" required maxlength="60" value="<?= Vista::escapar((string) ($_POST['nombre_categoria'] ?? '')) ?>" placeholder="Por ejemplo: Deportes"></label>
                    <label>Imagen<input name="imagen_categoria" type="file" accept="image/png,image/jpeg,image/webp" required></label>
                    <label class="campo-doble">Descripción breve<input name="descripcion_categoria" required maxlength="160" value="<?= Vista::escapar((string) ($_POST['descripcion_categoria'] ?? '')) ?>" placeholder="Contá qué tipo de propuestas incluye"></label>
                </div></div>
                <div class="acciones-formulario-admin"><button class="boton boton-principal" type="submit">Guardar categoría</button><a class="boton boton-neutral" href="administrar-actividades.php">Cancelar</a></div>
            </form>
        </div></section>
        <?php endif; ?>

        <?php if ($mostrarFormulario): ?>
        <section class="seccion panel-formulario-actividad" id="formulario"><div class="contenedor">
            <div class="encabezado-formulario-admin"><div><p class="texto-destacado">Formulario</p><h2><?= $editando ? 'Editar ' . Vista::escapar($editando->nombre) : 'Agregar actividad' ?></h2></div><a class="boton-cerrar-panel" href="administrar-actividades.php" aria-label="Cerrar formulario">×</a></div>
            <form class="formulario-actividad formulario-actividad-redisenado" action="administrar-actividades.php#mensaje-gestion" method="post" enctype="multipart/form-data">
                <input type="hidden" name="token_csrf" value="<?= Vista::escapar(Sesion::tokenCsrf()) ?>">
                <input type="hidden" name="accion" value="guardar">
                <input type="hidden" name="id" value="<?= $editando?->id ?? 0 ?>">
                <input type="hidden" name="slug" value="<?= Vista::escapar($editando?->slug ?? '') ?>">
                <input type="hidden" name="imagen_actual" value="<?= Vista::escapar($editando?->imagen ?? '') ?>">

                <div class="bloque-formulario-admin">
                    <div class="titulo-bloque-formulario"><span>1</span><div><h3>Información principal</h3><p>Los datos que aparecerán en la tarjeta y en la página de la actividad.</p></div></div>
                    <div class="grilla-campos-admin">
                        <label class="campo-doble">Nombre de la actividad<input name="nombre" required maxlength="80" value="<?= Vista::escapar($valorFormulario('nombre', $editando?->nombre ?? '')) ?>" placeholder="Por ejemplo: Fútbol recreativo"></label>
                        <label>Categoría<select name="categoria" required><?php foreach ($categorias as $categoria): ?><option value="<?= Vista::escapar($categoria->slug) ?>"<?= Vista::seleccionado($categoriaFormulario, $categoria->slug) ?>><?= Vista::escapar($categoria->nombre) ?></option><?php endforeach; ?></select></label>
                        <label>Imagen de portada<input name="imagen" type="file" accept="image/png,image/jpeg,image/webp"><small>Es opcional. Si no cargás una, se usará la imagen de la categoría.</small></label>
                        <label class="campo-doble">Descripción breve<input name="descripcionCorta" required maxlength="160" value="<?= Vista::escapar($valorFormulario('descripcionCorta', $editando?->descripcionCorta ?? '')) ?>" placeholder="Una frase corta para la tarjeta"></label>
                        <label class="campo-doble">Descripción completa<textarea name="descripcion" required rows="4" placeholder="Explicá en qué consiste la propuesta"><?= Vista::escapar($valorFormulario('descripcion', $editando?->descripcion ?? '')) ?></textarea></label>
                        <?php if ($editando?->imagen): ?><div class="vista-imagen campo-doble"><img src="<?= Vista::escapar($editando->imagen) ?>" alt="Vista previa de la imagen actual"></div><?php endif; ?>
                    </div>
                </div>

                <div class="bloque-formulario-admin">
                    <div class="titulo-bloque-formulario"><span>2</span><div><h3>Horario y lugar</h3><p>Seleccioná una hora de inicio y otra de cierre. La duración se obtiene de ese rango.</p></div></div>
                    <div class="grilla-campos-admin">
                        <label class="campo-doble">Días<input name="dias" required value="<?= Vista::escapar($valorFormulario('dias', $editando?->dias ?? '')) ?>" placeholder="Por ejemplo: Lunes, miércoles y viernes"></label>
                        <div class="campo-doble grupo-horario-admin">
                            <label>Desde<select name="hora_inicio" required><?php foreach (array_slice($opcionesHorario, 0, -1) as $hora): ?><option value="<?= $hora ?>"<?= Vista::seleccionado($horaInicioFormulario, $hora) ?>><?= $hora ?></option><?php endforeach; ?></select></label>
                            <span>hasta</span>
                            <label>Hasta<select name="hora_fin" required><?php foreach ($opcionesHorario as $hora): ?><option value="<?= $hora ?>"<?= Vista::seleccionado($horaFinFormulario, $hora) ?>><?= $hora ?></option><?php endforeach; ?></select></label>
                        </div>
                        <label>Sector<input name="sector" required value="<?= Vista::escapar($valorFormulario('sector', $editando?->sector ?? '')) ?>" placeholder="Por ejemplo: Cancha norte"></label>
                        <label>Responsable<input name="responsable" required value="<?= Vista::escapar($valorFormulario('responsable', $editando?->responsable ?? '')) ?>" placeholder="Área o persona responsable"></label>
                    </div>
                </div>

                <div class="bloque-formulario-admin">
                    <div class="titulo-bloque-formulario"><span>3</span><div><h3>Participación</h3><p>Completá únicamente la información necesaria para organizar la actividad.</p></div></div>
                    <div class="grilla-campos-admin">
                        <label>Cupo máximo<input name="cupo" type="number" min="<?= max(1, $editando?->cantidadInscriptos ?? 0) ?>" max="500" required value="<?= Vista::escapar($valorFormulario('cupo', $editando?->cupo ?? 1)) ?>"><?php if ($editando && $editando->cantidadInscriptos > 0): ?><small>Hay <?= $editando->cantidadInscriptos ?> <?= $editando->cantidadInscriptos === 1 ? 'persona inscripta' : 'personas inscriptas' ?>. El cupo no puede quedar por debajo de esa cantidad.</small><?php endif; ?></label>
                        <label>Edad recomendada<select name="edad"><option value="todas"<?= Vista::seleccionado($edadFormulario, 'todas') ?>>Todas las edades</option><option value="ninos"<?= Vista::seleccionado($edadFormulario, 'ninos') ?>>Niños</option><option value="adolescentes"<?= Vista::seleccionado($edadFormulario, 'adolescentes') ?>>Adolescentes</option><option value="adultos"<?= Vista::seleccionado($edadFormulario, 'adultos') ?>>Adultos</option></select></label>
                        <label>¿La actividad es paga?<select name="modalidad"><option value="incluida"<?= Vista::seleccionado($modalidadFormulario, 'incluida') ?>>No, está incluida</option><option value="inscripcion"<?= Vista::seleccionado($modalidadFormulario, 'inscripcion') ?>>Sí, es paga</option></select></label>
                        <label data-campo-precio<?= $modalidadFormulario !== 'inscripcion' ? ' hidden' : '' ?>>Precio por persona<input name="precio" type="number" min="1" step="100" value="<?= Vista::escapar($valorFormulario('precio', $editando?->precio ?? 0)) ?>"<?= $modalidadFormulario === 'inscripcion' ? ' required' : '' ?>><small>Ingresá el importe sin puntos ni símbolos.</small></label>
                        <label class="opcion-tendencia campo-doble"><input name="tendencia" type="checkbox" value="1"<?= Vista::marcado($tendenciaFormulario) ?>><span>Mostrar esta actividad entre las tendencias</span></label>
                    </div>
                </div>

                <details class="bloque-formulario-admin bloque-opcional-admin"<?= ($valorFormulario('requisitos', $editando?->requisitos ?? '') !== '' || $valorFormulario('importante', $editando?->importante ?? '') !== '') ? ' open' : '' ?>>
                    <summary><span>Agregar detalles opcionales</span><small>Qué llevar e información importante</small></summary>
                    <div class="grilla-campos-admin">
                        <label class="campo-doble">Qué llevar<textarea name="requisitos" rows="3" placeholder="Ropa cómoda, botella de agua, materiales..."><?= Vista::escapar($valorFormulario('requisitos', $editando?->requisitos ?? '')) ?></textarea></label>
                        <label class="campo-doble">Información importante<textarea name="importante" rows="3" placeholder="Cambios por lluvia, acompañamiento de menores u otras aclaraciones"><?= Vista::escapar($valorFormulario('importante', $editando?->importante ?? '')) ?></textarea></label>
                    </div>
                </details>

                <div class="acciones-formulario-admin acciones-formulario-fijas"><button class="boton boton-principal" type="submit">Guardar y ver actividad</button><a class="boton boton-neutral" href="administrar-actividades.php">Cancelar</a></div>
            </form>
        </div></section>
        <?php endif; ?>

        <?php if (!$mostrarFormularioCategoria && !$mostrarFormulario): ?>
        <section class="seccion categorias-administracion" id="categorias-administracion"><div class="contenedor"><div class="encabezado-listado-admin"><div><p class="texto-destacado">Categorías</p><h2>Organizá las propuestas</h2><p>Entrá en una categoría o agregá directamente una actividad dentro de ella.</p></div><a class="boton boton-neutral" href="administrar-actividades.php?nueva_categoria=1#formulario-categoria">Agregar categoría</a></div><div class="grilla-categorias-admin">
            <?php foreach ($categorias as $categoria): ?><article class="tarjeta-categoria-admin"><img src="<?= Vista::escapar($categoria->imagen) ?>" alt=""><div><strong><?= Vista::escapar($categoria->nombre) ?></strong><small><?= Vista::escapar($categoria->descripcion) ?></small><span><?= $categoria->cantidadActividades ?> <?= $categoria->cantidadActividades === 1 ? 'actividad' : 'actividades' ?></span></div><div class="acciones-categoria-admin"><a href="categoria-actividades.php?categoria=<?= rawurlencode($categoria->slug) ?>">Ver categoría</a><a class="boton boton-principal" href="administrar-actividades.php?nuevo=1&amp;categoria=<?= rawurlencode($categoria->slug) ?>#formulario">Agregar actividad</a><form method="post" onsubmit="return confirm('¿Eliminar esta categoría? Esta acción no se puede deshacer desde el panel.')"><input type="hidden" name="token_csrf" value="<?= Vista::escapar(Sesion::tokenCsrf()) ?>"><input type="hidden" name="accion" value="eliminar_categoria"><input type="hidden" name="id_categoria" value="<?= $categoria->id ?>"><button type="submit" class="boton-eliminar-categoria"<?= $categoria->cantidadActividades > 0 ? ' disabled title="Primero eliminá o mové las actividades de esta categoría"' : '' ?>>Eliminar categoría</button></form></div></article><?php endforeach; ?>
        </div></div></section>

        <section class="seccion listado-administracion"><div class="contenedor"><div class="encabezado-listado-admin"><div><p class="texto-destacado">Contenido publicado</p><h2>Actividades cargadas</h2></div></div><div class="lista-administracion">
            <?php foreach ($actividades as $actividad): ?><article class="fila-actividad-admin"><span class="miniatura-admin"><img src="<?= Vista::escapar($actividad->imagen) ?>" alt=""></span><span><strong><?= Vista::escapar($actividad->nombre) ?></strong><small><?= Vista::escapar($actividad->categoriaNombre) ?> · <?= $actividad->cantidadInscriptos ?>/<?= $actividad->cupo ?> inscriptos · <?= Vista::escapar(Vista::precio($actividad)) ?></small></span><span class="acciones-admin"><a href="administrar-actividades.php?editar=<?= $actividad->id ?>#formulario">Editar</a><form method="post" onsubmit="return confirm('¿Eliminar esta actividad?')"><input type="hidden" name="token_csrf" value="<?= Vista::escapar(Sesion::tokenCsrf()) ?>"><input type="hidden" name="accion" value="eliminar"><input type="hidden" name="id" value="<?= $actividad->id ?>"><button type="submit" class="boton-eliminar">Eliminar</button></form></span></article><?php endforeach; ?>
        </div></div></section>
        <?php endif; ?>
    </main>
</body>
</html>
