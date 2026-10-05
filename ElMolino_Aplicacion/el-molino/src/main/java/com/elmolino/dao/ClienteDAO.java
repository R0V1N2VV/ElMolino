package com.elmolino.dao;

import com.elmolino.model.Cliente;
import com.elmolino.util.Conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a la tabla cliente. Todas las consultas usan PreparedStatement.
 * Los demas DAO (AlojamientoDAO, ReservaDAO, etc.) siguen este mismo molde.
 */
public class ClienteDAO {

    public List<Cliente> listarTodos() throws SQLException {
        String sql = "SELECT idCliente, dni, nombre, apellido, telefono, email, direccion "
                   + "FROM cliente ORDER BY apellido, nombre";

        List<Cliente> lista = new ArrayList<>();
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(new Cliente(
                        rs.getInt("idCliente"),
                        rs.getInt("dni"),
                        rs.getString("nombre"),
                        rs.getString("apellido"),
                        rs.getString("telefono"),
                        rs.getString("email"),
                        rs.getString("direccion")));
            }
        }
        return lista;
    }

    /** Inserta un cliente y devuelve el id que le asigno la base. */
    public int insertar(Cliente c) throws SQLException {
        String sql = "INSERT INTO cliente (dni, nombre, apellido, telefono, email, direccion) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, c.getDni());
            ps.setString(2, c.getNombre());
            ps.setString(3, c.getApellido());
            ps.setInt(4, Integer.parseInt(c.getTelefono()));
            ps.setString(5, c.getEmail());
            ps.setString(6, c.getDireccion());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("No se obtuvo el id del cliente insertado");
    }

    public boolean actualizar(Cliente c) throws SQLException {
        String sql = "UPDATE cliente SET dni = ?, nombre = ?, apellido = ?, telefono = ?, "
                   + "email = ?, direccion = ? WHERE idCliente = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, c.getDni());
            ps.setString(2, c.getNombre());
            ps.setString(3, c.getApellido());
            ps.setInt(4, Integer.parseInt(c.getTelefono()));
            ps.setString(5, c.getEmail());
            ps.setString(6, c.getDireccion());
            ps.setInt(7, c.getIdCliente());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(int idCliente) throws SQLException {
        String sql = "DELETE FROM cliente WHERE idCliente = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            return ps.executeUpdate() > 0;
        }
    }
}
