<?php
declare(strict_types=1);

final class Autenticacion
{
    public function __construct(private PDO $conexion) {}
    public function iniciarSesion(string $correo, string $contrasena): ?array
    {
        if (!filter_var($correo, FILTER_VALIDATE_EMAIL) || $contrasena === '') return null;
        $consulta = $this->conexion->prepare('SELECT idUsuario, nombre, apellido, email, password_hash, rol FROM usuarios WHERE email = :email LIMIT 1');
        $consulta->execute(['email' => strtolower(trim($correo))]);
        $usuario = $consulta->fetch();
        if (!$usuario || !SeguridadPassword::verificar($contrasena, $usuario['password_hash'])) return null;
        if (SeguridadPassword::necesitaMigracion($usuario['password_hash'])) {
            $nuevoHash = SeguridadPassword::crearHash($contrasena);
            $actualizar = $this->conexion->prepare('UPDATE usuarios SET password_hash = :password_hash WHERE idUsuario = :idUsuario');
            $actualizar->execute(['password_hash' => $nuevoHash, 'idUsuario' => $usuario['idUsuario']]);
        }
        $cliente = $this->conexion->prepare('SELECT idCliente FROM cliente WHERE email = :email LIMIT 1');
        $cliente->execute(['email' => $usuario['email']]);
        $datosCliente = $cliente->fetch();
        return ['id_usuario' => (int) $usuario['idUsuario'], 'id_cliente' => $datosCliente ? (int) $datosCliente['idCliente'] : null, 'nombre' => $usuario['nombre'], 'email' => $usuario['email'], 'rol' => $usuario['rol']];
    }
}
