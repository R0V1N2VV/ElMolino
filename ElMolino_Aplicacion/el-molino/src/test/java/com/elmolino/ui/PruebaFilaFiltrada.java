package com.elmolino.ui;

import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/** Comprobación sin base de datos: una fila filtrada conserva el ID de su registro real. */
public final class PruebaFilaFiltrada {
    private PruebaFilaFiltrada() { }

    public static void main(String[] args) {
        DefaultTableModel modelo = new DefaultTableModel(new Object[]{"ID", "Nombre"}, 0);
        modelo.addRow(new Object[]{101, "Ana"});
        modelo.addRow(new Object[]{202, "Beatriz"});
        JTable tabla = new JTable(modelo);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modelo);
        tabla.setRowSorter(sorter);
        sorter.setRowFilter(RowFilter.regexFilter("Beatriz", 1));
        int filaVista = 0;
        int filaModelo = tabla.convertRowIndexToModel(filaVista);
        if (!Integer.valueOf(202).equals(modelo.getValueAt(filaModelo, 0))) {
            throw new AssertionError("El filtro devolvió el ID de otra fila.");
        }
        System.out.println("OK: el ID de la fila filtrada coincide.");
    }
}
