package com.elmolino.dao;

import com.elmolino.model.Usuario;
import com.elmolino.util.Conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

public class UsuarioDAO extends BaseDAO<Usuario> {

    @Override
    protected Usuario mapear(ResultSet resultados) throws SQLException {
        return new Usuario(resultados.getInt("idUsuario"), resultados.getString("nombre"),
                resultados.getString("apellido"), resultados.getString("email"),
                resultados.getString("password_hash"), resultados.getString("rol"));
    }

    /** Roles admitidos para cuentas internas de la aplicación. */
    public static final Set<String> ROLES_EMPLEADO = Set.of(
            "SUPERADMIN", "ADMINISTRADOR", "RECEPCION", "ADMINISTRACION", "COORDINADOR", "GERENCIA", "LIMPIEZA");

    /** Busca una cuenta por email y devuelve null si no existe. */
    public Usuario buscarPorEmail(String correo) throws SQLException {
        // La consulta trae los campos usados para validar el acceso.
        String consultaSql = "SELECT idUsuario, nombre, apellido, email, password_hash, rol "
                   + "FROM usuarios WHERE email = ?";

        return consultarUno(consultaSql, sentencia -> sentencia.setString(1, correo));
    }

    /** Cambia una clave antigua por su huella segura luego de un acceso valido. */
    public void actualizarPasswordHash(int idUsuario, String hashContrasena) throws SQLException {
        // Actualiza solo la cuenta indicada y nunca modifica otras columnas.
        String consultaSql = "UPDATE usuarios SET password_hash = ? WHERE idUsuario = ?";
        try (Connection conexion = Conexion.obtener();
             PreparedStatement sentencia = conexion.prepareStatement(consultaSql)) {
            sentencia.setString(1, hashContrasena);
            sentencia.setInt(2, idUsuario);
            sentencia.executeUpdate();
        }
    }
}
