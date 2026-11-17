package com.elmolino.dao;

import com.elmolino.util.Conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstracción común para consultas de lectura de los DAO.
 * Encapsula la apertura y cierre de recursos JDBC; cada DAO define cómo
 * convertir una fila en su modelo mediante polimorfismo.
 *
 * @param <T> tipo de entidad que representa el DAO concreto
 */
abstract class BaseDAO<T> {

    @FunctionalInterface
    protected interface ConfiguracionConsulta {
        void aplicar(PreparedStatement sentencia) throws SQLException;
    }

    /** Implementado por cada DAO para mapear una fila de su consulta. */
    protected abstract T mapear(ResultSet resultados) throws SQLException;

    /** Ejecuta una consulta y mapea todas sus filas usando la implementación concreta. */
    protected final List<T> consultarLista(String sql, ConfiguracionConsulta configuracion) throws SQLException {
        List<T> filas = new ArrayList<>();
        try (Connection conexion = Conexion.obtener();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            configuracion.aplicar(sentencia);
            try (ResultSet resultados = sentencia.executeQuery()) {
                while (resultados.next()) filas.add(mapear(resultados));
            }
        }
        return filas;
    }

    /** Ejecuta una consulta que puede devolver cero o una entidad. */
    protected final T consultarUno(String sql, ConfiguracionConsulta configuracion) throws SQLException {
        try (Connection conexion = Conexion.obtener();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            configuracion.aplicar(sentencia);
            try (ResultSet resultados = sentencia.executeQuery()) {
                return resultados.next() ? mapear(resultados) : null;
            }
        }
    }
}
