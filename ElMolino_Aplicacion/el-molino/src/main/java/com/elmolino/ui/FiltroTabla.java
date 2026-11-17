package com.elmolino.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.math.BigDecimal;
import java.text.Collator;
import java.util.Locale;
import java.util.regex.Pattern;

/** Agrega una busqueda instantanea que filtra todas las columnas de una tabla. */
final class FiltroTabla {

    /**
     * Construye el objeto, guarda sus datos iniciales y deja sus componentes preparados para usarse.
     */
    private FiltroTabla() { }

    /**
     * Activa el ordenamiento de una tabla al hacer clic en sus encabezados.
     * Un clic ordena de menor a mayor; el siguiente invierte el orden.
     * Se usa en todas las ventanas para que la búsqueda y el ordenamiento
     * funcionen igual, incluso si una tabla vuelve a cargar su modelo.
     */
    static void habilitarOrdenamiento(JTable tabla) {
        TableRowSorter<TableModel> ordenador = new TableRowSorter<>(tabla.getModel());
        for (int columna = 0; columna < tabla.getModel().getColumnCount(); columna++) {
            ordenador.setComparator(columna, FiltroTabla::compararValores);
        }
        ordenador.setSortsOnUpdates(true);
        tabla.setRowSorter(ordenador);

        JTableHeader encabezado = tabla.getTableHeader();
        if (encabezado != null) {
            encabezado.setToolTipText("Tocá un título para ordenar. Volvé a tocarlo para invertir el orden.");
        }
    }

    /** Compara números y fechas como valores reales; el resto se ordena como texto en español. */
    private static int compararValores(Object primero, Object segundo) {
        if (primero == segundo) return 0;
        if (primero == null) return 1;
        if (segundo == null) return -1;
        if (primero instanceof Number && segundo instanceof Number) {
            return new BigDecimal(primero.toString()).compareTo(new BigDecimal(segundo.toString()));
        }
        if (primero instanceof java.util.Date fechaUno && segundo instanceof java.util.Date fechaDos) {
            return fechaUno.compareTo(fechaDos);
        }
        Collator comparadorTexto = Collator.getInstance(new Locale("es", "AR"));
        comparadorTexto.setStrength(Collator.PRIMARY);
        return comparadorTexto.compare(String.valueOf(primero), String.valueOf(segundo));
    }

    /**
     * Crea el nuevo valor a partir de los datos recibidos.
     */
    static JPanel crear(JTable tabla) {
        return crear(tabla, "Buscar");
    }

    /**
     * Crea un filtro con una indicación concreta para la sección donde se muestra.
     */
    static JPanel crear(JTable tabla, String etiquetaBusqueda) {
        habilitarOrdenamiento(tabla);

        JTextField busqueda = new JTextField(24);
        busqueda.setToolTipText("Escribí para filtrar. También podés tocar los títulos de las columnas para ordenar.");
        busqueda.getDocument().addDocumentListener(new DocumentListener() {
            /**
             * Filtra los datos y conserva solamente los elementos que cumplen los criterios.
             */
            private void filtrar() {
                String texto = busqueda.getText().trim();
                if (tabla.getRowSorter() instanceof TableRowSorter<?> ordenador) {
                    ordenador.setRowFilter(texto.isEmpty()
                            ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(texto)));
                }
            }
            /**
             * Ejecuta la lógica de “insert update” en FiltroTabla.java.
             */
            @Override public void insertUpdate(DocumentEvent evento) { filtrar(); }
            @Override public void removeUpdate(DocumentEvent evento) { filtrar(); }
            @Override public void changedUpdate(DocumentEvent evento) { filtrar(); }
        });

        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        panel.add(new JLabel(etiquetaBusqueda + " · ordená tocando los títulos:"), BorderLayout.WEST);
        panel.add(busqueda, BorderLayout.CENTER);
        return panel;
    }
}
