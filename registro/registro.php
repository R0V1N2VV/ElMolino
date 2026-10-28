<?php
declare(strict_types=1);

require_once __DIR__ . '/inicio.php';

Sesion::iniciar();
if ($usuarioActual = Sesion::usuario()) Utilidades::redirigir(Autorizacion::destinoInicial($usuarioActual));

$errores = [];
$diagnostico = null;
$datos = Sesion::obtener('registro_datos', []);
$paso = isset($_GET['paso']) && (int) $_GET['paso'] === 1 ? 1 : ($datos ? 2 : 1);

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if (!Sesion::validarCsrf($_POST['token_csrf'] ?? null)) {
        $errores[] = 'La sesión del formulario venció. Volvé a intentarlo.';
    } else {
        try {
            $registro = new Registro(Conexion::obtener(), new Correo());
            $accion = (string) ($_POST['accion'] ?? '');

            if ($accion === 'guardar_datos') {
                $resultado = $registro->validarDatos($_POST);
                $datos = $resultado['datos'];
                $errores = $resultado['errores'];
                if (!$errores) {
                    Sesion::guardar('registro_datos', $datos);
                    $paso = 2;
                }
            } elseif ($accion === 'enviar_codigo') {
                if (!$datos) {
                    $errores[] = 'Primero completá tus datos personales.';
                    $paso = 1;
                } else {
                    $resultado = $registro->crearPendiente(
                        $datos,
                        (string) ($_POST['contrasena'] ?? ''),
                        (string) ($_POST['repetir_contrasena'] ?? '')
                    );
                    $errores = $resultado['errores'];
                    $diagnostico = $resultado['diagnostico'] ?? null;
                    if (!$errores) {
                        Sesion::guardar('registro_pendiente_id', $resultado['id']);
                        Sesion::eliminar('registro_datos');
                        Utilidades::redirigir('verificar.php');
                    }
                    $paso = 2;
                }
            }
        } catch (PDOException $error) {
            $errores[] = 'No se pudo consultar la base de datos. Verificá la conexión.';
            $info = $error->errorInfo ?? [];
            $diagnostico = 'SQLSTATE ' . (string) ($info[0] ?? $error->getCode()) . '; código MySQL ' . (string) ($info[1] ?? 'sin código');
        }
    }
}
$acompanantesDatos = is_array($datos['acompanantes'] ?? null) ? $datos['acompanantes'] : [];
?>
<!DOCTYPE html>
<html lang="es-AR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="icon" type="image/svg+xml" href="../imgs/favicon-molino.svg">
    <title>Registro | El Molino</title>
    <link rel="stylesheet" href="../estilos/estilos.css?v=<?= (int) filemtime(__DIR__ . '/../estilos/estilos.css') ?>">
    <script src="../script.js?v=<?= (int) filemtime(__DIR__ . '/../script.js') ?>" defer></script>
</head>
<body class="pagina-registro">
    <main class="tarjeta-registro tarjeta-pasos">
        <a class="enlace-volver" href="../index.php">← Volver al inicio</a>
        <p class="texto-destacado">CENTRO RECREATIVO</p>
        <h1>Crear cuenta</h1>
        <p>Completá los datos para consultar reservas y actividades.</p>

        <ol class="pasos-registro" aria-label="Progreso del registro">
            <li class="<?= $paso === 1 ? 'paso-activo' : 'paso-completo' ?>"><span>1</span> Datos y acompañantes</li>
            <li class="<?= $paso === 2 ? 'paso-activo' : '' ?>"><span>2</span> Contraseña</li>
            <li><span>3</span> Verificación</li>
        </ol>

        <?php if ($errores): ?><div class="mensaje mensaje-error" role="alert"><?= Utilidades::escapar(implode(' ', $errores)) ?><?php if ($diagnostico): ?><br><small>Diagnóstico: <?= Utilidades::escapar($diagnostico) ?></small><?php endif; ?></div><?php endif; ?>

        <?php if ($paso === 1): ?>
            <form method="post" class="formulario-registro">
                <input type="hidden" name="token_csrf" value="<?= Utilidades::escapar(Sesion::tokenCsrf()) ?>"><input type="hidden" name="accion" value="guardar_datos">
                <div class="fila-formulario"><div><label for="nombre">Nombre</label><input id="nombre" name="nombre" value="<?= Utilidades::escapar($datos['nombre'] ?? '') ?>" autocomplete="given-name" required></div><div><label for="apellido">Apellido</label><input id="apellido" name="apellido" value="<?= Utilidades::escapar($datos['apellido'] ?? '') ?>" autocomplete="family-name" required></div></div>
                <label for="dni">DNI</label><input id="dni" name="dni" inputmode="numeric" value="<?= Utilidades::escapar($datos['dni'] ?? '') ?>" required>
                <label for="telefono">Teléfono</label><input id="telefono" name="telefono" type="tel" inputmode="tel" value="<?= Utilidades::escapar($datos['telefono'] ?? '') ?>" autocomplete="tel" required>
                <label for="email">Correo electrónico</label><input id="email" name="email" type="email" value="<?= Utilidades::escapar($datos['email'] ?? '') ?>" autocomplete="email" required>
                <label for="direccion">Domicilio</label><input id="direccion" name="direccion" value="<?= Utilidades::escapar($datos['direccion'] ?? '') ?>" autocomplete="street-address" required>

                <fieldset class="bloque-registro-acompanantes">
                    <legend>¿Vas a venir con acompañantes?</legend>
                    <label class="opcion-agregar-acompanantes" for="incluye-acompanantes">
                        <input id="incluye-acompanantes" name="incluye_acompanantes" type="checkbox" value="1"<?= $acompanantesDatos ? ' checked' : '' ?>>
                        <span><strong>Sí, quiero registrarlos ahora</strong><small>Después vas a poder seleccionarlos al reservar un lugar en una actividad.</small></span>
                    </label>
                    <div class="panel-registro-acompanantes" data-panel-acompanantes<?= $acompanantesDatos ? '' : ' hidden' ?>>
                        <div class="cabecera-acompanantes-registro"><div><h2>Personas que te acompañan</h2><p>Podés agregar hasta 8 personas.</p></div><button class="boton-agregar-acompanante" type="button" data-agregar-acompanante>Agregar acompañante</button></div>
                        <div class="lista-registro-acompanantes" data-lista-acompanantes>
                            <?php foreach ($acompanantesDatos as $indice => $acompanante): ?>
                                <article class="fila-registro-acompanante" data-fila-acompanante>
                                    <div class="cabecera-fila-acompanante"><h3>Acompañante <span><?= $indice + 1 ?></span></h3><button type="button" data-quitar-acompanante>Quitar</button></div>
                                    <div class="fila-formulario"><div><label>Nombre<input name="acompanantes[<?= $indice ?>][nombre]" data-campo="nombre" value="<?= Utilidades::escapar($acompanante['nombre'] ?? '') ?>" required></label></div><div><label>Apellido<input name="acompanantes[<?= $indice ?>][apellido]" data-campo="apellido" value="<?= Utilidades::escapar($acompanante['apellido'] ?? '') ?>" required></label></div></div>
                                    <div class="grilla-datos-acompanante"><label>DNI<input name="acompanantes[<?= $indice ?>][dni]" data-campo="dni" inputmode="numeric" value="<?= Utilidades::escapar($acompanante['dni'] ?? '') ?>" required></label><label>Edad<input name="acompanantes[<?= $indice ?>][edad]" data-campo="edad" type="number" min="0" max="120" value="<?= Utilidades::escapar($acompanante['edad'] ?? '') ?>" required></label><label>Vínculo<input name="acompanantes[<?= $indice ?>][parentesco]" data-campo="parentesco" value="<?= Utilidades::escapar($acompanante['parentesco'] ?? '') ?>" placeholder="Ej.: hija, amigo" required></label></div>
                                </article>
                            <?php endforeach; ?>
                        </div>
                    </div>
                </fieldset>
                <button type="submit">Continuar</button>
            </form>
        <?php else: ?>
            <form method="post" class="formulario-registro">
                <input type="hidden" name="token_csrf" value="<?= Utilidades::escapar(Sesion::tokenCsrf()) ?>"><input type="hidden" name="accion" value="enviar_codigo">
                <p class="resumen-registro">El código se enviará a <strong><?= Utilidades::escapar($datos['email'] ?? '') ?></strong>.<?= $acompanantesDatos ? ' También se registrarán ' . count($acompanantesDatos) . (count($acompanantesDatos) === 1 ? ' acompañante.' : ' acompañantes.') : '' ?></p>
                <label for="contrasena">Contraseña</label><input id="contrasena" name="contrasena" type="password" minlength="8" autocomplete="new-password" required>
                <label for="repetir_contrasena">Repetir contraseña</label><input id="repetir_contrasena" name="repetir_contrasena" type="password" minlength="8" autocomplete="new-password" required>
                <button type="submit">Enviar código de verificación</button><a class="enlace-secundario" href="registro.php?paso=1">Editar mis datos</a>
            </form>
        <?php endif; ?>
        <p class="enlace-inicio-sesion">¿Ya tenés una cuenta? <a href="login.php">Iniciá sesión</a></p>

        <template id="plantilla-acompanante-registro">
            <article class="fila-registro-acompanante" data-fila-acompanante>
                <div class="cabecera-fila-acompanante"><h3>Acompañante <span></span></h3><button type="button" data-quitar-acompanante>Quitar</button></div>
                <div class="fila-formulario"><div><label>Nombre<input data-campo="nombre" required></label></div><div><label>Apellido<input data-campo="apellido" required></label></div></div>
                <div class="grilla-datos-acompanante"><label>DNI<input data-campo="dni" inputmode="numeric" required></label><label>Edad<input data-campo="edad" type="number" min="0" max="120" required></label><label>Vínculo<input data-campo="parentesco" placeholder="Ej.: hija, amigo" required></label></div>
            </article>
        </template>
    </main>
</body>
</html>
