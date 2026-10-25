package com.elmolino.ui;

import com.elmolino.dao.GestionDAO;
import com.elmolino.dao.ReservaAcompananteDAO;
import com.elmolino.dao.ReservaAcompananteDAO.Vinculo;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/** Administra el vinculo entre reservas y acompanantes. */
public class VentanaAcompanantesReserva extends JFrame {

    private final VentanaPrincipal principal;
    private final ReservaAcompananteDAO accesoDatos = new ReservaAcompananteDAO();
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"ID reserva", "Reserva / cliente", "ID acompañante", "Acompañante"}, 0) {
        @Override public boolean isCellEditable(int fila, int columna) { return false; }
    };
    private final JTable tabla = new JTable(modelo);
    private final JLabel estado = new JLabel(" ");
    private final JComboBox<GestionDAO.Opcion> acompanantes = new JComboBox<>();

    public VentanaAcompanantesReserva(VentanaPrincipal principal) {
        super("El Molino - Acompañantes por reserva");
        this.principal = principal;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(900, 500);
        setLocationRelativeTo(principal);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // La tabla muestra los vinculos y la barra superior permite mantenerlos.
        JButton volver = new JButton("← Salir");
        volver.setToolTipText("Salir de esta sección y volver al menú principal");
        volver.setForeground(java.awt.Color.WHITE);
        volver.setBackground(new java.awt.Color(0x4F7358));
        volver.setOpaque(true);
        volver.setContentAreaFilled(true);
        JButton editar = new JButton("Editar asociación");
        JButton eliminar = new JButton("Quitar asociación");
        JButton actualizar = new JButton("Actualizar");
        volver.addActionListener(evento -> dispose());
        editar.addActionListener(evento -> editar());
        eliminar.addActionListener(evento -> eliminar());
        actualizar.addActionListener(evento -> cargar());

        JPanel acciones = new JPanel();
        acciones.add(volver);
        acciones.add(editar);
        acciones.add(eliminar);
        acciones.add(actualizar);
        JPanel barra = new JPanel(new BorderLayout());
        barra.add(acciones, BorderLayout.WEST);
        barra.add(FiltroTabla.crear(tabla), BorderLayout.EAST);
        add(barra, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(estado, BorderLayout.SOUTH);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent evento) {
                principal.setVisible(true);
                principal.toFront();
            }
        });
        cargarOpciones();
        cargar();
    }

    private void cargarOpciones() {
        // Carga las opciones de acompanante para el selector de la pantalla.
        try {
            for (GestionDAO.Opcion opcion : GestionDAO.opciones(
                    "SELECT idAcompañante, CONCAT(apellido, ', ', nombre, ' - DNI ', dni) FROM `acompañante` ORDER BY apellido, nombre")) {
                acompanantes.addItem(opcion);
            }
        } catch (Exception excepcion) {
            error("No se pudieron cargar reservas y acompañantes", excepcion);
        }
    }

    private void cargar() {
        // Consulta y muestra todos los vinculos sin bloquear la interfaz.
        estado.setText("Cargando asociaciones...");
        new SwingWorker<List<Vinculo>, Void>() {
            @Override protected List<Vinculo> doInBackground() throws Exception { return accesoDatos.listar(); }
            @Override protected void done() {
                try {
                    List<Vinculo> lista = get();
                    modelo.setRowCount(0);
                    for (Vinculo vinculo : lista) modelo.addRow(new Object[]{vinculo.idReserva(), vinculo.reserva(), vinculo.idAcompanante(), vinculo.acompanante()});
                    estado.setText(lista.size() + " acompañantes asociados");
                } catch (Exception excepcion) { error("No se pudieron cargar las asociaciones", excepcion); }
            }
        }.execute();
    }

    private void editar() {
        // Reemplaza el acompanante asociado a la reserva seleccionada.
        int filaVista = tabla.getSelectedRow();
        if (filaVista < 0) {
            JOptionPane.showMessageDialog(this, "Seleccioná una asociación de la tabla.",
                    "Editar asociación", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int fila = tabla.convertRowIndexToModel(filaVista);
        int idReserva = ((Number) modelo.getValueAt(fila, 0)).intValue();
        int idAnterior = ((Number) modelo.getValueAt(fila, 2)).intValue();
        try {
            acompanantes.removeAllItems();
            for (GestionDAO.Opcion opcion : GestionDAO.opciones(
                    "SELECT idAcompañante, CONCAT(apellido, ', ', nombre, ' - DNI ', dni) FROM `acompañante` ORDER BY apellido, nombre"))
                acompanantes.addItem(opcion);
            for (int indice = 0; indice < acompanantes.getItemCount(); indice++)
                if (acompanantes.getItemAt(indice).id() == idAnterior) acompanantes.setSelectedIndex(indice);
        } catch (Exception excepcion) {
            error("No se pudieron cargar acompañantes", excepcion);
            return;
        }
        if (JOptionPane.showConfirmDialog(this, acompanantes, "Editar acompañante asociado",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        GestionDAO.Opcion nuevo = (GestionDAO.Opcion) acompanantes.getSelectedItem();
        if (nuevo == null) return;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                accesoDatos.actualizar(idReserva, idAnterior, nuevo.id()); return null;
            }
            @Override protected void done() {
                try { get(); cargar(); }
                catch (Exception excepcion) { error("No se pudo editar. Revisá si ya estaba asociado", excepcion); }
            }
        }.execute();
    }

    private void eliminar() {
        // Pide confirmacion antes de quitar el vinculo elegido.
        int filaVista = tabla.getSelectedRow();
        if (filaVista < 0) {
            JOptionPane.showMessageDialog(this, "Seleccioná una asociación de la tabla.",
                    "Quitar asociación", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int fila = tabla.convertRowIndexToModel(filaVista);
        int idReserva = ((Number) modelo.getValueAt(fila, 0)).intValue();
        int idAcompanante = ((Number) modelo.getValueAt(fila, 2)).intValue();
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                accesoDatos.eliminar(idReserva, idAcompanante); return null;
            }
            @Override protected void done() {
                try { get(); cargar(); }
                catch (Exception excepcion) { error("No se pudo quitar la asociación", excepcion); }
            }
        }.execute();
    }

    private void error(String mensaje, Exception excepcion) {
        // Muestra la causa del error para ayudar a identificar el problema.
        Throwable causa = excepcion;
        while (causa.getCause() != null) causa = causa.getCause();
        estado.setText("Ocurrió un error");
        JOptionPane.showMessageDialog(this, mensaje + ":\n" + causa.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
