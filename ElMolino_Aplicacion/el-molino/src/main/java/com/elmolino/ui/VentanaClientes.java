package com.elmolino.ui;

import com.elmolino.dao.ClienteDAO;
import com.elmolino.model.Cliente;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Muestra y permite editar los datos de los clientes.
 */
public class VentanaClientes extends JFrame {

    private final VentanaPrincipal ventanaPrincipal;
    private final ClienteDAO accesoDatosCliente = new ClienteDAO();
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"ID", "DNI", "Apellido", "Nombre", "Teléfono", "Email", "Dirección"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tabla = new JTable(modelo);
    private final JLabel estado = new JLabel(" ");

    public VentanaClientes(VentanaPrincipal ventanaPrincipal) {
        super("El Molino - Clientes");
        this.ventanaPrincipal = ventanaPrincipal;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(900, 500);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // La barra superior agrupa las acciones y la tabla ocupa el espacio principal.
        JButton botonVolver = new JButton("← Salir");
        botonVolver.setToolTipText("Salir de esta sección y volver al menú principal");
        botonVolver.setForeground(java.awt.Color.WHITE);
        botonVolver.setBackground(new java.awt.Color(0x4F7358));
        botonVolver.setOpaque(true);
        botonVolver.setContentAreaFilled(true);
        botonVolver.addActionListener(evento -> volverAlMenu());
        JButton botonActualizar = new JButton("Actualizar");
        JButton botonEditar = new JButton("Editar seleccionado");
        botonActualizar.addActionListener(evento -> cargarClientes());
        botonEditar.addActionListener(evento -> editarSeleccionado());

        JPanel herramientas = new JPanel();
        herramientas.add(botonVolver);
        herramientas.add(botonEditar);
        herramientas.add(botonActualizar);
        JPanel arriba = new JPanel(new BorderLayout(8, 8));
        arriba.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        arriba.add(new JLabel("Clientes registrados"), BorderLayout.NORTH);
        arriba.add(herramientas, BorderLayout.CENTER);

        JPanel abajo = new JPanel(new BorderLayout());
        abajo.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        abajo.add(estado, BorderLayout.WEST);

        add(arriba, BorderLayout.NORTH);
        tabla.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(abajo, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent evento) {
                ventanaPrincipal.setVisible(true);
                ventanaPrincipal.toFront();
            }
        });

        cargarClientes();
    }

    private void volverAlMenu() {
        // Cierra esta pantalla; el evento de cierre vuelve a mostrar el menu principal.
        dispose();
    }

    private void editarSeleccionado() {
        // Copia la fila elegida a un formulario y valida los datos antes de guardarlos.
        int filaVista = tabla.getSelectedRow();
        if (filaVista < 0) {
            JOptionPane.showMessageDialog(this, "Seleccioná un cliente de la tabla.",
                    "Editar cliente", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int fila = tabla.convertRowIndexToModel(filaVista);
        Cliente cliente = new Cliente(
                (int) modelo.getValueAt(fila, 0),
                Integer.parseInt(modelo.getValueAt(fila, 1).toString()),
                modelo.getValueAt(fila, 3).toString(),
                modelo.getValueAt(fila, 2).toString(),
                textoCelda(fila, 4), textoCelda(fila, 5), textoCelda(fila, 6));
        mostrarFormulario(cliente);
    }

    private String textoCelda(int fila, int columna) {
        Object valor = modelo.getValueAt(fila, columna);
        return valor == null ? "" : valor.toString();
    }

    private void mostrarFormulario(Cliente original) {
        // Si no hay cliente original, crea uno nuevo; si existe, guarda sus cambios.
        boolean edicion = original != null;
        JTextField dni = new JTextField(edicion ? String.valueOf(original.getDni()) : "");
        JTextField nombre = new JTextField(edicion ? original.getNombre() : "");
        JTextField apellido = new JTextField(edicion ? original.getApellido() : "");
        JTextField telefono = new JTextField(edicion ? original.getTelefono() : "");
        JTextField correo = new JTextField(edicion ? original.getEmail() : "");
        JTextField direccion = new JTextField(edicion ? original.getDireccion() : "");

        JPanel formulario = new JPanel(new GridLayout(0, 2, 8, 8));
        formulario.add(new JLabel("DNI:")); formulario.add(dni);
        formulario.add(new JLabel("Nombre:")); formulario.add(nombre);
        formulario.add(new JLabel("Apellido:")); formulario.add(apellido);
        formulario.add(new JLabel("Teléfono:")); formulario.add(telefono);
        formulario.add(new JLabel("Email:")); formulario.add(correo);
        formulario.add(new JLabel("Dirección:")); formulario.add(direccion);

        int opcion = JOptionPane.showConfirmDialog(this, formulario,
                edicion ? "Editar cliente" : "Nuevo cliente",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION) {
            return;
        }

        String dniTexto = dni.getText().trim();
        String nombreTexto = nombre.getText().trim();
        String apellidoTexto = apellido.getText().trim();
        String telefonoTexto = telefono.getText().trim();
        if (!dniTexto.matches("\\d{1,10}") || nombreTexto.isEmpty() || apellidoTexto.isEmpty()
                || !telefonoTexto.matches("\\d{1,10}") || correo.getText().trim().isEmpty()
                || direccion.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Completá todos los campos y verificá que DNI y teléfono sean numéricos.",
                    "Revisá los datos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Integer.parseInt(dniTexto);
            Integer.parseInt(telefonoTexto);
        } catch (NumberFormatException excepcion) {
            JOptionPane.showMessageDialog(this, "El DNI y el teléfono no pueden superar el límite numérico permitido.",
                    "Revisá los datos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String correoTexto = correo.getText().trim();
        if (!correoTexto.isEmpty() && !correoTexto.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            JOptionPane.showMessageDialog(this, "El email no tiene un formato válido.",
                    "Revisá los datos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Cliente cliente = new Cliente(edicion ? original.getIdCliente() : 0,
                Integer.parseInt(dniTexto), nombreTexto, apellidoTexto,
                telefonoTexto, correoTexto, direccion.getText().trim());
        estado.setText(edicion ? "Guardando cambios..." : "Creando cliente...");
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                if (edicion) {
                    return accesoDatosCliente.actualizar(cliente);
                }
                accesoDatosCliente.insertar(cliente);
                return true;
            }

            @Override
            protected void done() {
                try {
                    if (!get()) {
                        JOptionPane.showMessageDialog(VentanaClientes.this,
                                "No se encontró el cliente para actualizar.",
                                "Cliente no actualizado", JOptionPane.WARNING_MESSAGE);
                    }
                    cargarClientes();
                } catch (Exception excepcion) {
                    mostrarError("No se pudo guardar el cliente", excepcion);
                }
            }
        }.execute();
    }

    private void eliminarSeleccionado() {
        // Pide confirmacion antes de borrar para evitar una eliminacion por error.
        int filaVista = tabla.getSelectedRow();
        if (filaVista < 0) {
            JOptionPane.showMessageDialog(this, "Seleccioná un cliente de la tabla.",
                    "Eliminar cliente", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int fila = tabla.convertRowIndexToModel(filaVista);
        int id = (int) modelo.getValueAt(fila, 0);
        int confirmar = JOptionPane.showConfirmDialog(this,
                "¿Querés eliminar este cliente?", "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmar != JOptionPane.YES_OPTION) {
            return;
        }

        estado.setText("Eliminando cliente...");
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return accesoDatosCliente.eliminar(id);
            }

            @Override
            protected void done() {
                try {
                    if (!get()) {
                        JOptionPane.showMessageDialog(VentanaClientes.this,
                                "No se encontró el cliente para eliminar.",
                                "Cliente no eliminado", JOptionPane.WARNING_MESSAGE);
                    }
                    cargarClientes();
                } catch (Exception excepcion) {
                    mostrarError("No se pudo eliminar. Puede tener reservas asociadas.", excepcion);
                }
            }
        }.execute();
    }

    private void mostrarError(String mensaje, Exception excepcion) {
        Throwable causa = excepcion;
        while (causa.getCause() != null) {
            causa = causa.getCause();
        }
        estado.setText("Ocurrió un error");
        JOptionPane.showMessageDialog(this, mensaje + ":\n" + causa.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }

    /** Carga clientes en segundo plano para que la ventana no se congele. */
    private void cargarClientes() {
        // Actualiza la tabla y el estado cuando termina la consulta.
        estado.setText("Cargando...");
        new SwingWorker<List<Cliente>, Void>() {
            @Override
            protected List<Cliente> doInBackground() throws Exception {
                return accesoDatosCliente.listarTodos();
            }

            @Override
            protected void done() {
                try {
                    List<Cliente> clientes = get();
                    modelo.setRowCount(0);
                    for (Cliente cliente : clientes) {
                        modelo.addRow(new Object[]{
                                cliente.getIdCliente(), cliente.getDni(), cliente.getApellido(), cliente.getNombre(),
                                cliente.getTelefono(), cliente.getEmail(), cliente.getDireccion()});
                    }
                    estado.setText(clientes.size() + " clientes");
                } catch (Exception excepcion) {
                    // Obtiene el error original para mostrar su detalle.
                    Throwable causa = excepcion;
                    while ((causa instanceof ExecutionException
                            || causa instanceof ExceptionInInitializerError)
                            && causa.getCause() != null) {
                        causa = causa.getCause();
                    }
                    estado.setText("Error al cargar");
                    JOptionPane.showMessageDialog(VentanaClientes.this,
                            "No se pudo conectar o consultar la base:\n" + causa.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
