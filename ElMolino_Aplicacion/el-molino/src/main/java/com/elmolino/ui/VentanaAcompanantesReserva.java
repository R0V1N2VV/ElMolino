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

/** Administra la relación entre acompañantes y reservas (tabla puente). */
public class VentanaAcompanantesReserva extends JFrame {

    private final VentanaPrincipal principal;
    private final ReservaAcompananteDAO dao = new ReservaAcompananteDAO();
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

        JButton volver = new JButton("← Volver al menú");
        JButton editar = new JButton("Editar asociación");
        JButton eliminar = new JButton("Quitar asociación");
        JButton actualizar = new JButton("Actualizar");
        volver.addActionListener(e -> dispose());
        editar.addActionListener(e -> editar());
        eliminar.addActionListener(e -> eliminar());
        actualizar.addActionListener(e -> cargar());

        JPanel barra = new JPanel();
        barra.add(volver);
        barra.add(editar);
        barra.add(eliminar);
        barra.add(actualizar);
        add(barra, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(estado, BorderLayout.SOUTH);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                principal.setVisible(true);
                principal.toFront();
            }
        });
        cargarOpciones();
        cargar();
    }

    private void cargarOpciones() {
        try {
            for (GestionDAO.Opcion opcion : GestionDAO.opciones(
                    "SELECT idAcompañante, CONCAT(apellido, ', ', nombre, ' - DNI ', dni) FROM `acompañante` ORDER BY apellido, nombre")) {
                acompanantes.addItem(opcion);
            }
        } catch (Exception ex) {
            error("No se pudieron cargar reservas y acompañantes", ex);
        }
    }

    private void cargar() {
        estado.setText("Cargando asociaciones...");
        new SwingWorker<List<Vinculo>, Void>() {
            @Override protected List<Vinculo> doInBackground() throws Exception { return dao.listar(); }
            @Override protected void done() {
                try {
                    List<Vinculo> lista = get();
                    modelo.setRowCount(0);
                    for (Vinculo v : lista) modelo.addRow(new Object[]{v.idReserva(), v.reserva(), v.idAcompanante(), v.acompanante()});
                    estado.setText(lista.size() + " acompañantes asociados");
                } catch (Exception ex) { error("No se pudieron cargar las asociaciones", ex); }
            }
        }.execute();
    }

    private void editar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccioná una asociación de la tabla.",
                    "Editar asociación", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int idReserva = ((Number) modelo.getValueAt(fila, 0)).intValue();
        int idAnterior = ((Number) modelo.getValueAt(fila, 2)).intValue();
        try {
            acompanantes.removeAllItems();
            for (GestionDAO.Opcion opcion : GestionDAO.opciones(
                    "SELECT idAcompañante, CONCAT(apellido, ', ', nombre, ' - DNI ', dni) FROM `acompañante` ORDER BY apellido, nombre"))
                acompanantes.addItem(opcion);
            for (int i = 0; i < acompanantes.getItemCount(); i++)
                if (acompanantes.getItemAt(i).id() == idAnterior) acompanantes.setSelectedIndex(i);
        } catch (Exception ex) {
            error("No se pudieron cargar acompañantes", ex);
            return;
        }
        if (JOptionPane.showConfirmDialog(this, acompanantes, "Editar acompañante asociado",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        GestionDAO.Opcion nuevo = (GestionDAO.Opcion) acompanantes.getSelectedItem();
        if (nuevo == null) return;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                dao.actualizar(idReserva, idAnterior, nuevo.id()); return null;
            }
            @Override protected void done() {
                try { get(); cargar(); }
                catch (Exception ex) { error("No se pudo editar. Revisá si ya estaba asociado", ex); }
            }
        }.execute();
    }

    private void eliminar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccioná una asociación de la tabla.",
                    "Quitar asociación", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int idReserva = ((Number) modelo.getValueAt(fila, 0)).intValue();
        int idAcompanante = ((Number) modelo.getValueAt(fila, 2)).intValue();
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                dao.eliminar(idReserva, idAcompanante); return null;
            }
            @Override protected void done() {
                try { get(); cargar(); }
                catch (Exception ex) { error("No se pudo quitar la asociación", ex); }
            }
        }.execute();
    }

    private void error(String mensaje, Exception ex) {
        Throwable causa = ex;
        while (causa.getCause() != null) causa = causa.getCause();
        estado.setText("Ocurrió un error");
        JOptionPane.showMessageDialog(this, mensaje + ":\n" + causa.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
