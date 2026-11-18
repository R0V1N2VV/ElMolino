package com.elmolino.dao;

import com.elmolino.model.Cliente;
import com.elmolino.dao.GestionDAO;
import com.elmolino.util.Conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Acceso a los datos de clientes. Las consultas usan parametros preparados.
 */
public class ClienteDAO extends BaseDAO<Cliente> {

    @Override
    protected Cliente mapear(ResultSet resultados) throws SQLException {
        return new Cliente(resultados.getInt("idCliente"), resultados.getInt("dni"),
                resultados.getString("nombre"), resultados.getString("apellido"),
                resultados.getString("telefono"), resultados.getString("email"),
                resultados.getString("direccion"));
    }

    /**
     * Carga todos que necesita esta parte del sistema.
     */
    public List<Cliente> listarTodos() throws SQLException {
        // Comprueba el rol y devuelve los clientes ordenados por apellido y nombre.
        GestionDAO.validarAccesoClientes();
        String consultaSql = "SELECT idCliente, dni, nombre, apellido, telefono, email, direccion "
                   + "FROM cliente ORDER BY apellido, nombre";

        return consultarLista(consultaSql, sentencia -> { });
    }

    /**
     * Actualiza la información visible para reflejar el estado más reciente.
     */
    public boolean actualizar(Cliente cliente) throws SQLException {
        // Cambia los datos del cliente indicado y devuelve si se actualizo una fila.
        GestionDAO.validarAccesoClientes();
        GestionDAO.validarPermisoEscritura("cliente", "Clientes", GestionDAO.Operacion.EDITAR);
        String consultaSql = "UPDATE cliente SET dni = ?, nombre = ?, apellido = ?, telefono = ?, "
                   + "email = ?, direccion = ? WHERE idCliente = ?";
        try (Connection conexion = Conexion.obtener()) {
            conexion.setAutoCommit(false);
            try {
                var anterior = AuditoriaDAO.capturar(conexion, "cliente", "idCliente", cliente.getIdCliente());
                String emailAnterior = String.valueOf(anterior.getOrDefault("email", ""));
                if (!emailAnterior.equalsIgnoreCase(cliente.getEmail())) {
                    validarEmailDisponible(conexion, cliente.getEmail(), emailAnterior, cliente.getIdCliente());
                }
                int filas;
                try (PreparedStatement sentencia = conexion.prepareStatement(consultaSql)) {
                    sentencia.setInt(1, cliente.getDni());
                    sentencia.setString(2, cliente.getNombre());
                    sentencia.setString(3, cliente.getApellido());
                    sentencia.setString(4, cliente.getTelefono());
                    sentencia.setString(5, cliente.getEmail());
                    sentencia.setString(6, cliente.getDireccion());
                    sentencia.setInt(7, cliente.getIdCliente());
                    filas = sentencia.executeUpdate();
                }
                if (filas > 0) {
                    if (!emailAnterior.isBlank() && !emailAnterior.equalsIgnoreCase(cliente.getEmail())) {
                        String roles = String.join(",", java.util.Collections.nCopies(UsuarioDAO.ROLES_EMPLEADO.size(), "?"));
                        try (PreparedStatement sincronizar = conexion.prepareStatement(
                                "UPDATE usuarios SET email=? WHERE LOWER(email)=LOWER(?) AND UPPER(rol) NOT IN (" + roles + ")")) {
                            sincronizar.setString(1, cliente.getEmail());
                            sincronizar.setString(2, emailAnterior);
                            int indice = 3;
                            for (String rol : UsuarioDAO.ROLES_EMPLEADO) sincronizar.setString(indice++, rol);
                            sincronizar.executeUpdate();
                        }
                    }
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

    /** Evita asignar al perfil un correo que pertenezca a otro cliente o a una cuenta del personal. */
    private static void validarEmailDisponible(Connection conexion, String nuevo, String anterior, int idCliente)
            throws SQLException {
        try (PreparedStatement cliente = conexion.prepareStatement(
                "SELECT 1 FROM cliente WHERE LOWER(email)=LOWER(?) AND idCliente<>? LIMIT 1")) {
            cliente.setString(1, nuevo); cliente.setInt(2, idCliente);
            try (ResultSet resultado = cliente.executeQuery()) {
                if (resultado.next()) throw new SQLException("Ese email ya está registrado en otro cliente.");
            }
        }
        try (PreparedStatement usuario = conexion.prepareStatement(
                "SELECT 1 FROM usuarios WHERE LOWER(email)=LOWER(?) AND LOWER(email)<>LOWER(?) LIMIT 1")) {
            usuario.setString(1, nuevo); usuario.setString(2, anterior);
            try (ResultSet resultado = usuario.executeQuery()) {
                if (resultado.next()) throw new SQLException("Ese email ya está usado por otra cuenta de la aplicación.");
            }
        }
    }

    /**
     * Elimina el elemento indicado del sistema.
     */
}
