package com.elmolino;

import com.elmolino.ui.VentanaLogin;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.util.Collections;

public class Main {

    public static void main(String[] argumentos) {
        // Prepara FlatLaf y configura colores y letras antes de crear las ventanas.
        FlatLaf.setGlobalExtraDefaults(Collections.singletonMap("@accentColor", "#4F7358"));
        FlatLightLaf.setup();
        aplicarPaletaNatural();

        SwingUtilities.invokeLater(() -> new VentanaLogin().setVisible(true));
    }

    // Define los colores y la letra base que comparten las pantallas.
    private static void aplicarPaletaNatural() {
        Color crema = new Color(0xF4FAF8);
        Color marfil = new Color(0xF0EDE4);
        Color borde = new Color(0xCFE4DC);
        Color petroleo = new Color(0x0F4C5C);
        Color azul = new Color(0x1B6B7A);
        Color verde = new Color(0x2D4A3E);
        Color verdeClaro = new Color(0x4F7358);
        Color naranja = new Color(0xC97B3D);
        java.awt.Dimension pantalla = Toolkit.getDefaultToolkit().getScreenSize();
        float escala = Math.max(0.95f, Math.min(1.4f, Math.min(pantalla.width / 1440f, pantalla.height / 900f)));
        Font fuente = new Font("Segoe UI", Font.PLAIN, Math.round(14 * escala));

        // El fondo claro y el texto verde siguen la identidad visual de El Molino.
        UIManager.put("Panel.background", crema);
        UIManager.put("OptionPane.background", crema);
        UIManager.put("OptionPane.messageForeground", verde);
        UIManager.put("Label.foreground", verde);
        // Los botones usan verde medio para mantener buen contraste sin oscurecer la interfaz.
        UIManager.put("Button.background", verdeClaro);
        UIManager.put("Button.foreground", crema);
        // Mantiene el mismo color al recibir foco, por ejemplo despues de editar un campo.
        UIManager.put("Button.focusedBackground", verdeClaro);
        UIManager.put("Button.default.focusedBackground", verdeClaro);
        UIManager.put("Button.hoverBackground", new Color(0x7FA87A));
        UIManager.put("Button.pressedBackground", verde);
        UIManager.put("Button.focusedBorderColor", new Color(0x8FC1CA));
        UIManager.put("Component.focusColor", new Color(0x4A93A0));
        // El tamano de letra crece segun el tamano de la pantalla.
        UIManager.put("defaultFont", fuente);
        UIManager.put("Label.font", fuente);
        UIManager.put("Button.font", fuente.deriveFont(Font.BOLD));
        UIManager.put("TextField.font", fuente);
        UIManager.put("PasswordField.font", fuente);
        UIManager.put("ComboBox.font", fuente);
        UIManager.put("Table.font", fuente);
        UIManager.put("TableHeader.font", fuente.deriveFont(Font.BOLD));
        UIManager.put("OptionPane.messageFont", fuente);
        UIManager.put("TabbedPane.font", fuente.deriveFont(Font.BOLD));
        UIManager.put("TextField.background", Color.WHITE);
        UIManager.put("TextField.foreground", verde);
        UIManager.put("TextField.caretForeground", petroleo);
        UIManager.put("TextField.selectionBackground", new Color(0x8FC1CA));
        UIManager.put("TextField.selectionForeground", petroleo);
        UIManager.put("PasswordField.background", Color.WHITE);
        UIManager.put("PasswordField.foreground", verde);
        UIManager.put("PasswordField.caretForeground", petroleo);
        UIManager.put("ComboBox.background", Color.WHITE);
        UIManager.put("ComboBox.foreground", verde);
        UIManager.put("Table.background", crema);
        UIManager.put("Table.foreground", verde);
        UIManager.put("Table.alternateRowColor", marfil);
        UIManager.put("Table.selectionBackground", azul);
        UIManager.put("Table.selectionForeground", Color.WHITE);
        UIManager.put("Table.gridColor", borde);
        UIManager.put("TableHeader.background", verde);
        UIManager.put("TableHeader.foreground", crema);
        UIManager.put("TabbedPane.selectedBackground", crema);
        UIManager.put("TabbedPane.underlineColor", naranja);
        UIManager.put("ScrollBar.thumb", new Color(0xB3CBA6));
    }
}
