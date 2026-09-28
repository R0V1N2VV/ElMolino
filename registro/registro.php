<?php
declare(strict_types=1);

final class Registro
{
    public function __construct(private PDO $conexion, private Correo $correo)
    {
    }

    public function validarDatos(array $entrada): array
    {
        $datos = [
            'nombre' => trim((string) ($entrada['nombre'] ?? '')),
            'apellido' => trim((string) ($entrada['apellido'] ?? '')),
            'dni' => preg_replace('/\D+/', '', (string) ($entrada['dni'] ?? '')),
            'telefono' => preg_replace('/\D+/', '', (string) ($entrada['telefono'] ?? '')),
            'email' => strtolower(trim((string) ($entrada['email'] ?? ''))),
            'direccion' => trim((string) ($entrada['direccion'] ?? '')),
        ];
        $errores = [];
        foreach (['nombre' => 'nombre', 'apellido' => 'apellido', 'dni' => 'DNI', 'telefono' => 'teléfono', 'direccion' => 'domicilio'] as $campo => $etiqueta) {
            if ($datos[$campo] === '') $errores[] = "Completá el campo {$etiqueta}.";
        }
        if (!filter_var($datos['email'], FILTER_VALIDATE_EMAIL)) $errores[] = 'Ingresá un correo electrónico válido.';
        if (strlen($datos['dni']) < 7 || strlen($datos['dni']) > 8 || strlen($datos['telefono']) < 8 || strlen($datos['telefono']) > 15) $errores[] = 'Revisá el DNI y el teléfono ingresados.';
        if (!$errores && $this->datosYaRegistrados($datos)) $errores[] = 'Ya existe una cuenta con esos datos.';
        return ['datos' => $datos, 'errores' => $errores];
    }

    public function crearPendiente(array $datos, string $contrasena, string $repetirContrasena): array
    {
        $errores = [];
        if (strlen($contrasena) < 8) $errores[] = 'La contraseña debe tener al menos 8 caracteres.';
        if ($contrasena !== $repetirContrasena) $errores[] = 'Las contraseñas no coinciden.';
        if ($errores) return ['errores' => $errores];

        $codigo = Utilidades::codigoVerificacion();
        $pendiente = [...$datos, 'password_hash' => password_hash($contrasena, PASSWORD_DEFAULT), 'codigo_hash' => password_hash($codigo, PASSWORD_DEFAULT)];
        try {
            $this->conexion->beginTransaction();
            $this->conexion->prepare('DELETE FROM registro_pendiente WHERE email = :email OR dni = :dni')->execute(['email' => $datos['email'], 'dni' => $datos['dni']]);
            $sql = 'INSERT INTO registro_pendiente (dni, nombre, apellido, telefono, email, direccion, password_hash, codigo_hash, vence_en) VALUES (:dni, :nombre, :apellido, :telefono, :email, :direccion, :password_hash, :codigo_hash, DATE_ADD(NOW(), INTERVAL 15 MINUTE))';
            $this->conexion->prepare($sql)->execute($pendiente);
            $id = (int) $this->conexion->lastInsertId();
            $this->conexion->commit();
            $this->correo->enviarCodigoRegistro($datos['email'], $datos['nombre'], $codigo);
            return ['id' => $id, 'errores' => []];
        } catch (Throwable $error) {
            if ($this->conexion->inTransaction()) $this->conexion->rollBack();
            if (isset($id)) $this->conexion->prepare('DELETE FROM registro_pendiente WHERE idRegistroPendiente = :id')->execute(['id' => $id]);
            return ['errores' => [$error instanceof RuntimeException ? $error->getMessage() : 'No se pudo guardar el registro pendiente. Ejecutá BD/registro_pendiente.sql.']];
        }
    }

    public function verificar(int $id, string $codigo): array
    {
        $consulta = $this->conexion->prepare('SELECT * FROM registro_pendiente WHERE idRegistroPendiente = :id LIMIT 1');
        $consulta->execute(['id' => $id]);
        $pendiente = $consulta->fetch();
        if (!$pendiente) return ['errores' => ['El registro pendiente ya no está disponible.'], 'usuario' => null];
        if (strtotime($pendiente['vence_en']) < time()) return ['errores' => ['El código venció. Pedí uno nuevo para continuar.'], 'usuario' => null];
        if ((int) $pendiente['intentos'] >= 5) return ['errores' => ['Superaste la cantidad de intentos. Pedí un código nuevo.'], 'usuario' => null];
        if (!password_verify($codigo, $pendiente['codigo_hash'])) {
            $this->conexion->prepare('UPDATE registro_pendiente SET intentos = intentos + 1 WHERE idRegistroPendiente = :id')->execute(['id' => $id]);
            return ['errores' => ['El código no es correcto.'], 'usuario' => null];
        }
        try {
            $this->conexion->beginTransaction();
            $this->conexion->prepare('INSERT INTO cliente (dni, nombre, apellido, telefono, email, direccion) VALUES (:dni, :nombre, :apellido, :telefono, :email, :direccion)')->execute([
                'dni' => $pendiente['dni'],
                'nombre' => $pendiente['nombre'],
                'apellido' => $pendiente['apellido'],
                'telefono' => $pendiente['telefono'],
                'email' => $pendiente['email'],
                'direccion' => $pendiente['direccion'],
            ]);
            $idCliente = (int) $this->conexion->lastInsertId();
            $this->conexion->prepare('INSERT INTO usuarios (nombre, apellido, email, password_hash, rol) VALUES (:nombre, :apellido, :email, :password_hash, :rol)')->execute(['nombre' => $pendiente['nombre'], 'apellido' => $pendiente['apellido'], 'email' => $pendiente['email'], 'password_hash' => $pendiente['password_hash'], 'rol' => 'cliente']);
            $idUsuario = (int) $this->conexion->lastInsertId();
            $this->conexion->prepare('DELETE FROM registro_pendiente WHERE idRegistroPendiente = :id')->execute(['id' => $id]);
            $this->conexion->commit();
            return ['errores' => [], 'usuario' => ['id_usuario' => $idUsuario, 'id_cliente' => $idCliente, 'nombre' => $pendiente['nombre'], 'email' => $pendiente['email'], 'rol' => 'cliente']];
        } catch (PDOException) {
            if ($this->conexion->inTransaction()) $this->conexion->rollBack();
            return ['errores' => ['No se pudo activar la cuenta. Es posible que esos datos ya estén registrados.'], 'usuario' => null];
        }
    }

    public function reenviarCodigo(int $id): string
    {
        $consulta = $this->conexion->prepare('SELECT * FROM registro_pendiente WHERE idRegistroPendiente = :id LIMIT 1');
        $consulta->execute(['id' => $id]);
        $pendiente = $consulta->fetch();
        if (!$pendiente) throw new RuntimeException('El registro pendiente ya no está disponible.');
        if (strtotime($pendiente['actualizado_en']) > time() - 60) throw new RuntimeException('Esperá un minuto antes de pedir otro código.');
        $codigo = Utilidades::codigoVerificacion();
        $this->correo->enviarCodigoRegistro($pendiente['email'], $pendiente['nombre'], $codigo);
        $this->conexion->prepare('UPDATE registro_pendiente SET codigo_hash = :codigo_hash, vence_en = DATE_ADD(NOW(), INTERVAL 15 MINUTE), intentos = 0 WHERE idRegistroPendiente = :id')->execute(['codigo_hash' => password_hash($codigo, PASSWORD_DEFAULT), 'id' => $id]);
        return 'Te enviamos un nuevo código.';
    }

    private function datosYaRegistrados(array $datos): bool
    {
        $consulta = $this->conexion->prepare('SELECT 1 FROM cliente WHERE email = :email OR dni = :dni OR telefono = :telefono LIMIT 1');
        $consulta->execute(['email' => $datos['email'], 'dni' => $datos['dni'], 'telefono' => $datos['telefono']]);
        $usuario = $this->conexion->prepare('SELECT 1 FROM usuarios WHERE email = :email LIMIT 1');
        $usuario->execute(['email' => $datos['email']]);
        return (bool) ($consulta->fetch() || $usuario->fetch());
    }
}

<!DOCTYPE html>
<html lang="es-AR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Registro | El Molino</title>
    <link rel="stylesheet" href="../estilos/estilos.css">
</head>
<body class="pagina-registro">
    <main class="tarjeta-registro tarjeta-pasos">
        <a class="enlace-volver" href="../index.html">← Volver al inicio</a>
        <p class="texto-destacado">CENTRO RECREATIVO</p>
        <h1>Crear cuenta</h1>
        <p>Completá los datos para consultar reservas y actividades.</p>

        <ol class="pasos-registro" aria-label="Progreso del registro">
            <li class="<?= $paso === 1 ? 'paso-activo' : 'paso-completo' ?>"><span>1</span> Tus datos</li>
            <li class="<?= $paso === 2 ? 'paso-activo' : '' ?>"><span>2</span> Contraseña</li>
            <li><span>3</span> Verificación</li>
        </ol>

        <?php if ($errores): ?>
            <div class="mensaje mensaje-error" role="alert"><?= Utilidades::escapar(implode(' ', $errores)) ?></div>
        <?php endif; ?>

        <?php if ($paso === 1): ?>
            <form method="post" class="formulario-registro">
                <input type="hidden" name="token_csrf" value="<?= Utilidades::escapar(Sesion::tokenCsrf()) ?>">
                <input type="hidden" name="accion" value="guardar_datos">
                <div class="fila-formulario">
                    <div><label for="nombre">Nombre</label><input id="nombre" name="nombre" value="<?= Utilidades::escapar($datos['nombre'] ?? '') ?>" autocomplete="given-name" required></div>
                    <div><label for="apellido">Apellido</label><input id="apellido" name="apellido" value="<?= Utilidades::escapar($datos['apellido'] ?? '') ?>" autocomplete="family-name" required></div>
                </div>
                <label for="dni">DNI</label><input id="dni" name="dni" inputmode="numeric" value="<?= Utilidades::escapar($datos['dni'] ?? '') ?>" required>
                <label for="telefono">Teléfono</label><input id="telefono" name="telefono" type="tel" inputmode="tel" value="<?= Utilidades::escapar($datos['telefono'] ?? '') ?>" autocomplete="tel" required>
                <label for="email">Correo electrónico</label><input id="email" name="email" type="email" value="<?= Utilidades::escapar($datos['email'] ?? '') ?>" autocomplete="email" required>
                <label for="direccion">Domicilio</label><input id="direccion" name="direccion" value="<?= Utilidades::escapar($datos['direccion'] ?? '') ?>" autocomplete="street-address" required>
                <button type="submit">Continuar</button>
            </form>
        <?php else: ?>
            <form method="post" class="formulario-registro">
                <input type="hidden" name="token_csrf" value="<?= Utilidades::escapar(Sesion::tokenCsrf()) ?>">
                <input type="hidden" name="accion" value="enviar_codigo">
                <p class="resumen-registro">El código se enviará a <strong><?= Utilidades::escapar($datos['email'] ?? '') ?></strong>.</p>
                <label for="contrasena">Contraseña</label><input id="contrasena" name="contrasena" type="password" minlength="8" autocomplete="new-password" required>
                <label for="repetir_contrasena">Repetir contraseña</label><input id="repetir_contrasena" name="repetir_contrasena" type="password" minlength="8" autocomplete="new-password" required>
                <button type="submit">Enviar código de verificación</button>
                <a class="enlace-secundario" href="registro.php?paso=1">Editar mis datos</a>
            </form>
        <?php endif; ?>
        <p class="enlace-inicio-sesion">¿Ya tenés una cuenta? <a href="login.php">Iniciá sesión</a></p>
    </main>
</body>
</html>
