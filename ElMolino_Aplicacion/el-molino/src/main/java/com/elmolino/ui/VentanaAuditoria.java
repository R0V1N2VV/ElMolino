package com.elmolino.ui;

import com.elmolino.dao.AuditoriaDAO;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Presenta el historial de cambios en modo de solo lectura. */
public class VentanaAuditoria extends JFrame {

    private final VentanaPrincipal principal;
    private final DefaultTableModel modelo = new DefaultTableModel(new String[]{
            "Fecha y hora", "Usuario", "Rol", "Acción", "Sección", "ID afectado", "Antes", "Después"}, 0) {
        @Override public boolean isCellEditable(int fila, int columna) { return false; }
    };
    private final JTable tabla = new JTable(modelo);
    private final JLabel estado = new JLabel(" ");

    public VentanaAuditoria(VentanaPrincipal principal) {
        super("El Molino - Registro de auditoría");
        this.principal = principal;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1100, 600);
        setLocationRelativeTo(principal);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        JButton salir = new JButton("← Salir");
        salir.addActionListener(evento -> dispose());
        JButton actualizar = new JButton("Actualizar");
        actualizar.addActionListener(evento -> cargar());
        JButton verInformacion = new JButton("Ver información");
        verInformacion.setToolTipText("Abrí los detalles completos del cambio seleccionado");
        verInformacion.addActionListener(evento -> mostrarInformacion());
        tabla.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent evento) {
                if (evento.getClickCount() == 2 && tabla.rowAtPoint(evento.getPoint()) >= 0) {
                    tabla.setRowSelectionInterval(tabla.rowAtPoint(evento.getPoint()), tabla.rowAtPoint(evento.getPoint()));
                    mostrarInformacion();
                }
            }
        });
        JPanel acciones = new JPanel();
        acciones.add(salir);
        acciones.add(actualizar);
        acciones.add(verInformacion);
        JPanel barra = new JPanel(new BorderLayout());
        barra.add(new JLabel("Historial de cambios · solo lectura"), BorderLayout.NORTH);
        barra.add(acciones, BorderLayout.WEST);
        barra.add(FiltroTabla.crear(tabla), BorderLayout.EAST);

        add(barra, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(estado, BorderLayout.SOUTH);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent evento) {
                principal.setVisible(true);
                principal.toFront();
            }
        });
        cargar();
    }

    private void mostrarInformacion() {
        int filaVista = tabla.getSelectedRow();
        if (filaVista < 0) {
            JOptionPane.showMessageDialog(this, "Seleccioná un cambio de la tabla.",
                    "Ver información", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int fila = tabla.convertRowIndexToModel(filaVista);
        String fecha = textoCelda(fila, 0);
        String usuario = textoCelda(fila, 1);
        String rol = textoCelda(fila, 2);
        String accion = textoCelda(fila, 3);
        String seccion = textoCelda(fila, 4);
        String identificador = textoCelda(fila, 5);
        String anterior = formatearJson(textoCelda(fila, 6));
        String nuevo = formatearJson(textoCelda(fila, 7));

        JDialog dialogo = new JDialog(this, "Detalle del cambio", true);
        dialogo.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JPanel contenido = new JPanel(new BorderLayout(8, 8));
        contenido.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JPanel datos = new JPanel(new GridLayout(0, 2, 8, 5));
        datos.add(new JLabel("Fecha y hora: " + fecha));
        datos.add(new JLabel("Usuario: " + usuario + " (" + rol + ")"));
        datos.add(new JLabel("Acción: " + accion));
        datos.add(new JLabel("Sección: " + seccion));
        datos.add(new JLabel("ID afectado: " + identificador));
        datos.add(new JLabel(" "));

        JTabbedPane pestañas = new JTabbedPane();
        pestañas.addTab("Antes", crearPanelTexto(anterior, "No hay datos anteriores para este registro."));
        pestañas.addTab("Después", crearPanelTexto(nuevo, "No hay datos nuevos para este registro."));
        JButton cerrar = new JButton("Cerrar");
        cerrar.addActionListener(evento -> dialogo.dispose());
        JPanel pie = new JPanel();
        pie.add(cerrar);
        contenido.add(datos, BorderLayout.NORTH);
        contenido.add(pestañas, BorderLayout.CENTER);
        contenido.add(pie, BorderLayout.SOUTH);
        dialogo.setContentPane(contenido);
        dialogo.setSize(900, 620);
        dialogo.setMinimumSize(new java.awt.Dimension(650, 420));
        dialogo.setLocationRelativeTo(this);
        dialogo.setVisible(true);
    }

    private JPanel crearPanelTexto(String texto, String textoVacio) {
        JTextArea area = new JTextArea(texto.isBlank() ? textoVacio : texto);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, area.getFont().getSize()));
        area.setCaretPosition(0);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        return panel;
    }

    private String textoCelda(int fila, int columna) {
        Object valor = modelo.getValueAt(fila, columna);
        return valor == null ? "" : valor.toString();
    }

    private String formatearJson(String json) {
        if (json.isBlank()) return "";
        StringBuilder formato = new StringBuilder();
        int nivel = 0;
        boolean dentroTexto = false;
        boolean escape = false;
        for (int indice = 0; indice < json.length(); indice++) {
            char caracter = json.charAt(indice);
            if (dentroTexto) {
                formato.append(caracter);
                if (escape) escape = false;
                else if (caracter == '\\') escape = true;
                else if (caracter == '"') dentroTexto = false;
                continue;
            }
            switch (caracter) {
                case '"' -> { dentroTexto = true; formato.append(caracter); }
                case '{', '[' -> {
                    formato.append(caracter);
                    if (indice + 1 < json.length() && json.charAt(indice + 1) != '}' && json.charAt(indice + 1) != ']') {
                        formato.append('\n');
                        nivel++;
                        formato.append("  ".repeat(nivel));
                    }
                }
                case '}', ']' -> {
                    if (indice > 0 && json.charAt(indice - 1) != '{' && json.charAt(indice - 1) != '[') {
                        formato.append('\n');
                        nivel = Math.max(0, nivel - 1);
                        formato.append("  ".repeat(nivel));
                    }
                    formato.append(caracter);
                }
                case ',' -> { formato.append(",\n").append("  ".repeat(nivel)); }
                case ':' -> formato.append(": ");
                default -> { if (!Character.isWhitespace(caracter)) formato.append(caracter); }
            }
        }
        return formato.toString();
    }

    private void cargar() {
        estado.setText("Cargando historial...");
        new SwingWorker<List<Object[]>, Void>() {
            @Override protected List<Object[]> doInBackground() throws Exception {
                return AuditoriaDAO.listar();
            }
            @Override protected void done() {
                try {
                    List<Object[]> filas = get();
                    modelo.setRowCount(0);
                    for (Object[] fila : filas) modelo.addRow(fila);
                    estado.setText(filas.size() + " cambios registrados");
                } catch (InterruptedException excepcion) {
                    Thread.currentThread().interrupt();
                    estado.setText("Se interrumpió la consulta");
                } catch (ExecutionException excepcion) {
                    Throwable causa = excepcion.getCause() == null ? excepcion : excepcion.getCause();
                    estado.setText("No se pudo cargar el historial");
                    JOptionPane.showMessageDialog(VentanaAuditoria.this,
                            "No se pudo consultar el registro de auditoría:\n" + causa.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
