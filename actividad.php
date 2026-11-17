<?php
/**
 * ARCHIVO EXPLICADO: actividad.php
 *
 * Muestra el detalle de una actividad y procesa la inscripción del cliente.
 * Las funciones de este archivo están documentadas junto a su declaración.
 */

declare(strict_types=1);

$actividad = null;
$error = null;
$errorInscripcion = null;
$estaInscripto = false;
$titularParticipa = false;
$acompanantes = [];
$acompanantesInscriptos = [];
$fechaActividadSeleccionada = null;
$fechasDisponibles = [];
$puedeInscribirsePorEstadia = false;
try {
    require_once __DIR__ . '/php/inicio_actividades.php';
    $actividad = $controladorActividades->detalle((string) ($_GET['id'] ?? ''));
    $usuario = Sesion::usuario();
} catch (Throwable $excepcion) {
    error_log('No se pudo cargar el detalle de la actividad: ' . $excepcion->getMessage());
    http_response_code(404);
    $error = 'No encontramos la actividad solicitada.';
}
$usuario = class_exists('Sesion') ? Sesion::usuario() : null;

// Los errores del sistema de inscripciones no deben ocultar una actividad que
// existe. Si falta actualizar la base, se muestra el detalle y se informa el
// paso pendiente en lugar de responder incorrectamente con un error 404.
if ($actividad && $_SERVER['REQUEST_METHOD'] === 'POST') {
    try {
        if (!$controladorInscripciones) {
            throw new RuntimeException('Las inscripciones todavía no están habilitadas. Ejecutá BD/04_ACTUALIZAR_LOGICA_ACTIVIDADES.sql en phpMyAdmin.');
        }
        $mensajeInscripcion = $controladorInscripciones->procesar($_POST, $usuario, $actividad);
        Sesion::guardarMensaje('exito', $mensajeInscripcion);
        Utilidades::redirigir('actividad.php?id=' . rawurlencode($actividad->slug));
    } catch (Throwable $excepcion) {
        error_log('No se pudo procesar la inscripción: ' . $excepcion->getMessage());
        $errorInscripcion = $excepcion instanceof InvalidArgumentException || $excepcion instanceof RuntimeException
            ? $excepcion->getMessage()
            : 'No se pudo completar la inscripción. Revisá que hayas ejecutado BD/04_ACTUALIZAR_LOGICA_ACTIVIDADES.sql.';
    }
}

if ($actividad && isset($repositorioInscripciones) && $repositorioInscripciones && (int) ($usuario['id_cliente'] ?? 0) > 0) {
    try {
        $idCliente = (int) $usuario['id_cliente'];
        // La pantalla trabaja con un día puntual. Así no se mezclan los datos
        // de una misma actividad tomada en lunes, miércoles, etc.
        $fechaConsultada = trim((string) ($_POST['fecha_actividad'] ?? $_GET['fecha'] ?? ''));
        if ($fechaConsultada === '') $fechaConsultada = $repositorioInscripciones->fechaSeleccionada($actividad->id, $idCliente) ?? '';
        $estaInscripto = $fechaConsultada !== '' && $repositorioInscripciones->estaInscripto($actividad->id, $idCliente, $fechaConsultada);
        $titularParticipa = $estaInscripto
            ? $repositorioInscripciones->titularParticipa($actividad->id, $idCliente, $fechaConsultada)
            : false;
        $acompanantes = $repositorioInscripciones->listarAcompanantes($idCliente);
        $acompanantesInscriptos = $repositorioInscripciones->acompanantesInscriptos($actividad->id, $idCliente, $fechaConsultada ?: null);
        $fechaActividadSeleccionada = $fechaConsultada ?: null;
        $puedeInscribirsePorEstadia = $repositorioInscripciones->tieneEstadiaConfirmada($idCliente);
    } catch (Throwable $excepcion) {
        error_log('No se pudieron cargar los datos de inscripción de la actividad: ' . $excepcion->getMessage());
        $errorInscripcion = 'La actividad existe, pero falta actualizar la base para usar las inscripciones. Ejecutá BD/04_ACTUALIZAR_LOGICA_ACTIVIDADES.sql en phpMyAdmin.';
        $repositorioInscripciones = null;
        $controladorInscripciones = null;
    }
}
if ($actividad && isset($repositorioInscripciones) && $repositorioInscripciones && (int) ($usuario['id_cliente'] ?? 0) > 0) {
    try {
        $fechasDisponibles = $repositorioInscripciones->fechasDisponiblesParaCliente(
            $actividad,
            (int) $usuario['id_cliente']
        );
    } catch (Throwable) {
        $fechasDisponibles = [];
    }
}
if (!$fechaActividadSeleccionada && $fechasDisponibles) $fechaActividadSeleccionada = $fechasDisponibles[0]['valor'];
$titularMarcado = $estaInscripto ? $titularParticipa : true;
$cantidadSeleccionada = ($titularParticipa ? 1 : 0) + count($acompanantesInscriptos);
if ($errorInscripcion && $_SERVER['REQUEST_METHOD'] === 'POST') {
    $titularMarcado = isset($_POST['participa_titular']);
    $acompanantesInscriptos = is_array($_POST['acompanantes'] ?? null)
        ? array_map('intval', $_POST['acompanantes'])
        : [];
    $fechaActividadSeleccionada = trim((string) ($_POST['fecha_actividad'] ?? ''));
}
$mensaje = class_exists('Sesion') ? Sesion::tomarMensaje() : null;
$esCoordinador = class_exists('Autorizacion') && Autorizacion::esCoordinador($usuario);
?>
<!DOCTYPE html>
<html lang="es-AR">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><meta name="theme-color" content="#004D40"><link rel="icon" type="image/svg+xml" href="imgs/favicon-molino.svg">
    <link rel="stylesheet" href="estilos/estilos.css?v=<?= (int) filemtime(__DIR__ . '/estilos/estilos.css') ?>"><script src="script.js?v=<?= (int) filemtime(__DIR__ . '/script.js') ?>" defer></script><title><?= $actividad ? Vista::escapar($actividad->nombre) : 'Actividad' ?> | El Molino</title>
</head>
<body>
    <a class="enlace-saltar" href="#contenido">Saltar al contenido</a>
    <?= Vista::encabezadoPrincipal($usuario, '', 'actividades') ?>

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
                        <div><dt>Cupos</dt><dd><strong><?= $actividad->cupo ?></strong> lugares por fecha</dd></div>
                        <div><dt>Valor</dt><dd><?= Vista::escapar(Vista::precio($actividad)) ?></dd></div>
                    </dl>
                    <?php if ($usuario && (int) ($usuario['id_cliente'] ?? 0) > 0): ?>
                        <?php if (!$repositorioInscripciones): ?>
                            <div class="requisito-estadia-actividad">
                                <strong>Falta actualizar la base de datos</strong>
                                <p>La actividad se puede consultar, pero las inscripciones quedarán deshabilitadas hasta ejecutar <code>BD/04_ACTUALIZAR_LOGICA_ACTIVIDADES.sql</code> en phpMyAdmin.</p>
                            </div>
                        <?php else: ?>
                        <?php if ($estaInscripto): ?>
                            <div class="estado-inscripcion-activa"><strong>Inscripción activa</strong><span><?= $cantidadSeleccionada ?> <?= $cantidadSeleccionada === 1 ? 'lugar reservado' : 'lugares reservados' ?>.</span></div>
                        <?php endif; ?>
                        <?php if ($puedeInscribirsePorEstadia): ?>
                        <form method="post" class="formulario-inscripcion selector-participantes"<?= (!$estaInscripto && $actividad->modalidad === 'inscripcion' && $actividad->precio > 0) ? ' data-confirmar-cobro' : '' ?>>
                            <input type="hidden" name="token_csrf" value="<?= Vista::escapar(Sesion::tokenCsrf()) ?>">
                            <input type="hidden" name="accion" value="guardar_inscripcion">
                            <label class="campo-fecha-actividad" for="fecha-actividad">
                                <span>¿Qué día querés participar?</span>
                                <select id="fecha-actividad" name="fecha_actividad" required>
                                    <option value="">Elegí una fecha</option>
                                    <?php foreach ($fechasDisponibles as $fechaDisponible): ?>
                                        <option value="<?= Vista::escapar($fechaDisponible['valor']) ?>"<?= $fechaActividadSeleccionada === $fechaDisponible['valor'] ? ' selected' : '' ?>><?= Vista::escapar($fechaDisponible['etiqueta']) ?></option>
                                    <?php endforeach; ?>
                                </select>
                                <small>Solo aparecen días habilitados que coinciden con tu estadía confirmada.</small>
                            </label>
                            <fieldset>
                                <legend>¿Quiénes van a participar?</legend>
                                <p>Elegí una o más personas. Cada selección ocupa un lugar del cupo.</p>
                                <div class="lista-acompanantes">
                                    <label class="opcion-acompanante opcion-titular">
                                        <input type="checkbox" name="participa_titular" value="1"<?= $titularMarcado ? ' checked' : '' ?>>
                                        <span><strong><?= Vista::escapar((string) ($usuario['nombre'] ?? 'Titular')) ?> (yo)</strong><small>Titular de la cuenta</small></span>
                                    </label>
                                    <?php foreach ($acompanantes as $acompanante): ?>
                                        <label class="opcion-acompanante">
                                            <input type="checkbox" name="acompanantes[]" value="<?= $acompanante['id'] ?>"<?= in_array($acompanante['id'], $acompanantesInscriptos, true) ? ' checked' : '' ?>>
                                            <span><strong><?= Vista::escapar($acompanante['nombre'] . ' ' . $acompanante['apellido']) ?></strong><small><?= Vista::escapar($acompanante['parentesco']) ?> · <?= $acompanante['edad'] ?> años</small></span>
                                        </label>
                                    <?php endforeach; ?>
                                </div>
                            </fieldset>
                            <button class="boton boton-principal boton-ancho" type="submit"<?= $fechasDisponibles ? '' : ' disabled' ?>><?= $estaInscripto ? 'Guardar fecha y participantes' : 'Confirmar inscripción' ?></button>
                        </form>
                        <?php else: ?>
                            <div class="requisito-estadia-actividad">
                                <strong>Necesitás una estadía confirmada</strong>
                                <p>Primero aboná la seña de tu alojamiento. Cuando administración la acredite, vas a poder elegir actividades dentro de las fechas de esa estadía.</p>
                                <a href="registro/cuenta.php">Ver mi estadía y la seña</a>
                            </div>
                        <?php endif; ?>
                        <?php if ($estaInscripto): ?>
                            <form method="post" class="formulario-inscripcion cancelar-inscripcion" onsubmit="return confirm('¿Querés cancelar toda la inscripción?')">
                                <input type="hidden" name="token_csrf" value="<?= Vista::escapar(Sesion::tokenCsrf()) ?>">
                                <input type="hidden" name="accion" value="cancelar_inscripcion">
                                <input type="hidden" name="fecha_actividad" value="<?= Vista::escapar((string) $fechaActividadSeleccionada) ?>">
                                <button class="boton boton-neutral boton-ancho" type="submit">Cancelar inscripción</button>
                            </form>
                        <?php endif; ?>
                        <?php endif; ?>
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
    <?php if ($actividad && !$estaInscripto && $actividad->modalidad === 'inscripcion' && $actividad->precio > 0): ?>
        <dialog class="dialogo-cobro-actividad" id="aviso-cobro-actividad" aria-labelledby="titulo-aviso-cobro">
            <form method="dialog">
                <p class="texto-destacado">ACTIVIDAD PAGA</p>
                <h2 id="titulo-aviso-cobro">Antes de inscribirte</h2>
                <p>Esta actividad cuesta <strong><?= Vista::escapar(Vista::precio($actividad)) ?> por persona</strong>. El importe se sumará a tu cuenta y se cobrará al finalizar tu estadía.</p>
                <div class="acciones-dialogo"><button class="boton boton-neutral" value="cancel">Volver</button><button class="boton boton-principal" value="confirmar">Confirmar inscripción</button></div>
            </form>
        </dialog>
    <?php endif; ?>
    <footer class="pie-pagina pie-pagina-simple"><div class="contenedor pie-inferior"><span>El Molino · Complejo recreativo</span><?= Vista::contactoPie() ?><a href="actividades.php">Volver a actividades</a></div></footer>
</body>
</html>
