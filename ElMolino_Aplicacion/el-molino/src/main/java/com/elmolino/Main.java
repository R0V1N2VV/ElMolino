package com.elmolino;

import com.elmolino.ui.VentanaLogin;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.SwingUtilities;
import java.util.Collections;

public class Main {

    public static void main(String[] args) {
        // Verde como color de acento, en linea con la paleta que pidio el cliente
        FlatLaf.setGlobalExtraDefaults(Collections.singletonMap("@accentColor", "#2E7D5B"));
        FlatLightLaf.setup();

        SwingUtilities.invokeLater(() -> new VentanaLogin().setVisible(true));
    }
}
