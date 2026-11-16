<?php
/**
 * ARCHIVO EXPLICADO: php/Autenticacion.php
 *
 * Comprueba las credenciales de empleados y clientes.
 */

declare(strict_types=1);

final class Autenticacion
{
    public function __construct(private PDO $conexion) {}

    /** Inicia sesión primero como empleado y luego como cliente del sitio. */
    public function iniciarSesion(string $correo, string $contrasena): ?array
    {
        if (!filter_var($correo, FILTER_VALIDATE_EMAIL) || $contrasena === '') return null;
        $correo = strtolower(trim($correo));

        $consultaEmpleado = $this->conexion->prepare(
            "SELECT idUsuario, nombre, apellido, email, password_hash, rol FROM usuarios WHERE email = :email AND UPPER(rol) <> 'CLIENTE' LIMIT 1"
        );
        $consultaEmpleado->execute(['email' => $correo]);
        $empleado = $consultaEmpleado->fetch();
        if ($empleado) {
            if (!SeguridadPassword::verificar($contrasena, $empleado['password_hash'])) return null;
            if (SeguridadPassword::necesitaMigracion($empleado['password_hash'])) {
                $actualizar = $this->conexion->prepare('UPDATE usuarios SET password_hash = :hash WHERE idUsuario = :id');
                $actualizar->execute(['hash' => SeguridadPassword::crearHash($contrasena), 'id' => $empleado['idUsuario']]);
            }
            return ['id_usuario' => (int) $empleado['idUsuario'], 'id_cliente' => null,
                'nombre' => $empleado['nombre'], 'email' => $empleado['email'], 'rol' => $empleado['rol']];
        }

        $consultaCliente = $this->conexion->prepare(
            'SELECT idCliente, nombre, email, password_hash FROM cliente WHERE email = :email LIMIT 1'
        );
        $consultaCliente->execute(['email' => $correo]);
        $cliente = $consultaCliente->fetch();
        if ($cliente && !empty($cliente['password_hash'])
                && SeguridadPassword::verificar($contrasena, $cliente['password_hash'])) {
            if (SeguridadPassword::necesitaMigracion($cliente['password_hash'])) {
                $actualizar = $this->conexion->prepare('UPDATE cliente SET password_hash = :hash WHERE idCliente = :id');
                $actualizar->execute(['hash' => SeguridadPassword::crearHash($contrasena), 'id' => $cliente['idCliente']]);
            }
            return ['id_usuario' => null, 'id_cliente' => (int) $cliente['idCliente'],
                'nombre' => $cliente['nombre'], 'email' => $cliente['email'], 'rol' => 'cliente'];
        }

        // Compatibilidad temporal con cuentas de cliente creadas antes de separar ambas tablas.
        $consultaAntigua = $this->conexion->prepare(
            "SELECT idUsuario, nombre, email, password_hash FROM usuarios WHERE email = :email AND UPPER(rol) = 'CLIENTE' LIMIT 1"
        );
        $consultaAntigua->execute(['email' => $correo]);
        $antiguo = $consultaAntigua->fetch();
        if (!$antiguo || !SeguridadPassword::verificar($contrasena, $antiguo['password_hash'])) return null;
        $buscarCliente = $this->conexion->prepare('SELECT idCliente FROM cliente WHERE email = :email LIMIT 1');
        $buscarCliente->execute(['email' => $correo]);
        $idCliente = $buscarCliente->fetchColumn();
        if (!$idCliente) return null;
        return ['id_usuario' => null, 'id_cliente' => (int) $idCliente,
            'nombre' => $antiguo['nombre'], 'email' => $antiguo['email'], 'rol' => 'cliente'];
    }
}
