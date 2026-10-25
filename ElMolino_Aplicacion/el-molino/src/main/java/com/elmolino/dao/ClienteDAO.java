package com.elmolino.dao;

import com.elmolino.model.Cliente;
import com.elmolino.dao.GestionDAO;
import com.elmolino.util.Conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a los datos de clientes. Las consultas usan parametros preparados.
 */
public class ClienteDAO {

    public List<Cliente> listarTodos() throws SQLException {
        // Comprueba el rol y devuelve los clientes ordenados por apellido y nombre.
        GestionDAO.validarAccesoClientes();
        String consultaSql = "SELECT idCliente, dni, nombre, apellido, telefono, email, direccion "
                   + "FROM cliente ORDER BY apellido, nombre";

        List<Cliente> lista = new ArrayList<>();
        try (Connection conexion = Conexion.obtener();
             PreparedStatement sentencia = conexion.prepareStatement(consultaSql);
             ResultSet resultados = sentencia.executeQuery()) {

            while (resultados.next()) {
                lista.add(new Cliente(
                        resultados.getInt("idCliente"),
                        resultados.getInt("dni"),
                        resultados.getString("nombre"),
                        resultados.getString("apellido"),
                        resultados.getString("telefono"),
                        resultados.getString("email"),
                        resultados.getString("direccion")));
            }
        }
        return lista;
    }

    /** Inserta un cliente y devuelve el identificador generado por la base. */
    public int insertar(Cliente cliente) throws SQLException {
        // Los valores se envian por parametros para separar datos y consulta SQL.
        GestionDAO.validarAccesoClientes();
        String consultaSql = "INSERT INTO cliente (dni, nombre, apellido, telefono, email, direccion) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conexion = Conexion.obtener()) {
            conexion.setAutoCommit(false);
            try {
                int idCliente;
                try (PreparedStatement sentencia = conexion.prepareStatement(consultaSql, Statement.RETURN_GENERATED_KEYS)) {
                    sentencia.setInt(1, cliente.getDni());
                    sentencia.setString(2, cliente.getNombre());
                    sentencia.setString(3, cliente.getApellido());
                    sentencia.setInt(4, Integer.parseInt(cliente.getTelefono()));
                    sentencia.setString(5, cliente.getEmail());
                    sentencia.setString(6, cliente.getDireccion());
                    sentencia.executeUpdate();
                    try (ResultSet clavesGeneradas = sentencia.getGeneratedKeys()) {
                        if (!clavesGeneradas.next()) throw new SQLException("No se obtuvo el id del cliente insertado");
                        idCliente = clavesGeneradas.getInt(1);
                    }
                }
                AuditoriaDAO.registrar(conexion, "CREAR", "Clientes", Integer.toString(idCliente),
                        java.util.Map.of(), AuditoriaDAO.capturar(conexion, "cliente", "idCliente", idCliente));
                conexion.commit();
                return idCliente;
            } catch (Exception excepcion) {
                conexion.rollback();
                if (excepcion instanceof SQLException excepcionSql) throw excepcionSql;
                throw new SQLException("No se pudo crear el cliente", excepcion);
            } finally {
                conexion.setAutoCommit(true);
            }
        }
    }

    public boolean actualizar(Cliente cliente) throws SQLException {
        // Cambia los datos del cliente indicado y devuelve si se actualizo una fila.
        GestionDAO.validarAccesoClientes();
        String consultaSql = "UPDATE cliente SET dni = ?, nombre = ?, apellido = ?, telefono = ?, "
                   + "email = ?, direccion = ? WHERE idCliente = ?";
        try (Connection conexion = Conexion.obtener()) {
            conexion.setAutoCommit(false);
            try {
                var anterior = AuditoriaDAO.capturar(conexion, "cliente", "idCliente", cliente.getIdCliente());
                int filas;
                try (PreparedStatement sentencia = conexion.prepareStatement(consultaSql)) {
                    sentencia.setInt(1, cliente.getDni());
                    sentencia.setString(2, cliente.getNombre());
                    sentencia.setString(3, cliente.getApellido());
                    sentencia.setInt(4, Integer.parseInt(cliente.getTelefono()));
                    sentencia.setString(5, cliente.getEmail());
                    sentencia.setString(6, cliente.getDireccion());
                    sentencia.setInt(7, cliente.getIdCliente());
                    filas = sentencia.executeUpdate();
                }
                if (filas > 0) {
                    AuditoriaDAO.registrar(conexion, "EDITAR", "Clientes", Integer.toString(cliente.getIdCliente()),
                            anterior, AuditoriaDAO.capturar(conexion, "cliente", "idCliente", cliente.getIdCliente()));
                }
                conexion.commit();
                return filas > 0;
            } catch (Exception excepcion) {
                conexion.rollback();
                if (excepcion instanceof SQLException excepcionSql) throw excepcionSql;
                throw new SQLException("No se pudo actualizar el cliente", excepcion);
            } finally {
                conexion.setAutoCommit(true);
            }
        }
    }

    public boolean eliminar(int idCliente) throws SQLException {
        // Elimina el cliente solo si la base permite borrar sus datos relacionados.
        GestionDAO.validarAccesoClientes();
        String consultaSql = "DELETE FROM cliente WHERE idCliente = ?";
        try (Connection conexion = Conexion.obtener()) {
            conexion.setAutoCommit(false);
            try {
                var anterior = AuditoriaDAO.capturar(conexion, "cliente", "idCliente", idCliente);
                int filas;
                try (PreparedStatement sentencia = conexion.prepareStatement(consultaSql)) {
                    sentencia.setInt(1, idCliente);
                    filas = sentencia.executeUpdate();
                }
                if (filas > 0) AuditoriaDAO.registrar(conexion, "ELIMINAR", "Clientes",
                        Integer.toString(idCliente), anterior, java.util.Map.of());
                conexion.commit();
                return filas > 0;
            } catch (Exception excepcion) {
                conexion.rollback();
                if (excepcion instanceof SQLException excepcionSql) throw excepcionSql;
                throw new SQLException("No se pudo eliminar el cliente", excepcion);
            } finally {
                conexion.setAutoCommit(true);
            }
        }
    }
}
