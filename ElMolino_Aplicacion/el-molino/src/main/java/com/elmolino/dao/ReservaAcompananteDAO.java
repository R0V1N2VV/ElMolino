package com.elmolino.dao;

import com.elmolino.util.Conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReservaAcompananteDAO {

    public record Vinculo(int idReserva, String reserva, int idAcompanante, String acompanante) { }

    public List<Vinculo> listar() throws SQLException {
        String sql = "SELECT ra.idReserva, CONCAT('Reserva ', ra.idReserva, ' - ', c.apellido, ', ', c.nombre), "
                + "ra.idAcompañante, CONCAT(a.apellido, ', ', a.nombre, ' - DNI ', a.dni) "
                + "FROM `reservaAcompañante` ra "
                + "JOIN reservas r ON r.idReserva = ra.idReserva "
                + "JOIN cliente c ON c.idCliente = r.idCliente "
                + "JOIN `acompañante` a ON a.idAcompañante = ra.idAcompañante "
                + "ORDER BY ra.idReserva DESC, a.apellido, a.nombre";
        List<Vinculo> vinculos = new ArrayList<>();
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) vinculos.add(new Vinculo(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getString(4)));
        }
        return vinculos;
    }

    public void agregar(int idReserva, int idAcompanante) throws SQLException {
        String sql = "INSERT INTO `reservaAcompañante` (idReserva, idAcompañante) VALUES (?, ?)";
        try (Connection con = Conexion.obtener(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idReserva);
            ps.setInt(2, idAcompanante);
            ps.executeUpdate();
        }
    }

    public void actualizar(int idReserva, int idAnterior, int idNuevo) throws SQLException {
        String sql = "UPDATE `reservaAcompañante` SET idAcompañante = ? WHERE idReserva = ? AND idAcompañante = ?";
        try (Connection con = Conexion.obtener(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idNuevo);
            ps.setInt(2, idReserva);
            ps.setInt(3, idAnterior);
            ps.executeUpdate();
        }
    }

    public void eliminar(int idReserva, int idAcompanante) throws SQLException {
        String sql = "DELETE FROM `reservaAcompañante` WHERE idReserva = ? AND idAcompañante = ?";
        try (Connection con = Conexion.obtener(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idReserva);
            ps.setInt(2, idAcompanante);
            ps.executeUpdate();
        }
    }
}
