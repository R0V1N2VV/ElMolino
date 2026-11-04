<?php
/**
 * ARCHIVO EXPLICADO: registro/cuenta.php
 *
 * Forma parte del flujo “cuenta” del sitio El Molino.
 * Las funciones de este archivo están documentadas junto a su declaración.
 */

declare(strict_types=1);

require_once __DIR__ . '/inicio.php';
require_once __DIR__ . '/../php/Vista.php';
require_once __DIR__ . '/../php/GestorReservasCliente.php';

Sesion::iniciar();
$usuario = Sesion::usuario();
if (!$usuario) {
    Sesion::guardarMensaje('error', 'Iniciá sesión para acceder a tu cuenta.');
    Utilidades::redirigir('login.php');
}

$mensaje = Sesion::tomarMensaje();
$acompanantes = [];
$actividades = [];
$alojamientos = [];
$errores = [];
$idCliente = (int) ($usuario['id_cliente'] ?? 0);
// Esta función anónima procesa cada elemento recibido sin crear un método público adicional.
$fechaPerfil = static function (?string $fecha): string {
    $valor = DateTimeImmutable::createFromFormat('!Y-m-d', (string) $fecha);
    return $valor ? $valor->format('d/m/Y') : (string) $fecha;
};

if ($idCliente > 0) {
    $conexion = Conexion::obtener();
    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        if (!Sesion::validarCsrf($_POST['token_csrf'] ?? null)) {
            $errores[] = 'La sesión del formulario venció. Volvé a intentarlo.';
        } else {
            try {
                $gestorReservas = new GestorReservasCliente($conexion);
                $idReserva = (int) ($_POST['id_reserva'] ?? 0);
                $accion = (string) ($_POST['accion'] ?? '');
                if ($accion === 'actualizar_acompanantes_reserva') {
                    $seleccionados = is_array($_POST['acompanantes'] ?? null) ? $_POST['acompanantes'] : [];
                    $gestorReservas->actualizarAcompanantes($idReserva, $idCliente, $seleccionados);
                    Sesion::guardarMensaje('exito', 'Actualizamos los acompañantes de la reserva.');
                    Utilidades::redirigir('cuenta.php');
                }
                if ($accion === 'cancelar_reserva') {
                    $gestorReservas->cancelar($idReserva, $idCliente);
                    Sesion::guardarMensaje('exito', 'La reserva fue cancelada. Si había pagos, administración gestionará el reintegro.');
                    Utilidades::redirigir('cuenta.php');
                }
                throw new RuntimeException('La acción solicitada no es válida.');
            } catch (RuntimeException $error) {
                $errores[] = $error->getMessage();
            } catch (PDOException $error) {
                error_log('No se pudo modificar la reserva: ' . $error->getMessage());
                $errores[] = 'No se pudo modificar la reserva. Ejecutá la actualización SQL de reservas y pagos.';
            }
        }
    }
    try {
        $consultaAcompanantes = $conexion->prepare(
            'SELECT idAcompañante AS id, nombre, apellido, dni, edad, parentesco
               FROM `acompañante`
              WHERE idCliente = :cliente
              ORDER BY nombre, apellido'
        );
        $consultaAcompanantes->execute(['cliente' => $idCliente]);
        $acompanantes = $consultaAcompanantes->fetchAll();
    } catch (PDOException $error) {
        error_log('No se pudieron cargar los acompañantes de la cuenta: ' . $error->getMessage());
    }

    try {
        $consultaActividades = $conexion->prepare(
            'SELECT ia.idActividad, ia.participaTitular, ia.fechaInscripcion, ia.fechaActividad,
                    a.nombre, a.slug, a.dias, a.horario
               FROM inscripcionActividad ia
               JOIN Actividad a ON a.idActividad = ia.idActividad
              WHERE ia.idCliente = :cliente
              ORDER BY a.nombre'
        );
        $consultaActividades->execute(['cliente' => $idCliente]);
        $actividades = $consultaActividades->fetchAll();

        $consultaParticipantes = $conexion->prepare(
            'SELECT iaa.idActividad, ac.nombre, ac.apellido
               FROM inscripcionActividadAcompanante iaa
               JOIN `acompañante` ac ON ac.idAcompañante = iaa.idAcompañante
              WHERE iaa.idClienteTitular = :cliente
              ORDER BY ac.nombre, ac.apellido'
        );
        $consultaParticipantes->execute(['cliente' => $idCliente]);
        $acompanantesPorActividad = [];
        foreach ($consultaParticipantes->fetchAll() as $participante) {
            $acompanantesPorActividad[(int) $participante['idActividad']][] = $participante['nombre'] . ' ' . $participante['apellido'];
        }
        foreach ($actividades as &$actividad) {
            $actividad['acompanantes'] = $acompanantesPorActividad[(int) $actividad['idActividad']] ?? [];
        }
        unset($actividad);
    } catch (PDOException $error) {
        error_log('No se pudieron cargar las actividades de la cuenta: ' . $error->getMessage());
        $actividades = [];
    }

    try {
        $consultaAlojamientos = $conexion->prepare(
            "SELECT r.idReserva, r.fechaIngreso, r.fechaEgreso, r.cantidadPersonas,
                    r.estadoReserva, r.precioTotal, a.tipo, a.capacidad,
                    p.idPedido, p.estadoPago, p.montoSena, p.fechaLimiteSena
               FROM reservas r
               JOIN alojamientos a ON a.idAlojamiento = r.idAlojamiento
          LEFT JOIN detalle_pedido_alojamiento d ON d.idReserva = r.idReserva
          LEFT JOIN pedido_alojamiento p ON p.idPedido = d.idPedido
              WHERE r.idCliente = :cliente
                AND UPPER(TRIM(r.estadoReserva)) NOT IN ('CANCELADA', 'CANCELADO')
              ORDER BY (r.fechaEgreso >= CURRENT_DATE) DESC, r.fechaIngreso ASC
              LIMIT 6"
        );
        $consultaAlojamientos->execute(['cliente' => $idCliente]);
        $alojamientos = $consultaAlojamientos->fetchAll();
        $consultaHuespedes = $conexion->prepare(
            'SELECT ra.idReserva, ra.idAcompañante, ac.nombre, ac.apellido
               FROM `reservaAcompañante` ra
               JOIN `acompañante` ac ON ac.idAcompañante = ra.idAcompañante
               JOIN reservas r ON r.idReserva = ra.idReserva
              WHERE r.idCliente = :cliente'
        );
        $consultaHuespedes->execute(['cliente' => $idCliente]);
        $huespedesPorReserva = [];
        foreach ($consultaHuespedes->fetchAll() as $huesped) {
            $huespedesPorReserva[(int) $huesped['idReserva']][] = [
                'id' => (int) $huesped['idAcompañante'],
                'nombre' => $huesped['nombre'] . ' ' . $huesped['apellido'],
            ];
        }
        foreach ($alojamientos as &$alojamiento) {
            $alojamiento['acompanantes'] = $huespedesPorReserva[(int) $alojamiento['idReserva']] ?? [];
        }
        unset($alojamiento);
    } catch (PDOException $error) {
        error_log('No se pudieron cargar los alojamientos de la cuenta: ' . $error->getMessage());
    }
}
?>
<!DOCTYPE html>
<html lang="es-AR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="icon" type="image/svg+xml" href="../imgs/favicon-molino.svg">
    <title>Mi cuenta | El Molino</title>
    <link rel="stylesheet" href="../estilos/estilos.css?v=<?= (int) filemtime(__DIR__ . '/../estilos/estilos.css') ?>">
    <script src="../script.js?v=<?= (int) filemtime(__DIR__ . '/../script.js') ?>" defer></script>
</head>
<body class="pagina-cuenta">
    <a class="enlace-saltar" href="#contenido">Saltar al contenido</a>
    <header class="encabezado">
        <div class="contenedor barra-navegacion">
            <?= Vista::marca('../') ?>
            <button class="boton-menu" type="button" aria-expanded="false" aria-controls="navegacion-principal"><span></span><span></span><span></span><span class="solo-lector">Abrir menú</span></button>
            <nav class="navegacion-principal" id="navegacion-principal" aria-label="Navegación principal">
                <a href="../index.php">Inicio</a>
                <a href="../alojamientos.php">Alojamientos</a>
                <a href="../actividades.php">Actividades</a>
            </nav>
            <div class="acciones-encabezado"><?= Vista::accesoCuenta($usuario, '../') ?></div>
        </div>
    </header>

    <main class="contenedor contenido-cuenta" id="contenido">
        <section class="cabecera-cuenta">
            <div><p class="texto-destacado">MI CUENTA</p><h1>Hola, <?= Utilidades::escapar($usuario['nombre']) ?></h1><p><?= Utilidades::escapar($usuario['email']) ?></p></div>
            <form method="post" action="cerrar_sesion.php" class="formulario-salir-cuenta">
                <input type="hidden" name="token_csrf" value="<?= Utilidades::escapar(Sesion::tokenCsrf()) ?>">
                <button class="boton-enlace" type="submit">Cerrar sesión</button>
            </form>
        </section>

        <?php if ($mensaje): ?><p class="mensaje mensaje-<?= Utilidades::escapar($mensaje['tipo']) ?>" role="status"><?= Utilidades::escapar($mensaje['texto']) ?></p><?php endif; ?>
        <?php if ($errores): ?><p class="mensaje mensaje-error" role="alert"><?= Utilidades::escapar(implode(' ', $errores)) ?></p><?php endif; ?>

        <div class="grilla-resumen-cuenta">
            <section class="panel-resumen-cuenta" aria-labelledby="titulo-actividades-cuenta">
                <div class="encabezado-panel-cuenta"><div><p class="texto-destacado">ACTIVIDADES</p><h2 id="titulo-actividades-cuenta">Tus inscripciones</h2></div><span><?= count($actividades) ?></span></div>
                <?php if ($actividades): ?>
                    <div class="lista-resumen-cuenta">
                        <?php foreach ($actividades as $actividad): ?>
                            <?php
                            $participantes = [];
                            if ((bool) $actividad['participaTitular']) $participantes[] = 'Vos';
                            foreach ($actividad['acompanantes'] as $nombreAcompanante) $participantes[] = $nombreAcompanante;
                            ?>
                            <article class="item-resumen-cuenta">
                                <div><h3><?= Utilidades::escapar($actividad['nombre']) ?></h3><p><?= $fechaPerfil($actividad['fechaActividad']) ?> · <?= Utilidades::escapar((string) $actividad['horario']) ?></p></div>
                                <p class="participantes-cuenta"><strong>Participan:</strong> <?= Utilidades::escapar(implode(', ', $participantes)) ?></p>
                                <a href="../actividad.php?id=<?= rawurlencode((string) $actividad['slug']) ?>">Ver actividad →</a>
                            </article>
                        <?php endforeach; ?>
                    </div>
                <?php else: ?>
                    <div class="estado-vacio-cuenta"><p>Todavía no tenés actividades reservadas.</p><a href="../actividades.php">Explorar actividades</a></div>
                <?php endif; ?>
            </section>

            <section class="panel-resumen-cuenta" aria-labelledby="titulo-alojamientos-cuenta">
                <div class="encabezado-panel-cuenta"><div><p class="texto-destacado">ESTADÍA</p><h2 id="titulo-alojamientos-cuenta">Tus alojamientos</h2></div><span><?= count($alojamientos) ?></span></div>
                <?php if ($alojamientos): ?>
                    <div class="lista-resumen-cuenta">
                        <?php foreach ($alojamientos as $alojamiento): ?>
                            <article class="item-resumen-cuenta">
                                <div><h3><?= Utilidades::escapar($alojamiento['tipo']) ?> para <?= (int) $alojamiento['capacidad'] ?> personas</h3><p><?= $fechaPerfil($alojamiento['fechaIngreso']) ?> al <?= $fechaPerfil($alojamiento['fechaEgreso']) ?></p></div>
                                <p><strong><?= (int) $alojamiento['cantidadPersonas'] ?></strong> <?= (int) $alojamiento['cantidadPersonas'] === 1 ? 'huésped' : 'huéspedes' ?> · Estado: <?= Utilidades::escapar(ucfirst(strtolower(str_replace('_', ' ', (string) $alojamiento['estadoReserva'])))) ?></p>
                                <?php if (!empty($alojamiento['montoSena'])): ?><p><strong>Seña:</strong> $<?= number_format((float) $alojamiento['montoSena'], 2, ',', '.') ?> · <?= Utilidades::escapar(str_replace('_', ' ', (string) $alojamiento['estadoPago'])) ?></p><?php endif; ?>
                                <?php if ($alojamiento['acompanantes']): ?><p><strong>Acompañantes:</strong> <?= Utilidades::escapar(implode(', ', array_column($alojamiento['acompanantes'], 'nombre'))) ?></p><?php endif; ?>
                                <?php if ((string) $alojamiento['fechaIngreso'] > date('Y-m-d')): ?>
                                    <details class="gestionar-reserva-cuenta">
                                        <summary>Gestionar reserva</summary>
                                        <form method="post" class="formulario-acompanantes-reserva-cuenta">
                                            <input type="hidden" name="token_csrf" value="<?= Utilidades::escapar(Sesion::tokenCsrf()) ?>">
                                            <input type="hidden" name="accion" value="actualizar_acompanantes_reserva">
                                            <input type="hidden" name="id_reserva" value="<?= (int) $alojamiento['idReserva'] ?>">
                                            <fieldset><legend>Acompañantes de esta estadía</legend>
                                                <?php $idsActuales = array_column($alojamiento['acompanantes'], 'id'); ?>
                                                <?php if ((int) $alojamiento['cantidadPersonas'] <= 1): ?><p>La reserva es para una sola persona.</p><?php else: ?>
                                                    <?php foreach ($acompanantes as $acompananteCuenta): ?>
                                                        <label><input type="checkbox" name="acompanantes[]" value="<?= (int) $acompananteCuenta['id'] ?>"<?= in_array((int) $acompananteCuenta['id'], $idsActuales, true) ? ' checked' : '' ?>><span><?= Utilidades::escapar($acompananteCuenta['nombre'] . ' ' . $acompananteCuenta['apellido']) ?></span></label>
                                                    <?php endforeach; ?>
                                                <?php endif; ?>
                                            </fieldset>
                                            <button class="boton boton-secundario" type="submit">Guardar acompañantes</button>
                                        </form>
                                        <form method="post" onsubmit="return confirm('¿Querés cancelar esta reserva?')">
                                            <input type="hidden" name="token_csrf" value="<?= Utilidades::escapar(Sesion::tokenCsrf()) ?>">
                                            <input type="hidden" name="accion" value="cancelar_reserva">
                                            <input type="hidden" name="id_reserva" value="<?= (int) $alojamiento['idReserva'] ?>">
                                            <button class="boton-enlace boton-cancelar-reserva" type="submit">Cancelar reserva</button>
                                        </form>
                                    </details>
                                <?php endif; ?>
                            </article>
                        <?php endforeach; ?>
                    </div>
                <?php else: ?>
                    <div class="estado-vacio-cuenta"><p>Todavía no tenés una estadía reservada.</p><a href="../alojamientos.php">Elegir alojamiento</a></div>
                <?php endif; ?>
            </section>
        </div>

        <section class="seccion-cuenta-secundaria" aria-labelledby="titulo-acompanantes-cuenta">
            <div class="encabezado-panel-cuenta"><div><p class="texto-destacado">GRUPO</p><h2 id="titulo-acompanantes-cuenta">Tus acompañantes</h2></div><span><?= count($acompanantes) ?></span></div>
            <?php if ($acompanantes): ?>
                <ul class="lista-acompanantes-cuenta">
                    <?php foreach ($acompanantes as $acompanante): ?>
                        <li><span><strong><?= Utilidades::escapar($acompanante['nombre'] . ' ' . $acompanante['apellido']) ?></strong><small><?= Utilidades::escapar($acompanante['parentesco']) ?></small></span><em><?= (int) $acompanante['edad'] ?> años</em></li>
                    <?php endforeach; ?>
                </ul>
                <p class="ayuda-cuenta">Podés seleccionarlos cuando te inscribas en una actividad.</p>
            <?php else: ?>
                <p class="ayuda-cuenta">No hay acompañantes asociados a esta cuenta.</p>
            <?php endif; ?>
        </section>

        <?php if (Autorizacion::esCoordinador($usuario)): ?><a class="boton boton-principal" href="../administrar-actividades.php">Gestionar actividades</a><?php endif; ?>
    </main>
</body>
</html>
