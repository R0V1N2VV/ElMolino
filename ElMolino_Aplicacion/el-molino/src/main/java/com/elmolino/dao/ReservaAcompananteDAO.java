package com.elmolino.dao;

import com.elmolino.util.Conexion;
import com.elmolino.dao.GestionDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReservaAcompananteDAO {

    // Representa una fila de la relacion entre una reserva y un acompanante.
    public record Vinculo(int idReserva, String reserva, int idAcompanante, String acompanante) { }

    public List<Vinculo> listar() throws SQLException {
        // Une reservas, clientes y acompanantes para mostrar nombres legibles.
        GestionDAO.validarAccesoAcompanantesReserva();
        String consultaSql = "SELECT ra.idReserva, CONCAT('Reserva ', ra.idReserva, ' - ', c.apellido, ', ', c.nombre), "
                + "ra.idAcompañante, CONCAT(a.apellido, ', ', a.nombre, ' - DNI ', a.dni) "
                + "FROM `reservaAcompañante` ra "
                + "JOIN reservas r ON r.idReserva = ra.idReserva "
                + "JOIN cliente c ON c.idCliente = r.idCliente "
                + "JOIN `acompañante` a ON a.idAcompañante = ra.idAcompañante "
                + "ORDER BY ra.idReserva DESC, a.apellido, a.nombre";
        List<Vinculo> vinculos = new ArrayList<>();
        try (Connection conexion = Conexion.obtener();
             PreparedStatement sentencia = conexion.prepareStatement(consultaSql);
             ResultSet resultados = sentencia.executeQuery()) {
            while (resultados.next()) vinculos.add(new Vinculo(resultados.getInt(1), resultados.getString(2), resultados.getInt(3), resultados.getString(4)));
        }
        return vinculos;
    }

    public void agregar(int idReserva, int idAcompanante) throws SQLException {
        // Crea el vinculo con parametros para evitar insertar datos en la consulta.
        GestionDAO.validarAccesoAcompanantesReserva();
        String consultaSql = "INSERT INTO `reservaAcompañante` (idReserva, idAcompañante) VALUES (?, ?)";
        try (Connection conexion = Conexion.obtener(); PreparedStatement sentencia = conexion.prepareStatement(consultaSql)) {
            sentencia.setInt(1, idReserva);
            sentencia.setInt(2, idAcompanante);
            sentencia.executeUpdate();
        }
    }

    public void actualizar(int idReserva, int idAnterior, int idNuevo) throws SQLException {
        // Reemplaza el acompanante asociado a esa reserva.
        GestionDAO.validarAccesoAcompanantesReserva();
        String consultaSql = "UPDATE `reservaAcompañante` SET idAcompañante = ? WHERE idReserva = ? AND idAcompañante = ?";
        try (Connection conexion = Conexion.obtener(); PreparedStatement sentencia = conexion.prepareStatement(consultaSql)) {
            sentencia.setInt(1, idNuevo);
            sentencia.setInt(2, idReserva);
            sentencia.setInt(3, idAnterior);
            sentencia.executeUpdate();
        }
    }

    public void eliminar(int idReserva, int idAcompanante) throws SQLException {
        // Quita solo el vinculo seleccionado; no borra la reserva ni al acompanante.
        GestionDAO.validarAccesoAcompanantesReserva();
        String consultaSql = "DELETE FROM `reservaAcompañante` WHERE idReserva = ? AND idAcompañante = ?";
        try (Connection conexion = Conexion.obtener(); PreparedStatement sentencia = conexion.prepareStatement(consultaSql)) {
            sentencia.setInt(1, idReserva);
            sentencia.setInt(2, idAcompanante);
            sentencia.executeUpdate();
        }
    }
}
