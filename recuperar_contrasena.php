<?php
declare(strict_types=1);

require_once __DIR__ . '/inicio.php';

Sesion::iniciar();
$errores = [];
$paso = (string) Sesion::obtener('recuperacion_cliente', '') !== '' ? 'restablecer' : 'solicitar';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if (!Sesion::validarCsrf($_POST['token_csrf'] ?? null)) {
        $errores[] = 'La sesión venció. Volvé a intentarlo.';
    } else {
        $conexion = Conexion::obtener();
        $accion = (string) ($_POST['accion'] ?? '');
        try {
            if ($accion === 'usar_otro_correo') {
                Sesion::eliminar('recuperacion_cliente');
                Utilidades::redirigir('recuperar_contrasena.php');
            } elseif ($accion === 'solicitar') {
                $email = strtolower(trim((string) ($_POST['correo'] ?? '')));
                if (!filter_var($email, FILTER_VALIDATE_EMAIL)) throw new InvalidArgumentException('Ingresá un correo válido.');
                $cliente = $conexion->prepare('SELECT idCliente,nombre,email FROM cliente WHERE LOWER(email)=:email LIMIT 1');
                $cliente->execute(['email' => $email]);
                $datos = $cliente->fetch();
                // Se responde igual aunque el correo no exista para no exponer cuentas registradas.
                if ($datos) {
                    $codigo = (string) random_int(100000, 999999);
                    $conexion->prepare(
                        'INSERT INTO recuperacion_contrasena (idCliente,codigo_hash,venceEn,intentos)
                         VALUES (:cliente,:hash,DATE_ADD(NOW(),INTERVAL 15 MINUTE),0)
                         ON DUPLICATE KEY UPDATE codigo_hash=VALUES(codigo_hash),venceEn=VALUES(venceEn),intentos=0,creadoEn=NOW()'
                    )->execute(['cliente' => (int) $datos['idCliente'], 'hash' => password_hash($codigo, PASSWORD_DEFAULT)]);
                    (new Correo())->enviarCodigoRecuperacion((string) $datos['email'], (string) $datos['nombre'], $codigo);
                    Sesion::guardar('recuperacion_cliente', (int) $datos['idCliente']);
                    $paso = 'restablecer';
                }
                Sesion::guardarMensaje('exito', 'Si existe una cuenta con ese correo, enviamos un código de recuperación.');
                if (!$datos) Utilidades::redirigir('recuperar_contrasena.php');
            } elseif ($accion === 'restablecer') {
                $idCliente = (int) Sesion::obtener('recuperacion_cliente', 0);
                $codigo = preg_replace('/\D+/', '', (string) ($_POST['codigo'] ?? ''));
                $contrasena = (string) ($_POST['contrasena'] ?? '');
                $repetir = (string) ($_POST['repetir_contrasena'] ?? '');
                if ($idCliente <= 0) throw new RuntimeException('Solicitá un código nuevo para continuar.');
                if (strlen($codigo) !== 6) throw new InvalidArgumentException('Ingresá los seis números del código.');
                if (strlen($contrasena) < 8 || $contrasena !== $repetir) throw new InvalidArgumentException('La contraseña debe tener al menos 8 caracteres y coincidir en ambos campos.');
                $consulta = $conexion->prepare('SELECT codigo_hash,venceEn,intentos FROM recuperacion_contrasena WHERE idCliente=:cliente FOR UPDATE');
                $consulta->execute(['cliente' => $idCliente]);
                $recuperacion = $consulta->fetch();
                if (!$recuperacion || new DateTimeImmutable((string) $recuperacion['venceEn']) < new DateTimeImmutable('now')) throw new RuntimeException('El código venció. Solicitá uno nuevo.');
                if ((int) $recuperacion['intentos'] >= 5) throw new RuntimeException('Superaste los intentos permitidos. Solicitá un código nuevo.');
                if (!password_verify($codigo, (string) $recuperacion['codigo_hash'])) {
                    $conexion->prepare('UPDATE recuperacion_contrasena SET intentos=intentos+1 WHERE idCliente=:cliente')->execute(['cliente' => $idCliente]);
                    throw new InvalidArgumentException('El código no es correcto.');
                }
                $hash = SeguridadPassword::crearHash($contrasena);
                $conexion->beginTransaction();
                $conexion->prepare('UPDATE cliente SET password_hash=:hash WHERE idCliente=:cliente')->execute(['hash' => $hash, 'cliente' => $idCliente]);
                $conexion->prepare('UPDATE usuarios u JOIN cliente c ON LOWER(c.email)=LOWER(u.email) SET u.password_hash=:hash WHERE c.idCliente=:cliente AND UPPER(u.rol)=\'CLIENTE\'')->execute(['hash' => $hash, 'cliente' => $idCliente]);
                $conexion->prepare('DELETE FROM recuperacion_contrasena WHERE idCliente=:cliente')->execute(['cliente' => $idCliente]);
                $conexion->commit();
                Sesion::eliminar('recuperacion_cliente');
                Sesion::guardarMensaje('exito', 'Tu contraseña fue actualizada. Ya podés iniciar sesión.');
                Utilidades::redirigir('login.php');
            }
        } catch (Throwable $error) {
            if ($conexion->inTransaction()) $conexion->rollBack();
            $errores[] = $error instanceof InvalidArgumentException || $error instanceof RuntimeException
                ? $error->getMessage() : 'No se pudo completar la recuperación. Verificá que ejecutaste BD/09_RECUPERACION_CONTRASENA.sql.';
        }
    }
}
$mensaje = Sesion::tomarMensaje();
?>
<!doctype html><html lang="es-AR"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><link rel="stylesheet" href="../estilos/estilos.css"><title>Recuperar contraseña | El Molino</title></head>
<body><main class="pagina-registro"><section class="tarjeta-registro"><h1>Recuperar contraseña</h1>
<?php if ($mensaje): ?><p class="mensaje"><?= Utilidades::escapar($mensaje['texto']) ?></p><?php endif; ?>
<?php if ($errores): ?><p class="mensaje mensaje-error"><?= Utilidades::escapar(implode(' ', $errores)) ?></p><?php endif; ?>
<?php if ($paso === 'solicitar'): ?><form method="post"><input type="hidden" name="token_csrf" value="<?= Utilidades::escapar(Sesion::tokenCsrf()) ?>"><input type="hidden" name="accion" value="solicitar"><label>Correo electrónico<input name="correo" type="email" autocomplete="email" required></label><button class="boton boton-principal" type="submit">Enviar código</button></form>
<?php else: ?><p>Revisá tu correo e ingresá el código de seis números.</p><form method="post"><input type="hidden" name="token_csrf" value="<?= Utilidades::escapar(Sesion::tokenCsrf()) ?>"><input type="hidden" name="accion" value="restablecer"><label>Código<input name="codigo" inputmode="numeric" maxlength="6" required></label><label>Contraseña nueva<input name="contrasena" type="password" minlength="8" autocomplete="new-password" required></label><label>Repetir contraseña<input name="repetir_contrasena" type="password" minlength="8" autocomplete="new-password" required></label><button class="boton boton-principal" type="submit">Guardar contraseña</button></form><form method="post"><input type="hidden" name="token_csrf" value="<?= Utilidades::escapar(Sesion::tokenCsrf()) ?>"><input type="hidden" name="accion" value="usar_otro_correo"><button class="boton boton-neutral" type="submit">Usar otro correo</button></form><?php endif; ?>
<p><a href="login.php">Volver a iniciar sesión</a></p></section></main></body></html>
