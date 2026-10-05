<?php
declare(strict_types=1);

final class Registro
{
    public function __construct(private PDO $conexion, private Correo $correo) {}

    public function validarDatos(array $entrada): array
    {
        $datos = ['nombre' => trim((string) ($entrada['nombre'] ?? '')), 'apellido' => trim((string) ($entrada['apellido'] ?? '')), 'dni' => preg_replace('/\D+/', '', (string) ($entrada['dni'] ?? '')), 'telefono' => preg_replace('/\D+/', '', (string) ($entrada['telefono'] ?? '')), 'email' => strtolower(trim((string) ($entrada['email'] ?? ''))), 'direccion' => trim((string) ($entrada['direccion'] ?? ''))];
        $errores = [];
        foreach (['nombre' => 'nombre', 'apellido' => 'apellido', 'dni' => 'DNI', 'telefono' => 'teléfono', 'direccion' => 'domicilio'] as $campo => $etiqueta) if ($datos[$campo] === '') $errores[] = "Completá el campo {$etiqueta}.";
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
        $pendiente = [...$datos, 'password_hash' => SeguridadPassword::crearHash($contrasena), 'codigo_hash' => password_hash($codigo, PASSWORD_DEFAULT)];
        try {
            $this->conexion->beginTransaction();
            $this->conexion->prepare('DELETE FROM registro_pendiente WHERE email = :email OR dni = :dni')->execute(['email' => $datos['email'], 'dni' => $datos['dni']]);
            $this->conexion->prepare('INSERT INTO registro_pendiente (dni, nombre, apellido, telefono, email, direccion, password_hash, codigo_hash, vence_en) VALUES (:dni, :nombre, :apellido, :telefono, :email, :direccion, :password_hash, :codigo_hash, DATE_ADD(NOW(), INTERVAL 15 MINUTE))')->execute($pendiente);
            $id = (int) $this->conexion->lastInsertId();
            $this->conexion->commit();
            $this->correo->enviarCodigoRegistro($datos['email'], $datos['nombre'], $codigo);
            return ['id' => $id, 'errores' => []];
        } catch (Throwable $error) {
            if ($this->conexion->inTransaction()) $this->conexion->rollBack();
            if (isset($id)) $this->conexion->prepare('DELETE FROM registro_pendiente WHERE idRegistroPendiente = :id')->execute(['id' => $id]);
            if ($error instanceof RuntimeException) {
                return ['errores' => [$error->getMessage()]];
            }
            error_log('Error al crear registro pendiente: ' . $error->getMessage());
            $diagnostico = null;
            if ($error instanceof PDOException) {
                $info = $error->errorInfo ?? [];
                $sqlState = (string) ($info[0] ?? $error->getCode());
                $codigoMotor = (string) ($info[1] ?? 'sin código');
                $diagnostico = "SQLSTATE {$sqlState}; código MySQL {$codigoMotor}";
            } else {
                $diagnostico = 'Error PHP: ' . get_class($error) . '; detalle: ' . $error->getMessage();
            }
            return ['errores' => ['No se pudo completar el registro. Revisá la configuración de la base de datos y el archivo de registro pendiente.'], 'diagnostico' => $diagnostico];
        }
    }

    public function verificar(int $id, string $codigo): array
    {
        $consulta = $this->conexion->prepare('SELECT *, (vence_en <= NOW()) AS codigo_vencido FROM registro_pendiente WHERE idRegistroPendiente = :id LIMIT 1');
        $consulta->execute(['id' => $id]);
        $pendiente = $consulta->fetch();
        if (!$pendiente) return ['errores' => ['El registro pendiente ya no está disponible.'], 'usuario' => null];
        if ((bool) $pendiente['codigo_vencido']) return ['errores' => ['El código venció. Pedí uno nuevo para continuar.'], 'usuario' => null];
        if ((int) $pendiente['intentos'] >= 5) return ['errores' => ['Superaste la cantidad de intentos. Pedí un código nuevo.'], 'usuario' => null];
        if (!password_verify($codigo, $pendiente['codigo_hash'])) {
            $this->conexion->prepare('UPDATE registro_pendiente SET intentos = intentos + 1 WHERE idRegistroPendiente = :id')->execute(['id' => $id]);
            return ['errores' => ['El código no es correcto.'], 'usuario' => null];
        }
        try {
            $this->conexion->beginTransaction();
            $this->conexion->prepare('INSERT INTO cliente (dni, nombre, apellido, telefono, email, direccion) VALUES (:dni, :nombre, :apellido, :telefono, :email, :direccion)')->execute(['dni' => $pendiente['dni'], 'nombre' => $pendiente['nombre'], 'apellido' => $pendiente['apellido'], 'telefono' => $pendiente['telefono'], 'email' => $pendiente['email'], 'direccion' => $pendiente['direccion']]);
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
        $consulta = $this->conexion->prepare('SELECT *, (actualizado_en > DATE_SUB(NOW(), INTERVAL 60 SECOND)) AS espera_reenvio FROM registro_pendiente WHERE idRegistroPendiente = :id LIMIT 1');
        $consulta->execute(['id' => $id]);
        $pendiente = $consulta->fetch();
        if (!$pendiente) throw new RuntimeException('El registro pendiente ya no está disponible.');
        if ((bool) $pendiente['espera_reenvio']) throw new RuntimeException('Esperá un minuto antes de pedir otro código.');
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
