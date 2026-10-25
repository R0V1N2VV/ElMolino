package com.elmolino.ui;

import com.elmolino.util.Conexion;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

/** Presenta resumenes de reservas, alojamientos, actividades y pagos. */
public class VentanaInformes extends JFrame {

    private final VentanaPrincipal principal;
    private final JLabel estado = new JLabel("Cargando informes...");
    private final JTable reservas = tabla();
    private final JTable alojamientos = tabla();
    private final JTable actividades = tabla();
    private final JTable pagos = tabla();

    public VentanaInformes(VentanaPrincipal principal) {
        super("El Molino - Informes");
        this.principal = principal;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(950, 560);
        setLocationRelativeTo(principal);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Cada pestana tiene su tabla y la barra permite volver o actualizar informes.
        JButton volver = new JButton("← Salir");
        volver.setToolTipText("Salir de esta sección y volver al menú principal");
        volver.setForeground(java.awt.Color.WHITE);
        volver.setBackground(new java.awt.Color(0x4F7358));
        volver.setOpaque(true);
        volver.setContentAreaFilled(true);
        JButton actualizar = new JButton("Actualizar informes");
        volver.addActionListener(evento -> dispose());
        actualizar.addActionListener(evento -> cargarInformes());
        JPanel barra = new JPanel(new BorderLayout());
        barra.add(volver, BorderLayout.WEST);
        barra.add(new JLabel("  Resumen de gestión"), BorderLayout.CENTER);
        barra.add(actualizar, BorderLayout.EAST);

        JTabbedPane pestañas = new JTabbedPane();
        pestañas.addTab("Reservas", panelTabla(reservas));
        pestañas.addTab("Alojamientos", panelTabla(alojamientos));
        pestañas.addTab("Actividades", panelTabla(actividades));
        pestañas.addTab("Pagos", panelTabla(pagos));
        add(barra, BorderLayout.NORTH);
        add(pestañas, BorderLayout.CENTER);
        add(estado, BorderLayout.SOUTH);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent evento) {
                principal.setVisible(true);
                principal.toFront();
            }
        });
        cargarInformes();
    }

    private JTable tabla() {
        // Las tablas de informe son de solo lectura.
        return new JTable(new DefaultTableModel() {
            @Override public boolean isCellEditable(int fila, int columna) { return false; }
        });
    }

    private JPanel panelTabla(JTable tabla) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(FiltroTabla.crear(tabla), BorderLayout.NORTH);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);
        return panel;
    }

    private void cargarInformes() {
        // Inicia una consulta por cada grupo de datos que se muestra en las pestanas.
        cargar(reservas, "SELECT estadoReserva AS Estado, COUNT(*) AS Cantidad, "
                + "COALESCE(SUM(precioTotal),0) AS `Total reservado` FROM reservas GROUP BY estadoReserva ORDER BY estadoReserva");
        cargar(alojamientos, "SELECT a.numero AS Número, a.tipo AS Tipo, a.capacidad AS Capacidad, "
                + "a.estado AS Estado, COUNT(r.idReserva) AS `Reservas futuras` FROM alojamientos a "
                + "LEFT JOIN reservas r ON r.idAlojamiento = a.idAlojamiento AND r.fechaEgreso >= CURRENT_DATE "
                + "AND r.estadoReserva NOT IN ('CANCELADA','CANCELADO') GROUP BY a.idAlojamiento "
                + "ORDER BY a.numero");
        cargar(actividades, "SELECT a.nombre AS Actividad, a.horario AS Horario, a.cupoMax AS Cupo, "
                + "COUNT(i.idInscripcion) AS Inscriptos, GREATEST(a.cupoMax - COUNT(i.idInscripcion),0) AS Disponibles "
                + "FROM Actividad a LEFT JOIN inscripcionActividad i ON i.idActividad = a.idActividad "
                + "GROUP BY a.idActividad ORDER BY a.nombre");
        cargar(pagos, "SELECT DATE_FORMAT(fechaPago, '%Y-%m') AS Mes, metodoPago AS `Medio de pago`, "
                + "COUNT(*) AS Operaciones, SUM(montoTotal) AS Total FROM pago "
                + "GROUP BY DATE_FORMAT(fechaPago, '%Y-%m'), metodoPago ORDER BY Mes DESC");
    }

    private void cargar(JTable tabla, String consultaSql) {
        // Ejecuta cada consulta en segundo plano y coloca sus resultados en la tabla.
        new SwingWorker<DefaultTableModel, Void>() {
            @Override protected DefaultTableModel doInBackground() throws Exception {
                DefaultTableModel modelo = new DefaultTableModel() {
                    @Override public boolean isCellEditable(int fila, int columna) { return false; }
                };
                try (Connection conexion = Conexion.obtener();
                     PreparedStatement sentencia = conexion.prepareStatement(consultaSql);
                     ResultSet resultados = sentencia.executeQuery()) {
                    ResultSetMetaData metadatos = resultados.getMetaData();
                    for (int indice = 1; indice <= metadatos.getColumnCount(); indice++) modelo.addColumn(metadatos.getColumnLabel(indice));
                    while (resultados.next()) {
                        Object[] fila = new Object[metadatos.getColumnCount()];
                        for (int indice = 0; indice < fila.length; indice++) fila[indice] = resultados.getObject(indice + 1);
                        modelo.addRow(fila);
                    }
                }
                return modelo;
            }
            @Override protected void done() {
                try {
                    tabla.setModel(get());
                    estado.setText("Informes actualizados");
                } catch (Exception excepcion) {
                    Throwable causa = excepcion.getCause() == null ? excepcion : excepcion.getCause();
                    estado.setText("No se pudieron cargar los informes");
                    JOptionPane.showMessageDialog(VentanaInformes.this,
                            "Error al consultar los informes:\n" + causa.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
