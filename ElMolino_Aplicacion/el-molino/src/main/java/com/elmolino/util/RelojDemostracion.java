package com.elmolino.util;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

/**
 * Lee el reloj de demostración compartido con la web.
 *
 * La tabla es opcional: si todavía no fue instalada o está desactivada, la
 * aplicación conserva su comportamiento normal y usa el reloj local.
 */
public final class RelojDemostracion {

    /** Fecha y hora que deben emplear las validaciones de una exposición. */
    public record Momento(LocalDate fecha, LocalTime hora) { }

    private RelojDemostracion() { }

    /**
     * Obtiene el reloj activo desde la base. Cada operación lo vuelve a leer,
     * para que la persona que expone pueda modificarlo desde la web sin
     * reiniciar la aplicación Java.
     */
    public static Optional<Momento> obtenerActivo() {
        String sql = "SELECT activo, fecha, hora FROM configuracionDemostracion "
                + "WHERE idConfiguracion=1 LIMIT 1";
        try (Connection conexion = Conexion.obtener();
             PreparedStatement consulta = conexion.prepareStatement(sql);
             ResultSet resultado = consulta.executeQuery()) {
            if (!resultado.next() || !resultado.getBoolean("activo")) return Optional.empty();
            Date fecha = resultado.getDate("fecha");
            Time hora = resultado.getTime("hora");
            if (fecha == null || hora == null) return Optional.empty();
            return Optional.of(new Momento(fecha.toLocalDate(), hora.toLocalTime().withSecond(0).withNano(0)));
        } catch (SQLException error) {
            // Antes de importar BD/13 la tabla no existe. No es un error que
            // deba bloquear el uso normal de la aplicación.
            return Optional.empty();
        }
    }
}
