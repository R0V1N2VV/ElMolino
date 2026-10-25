package com.elmolino.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.JTable;
import javax.swing.table.TableRowSorter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.util.regex.Pattern;

/** Agrega una busqueda instantanea que filtra todas las columnas de una tabla. */
final class FiltroTabla {

    private FiltroTabla() { }

    static JPanel crear(JTable tabla) {
        TableRowSorter<?> ordenador = new TableRowSorter<>(tabla.getModel());
        tabla.setRowSorter(ordenador);

        JTextField busqueda = new JTextField(24);
        busqueda.setToolTipText("Escribi para filtrar los registros de esta seccion");
        busqueda.getDocument().addDocumentListener(new DocumentListener() {
            private void filtrar() {
                String texto = busqueda.getText().trim();
                ordenador.setRowFilter(texto.isEmpty()
                        ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(texto)));
            }
            @Override public void insertUpdate(DocumentEvent evento) { filtrar(); }
            @Override public void removeUpdate(DocumentEvent evento) { filtrar(); }
            @Override public void changedUpdate(DocumentEvent evento) { filtrar(); }
        });

        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        panel.add(new JLabel("Buscar:"), BorderLayout.WEST);
        panel.add(busqueda, BorderLayout.CENTER);
        return panel;
    }
}
