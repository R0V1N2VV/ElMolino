<?php
/**
 * ARCHIVO EXPLICADO: php/Autenticacion.php
 *
 * Comprueba las credenciales y recupera los datos del usuario que inicia sesión.
 * Las funciones de este archivo están documentadas junto a su declaración.
 */

declare(strict_types=1);

final class Autenticacion
{
    /**
     * Inicializa el objeto y deja preparadas las dependencias que utilizarán sus demás métodos.
     *
     * @param mixed $conexion Valor de conexion que recibe la función.
     */
    public function __construct(private PDO $conexion) {}
    /**
     * Inicia sesion con los datos recibidos.
     *
     * @param mixed $correo Valor de correo que recibe la función.
     * @param mixed $contrasena Valor de contrasena que recibe la función.
     * @return ?array Resultado producido por esta operación.
     */
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
