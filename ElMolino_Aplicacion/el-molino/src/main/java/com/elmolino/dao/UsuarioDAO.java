package com.elmolino.dao;

import com.elmolino.model.Usuario;
import com.elmolino.util.Conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;

public class UsuarioDAO {

    /** Busca una cuenta por email y devuelve null si no existe. */
    public Usuario buscarPorEmail(String correo) throws SQLException {
        // La consulta trae los campos usados para validar el acceso.
        String consultaSql = "SELECT idUsuario, nombre, apellido, email, password_hash, rol "
                   + "FROM usuarios WHERE email = ?";

        try (Connection conexion = Conexion.obtener();
             PreparedStatement sentencia = conexion.prepareStatement(consultaSql)) {

            sentencia.setString(1, correo);
            try (ResultSet resultados = sentencia.executeQuery()) {
                if (resultados.next()) {
                    return new Usuario(
                            resultados.getInt("idUsuario"),
                            resultados.getString("nombre"),
                            resultados.getString("apellido"),
                            resultados.getString("email"),
                            resultados.getString("password_hash"),
                            resultados.getString("rol"));
                }
            }
        }
        return null;
    }

    /** Crea una cuenta nueva con rol de recepcion definido por el sistema. */
    public boolean crearCuenta(String nombre, String apellido, String correo,
                               String hashContrasena) throws SQLException {
        // Primero evita duplicados y luego guarda la cuenta con la clave ya protegida.
        String consultaSql = "INSERT INTO usuarios (nombre, apellido, email, password_hash, rol) "
                   + "VALUES (?, ?, ?, ?, 'RECEPCION')";

        try (Connection conexion = Conexion.obtener()) {
            conexion.setAutoCommit(false);
            try (PreparedStatement sentencia = conexion.prepareStatement(consultaSql, Statement.RETURN_GENERATED_KEYS)) {
            try (PreparedStatement consultaExistente = conexion.prepareStatement(
                    "SELECT 1 FROM usuarios WHERE LOWER(email) = LOWER(?) LIMIT 1")) {
                consultaExistente.setString(1, correo);
                try (ResultSet resultados = consultaExistente.executeQuery()) {
                    if (resultados.next()) {
                        conexion.rollback();
                        return false;
                    }
                }
            }
            sentencia.setString(1, nombre);
            sentencia.setString(2, apellido);
            sentencia.setString(3, correo);
            sentencia.setString(4, hashContrasena);
            sentencia.executeUpdate();
            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (!claves.next()) throw new SQLException("No se obtuvo el id del usuario creado");
                int idUsuario = claves.getInt(1);
                AuditoriaDAO.registrar(conexion, "CREAR", "Usuarios", Integer.toString(idUsuario),
                        java.util.Map.of(), AuditoriaDAO.capturar(conexion, "usuarios", "idUsuario", idUsuario));
            }
            conexion.commit();
            return true;
            } catch (Exception excepcion) {
                conexion.rollback();
                if (excepcion instanceof SQLException excepcionSql) throw excepcionSql;
                throw new SQLException("No se pudo crear la cuenta", excepcion);
            } finally {
                conexion.setAutoCommit(true);
            }
        } catch (SQLIntegrityConstraintViolationException excepcion) {
            if (excepcion.getErrorCode() == 1062) {
                return false;
            }
            throw excepcion;
        } catch (SQLException excepcion) {
            // Algunos drivers reportan una clave repetida como error SQL general.
            if (excepcion.getErrorCode() == 1062) {
                return false;
            }
            throw excepcion;
        }
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
