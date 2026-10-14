package com.elmolino.dao;

import com.elmolino.model.Usuario;
import com.elmolino.util.Conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

public class UsuarioDAO {

    /** Devuelve el usuario si el email existe, o null si no hay ninguno con ese email. */
    public Usuario buscarPorEmail(String email) throws SQLException {
        String sql = "SELECT idUsuario, nombre, apellido, email, password_hash, rol "
                   + "FROM usuarios WHERE email = ?";

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Usuario(
                            rs.getInt("idUsuario"),
                            rs.getString("nombre"),
                            rs.getString("apellido"),
                            rs.getString("email"),
                            rs.getString("password_hash"),
                            rs.getString("rol"));
                }
            }
        }
        return null;
    }

    /** Crea una cuenta con rol inicial limitado; el formulario nunca recibe el rol. */
    public boolean crearCuenta(String nombre, String apellido, String email,
                               String passwordHash) throws SQLException {
        String sql = "INSERT INTO usuarios (nombre, apellido, email, password_hash, rol) "
                   + "VALUES (?, ?, ?, ?, 'RECEPCION')";

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            try (PreparedStatement buscar = con.prepareStatement(
                    "SELECT 1 FROM usuarios WHERE LOWER(email) = LOWER(?) LIMIT 1")) {
                buscar.setString(1, email);
                try (ResultSet rs = buscar.executeQuery()) {
                    if (rs.next()) {
                        return false;
                    }
                }
            }
            ps.setString(1, nombre);
            ps.setString(2, apellido);
            ps.setString(3, email);
            ps.setString(4, passwordHash);
            ps.executeUpdate();
            return true;
        } catch (SQLIntegrityConstraintViolationException ex) {
            if (ex.getErrorCode() == 1062) {
                return false;
            }
            throw ex;
        } catch (SQLException ex) {
            // Algunos drivers MySQL reportan una clave duplicada como SQLException general.
            if (ex.getErrorCode() == 1062) {
                return false;
            }
            throw ex;
        }
    }

    /** Actualiza una clave antigua en texto plano después de un inicio de sesión válido. */
    public void actualizarPasswordHash(int idUsuario, String passwordHash) throws SQLException {
        String sql = "UPDATE usuarios SET password_hash = ? WHERE idUsuario = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setInt(2, idUsuario);
            ps.executeUpdate();
        }
    }
}
