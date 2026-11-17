package com.elmolino.util;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/** Limita intentos consecutivos por correo durante la ejecución de la aplicación. */
public final class LimiteIntentosLogin {
    private static final int MAX_INTENTOS = 5;
    private static final Duration BLOQUEO = Duration.ofMinutes(15);
    private static final ConcurrentHashMap<String, Estado> ESTADOS = new ConcurrentHashMap<>();

    private LimiteIntentosLogin() { }

    public static Duration tiempoRestante(String correo) {
        Estado estado = ESTADOS.get(normalizar(correo));
        if (estado == null || estado.bloqueadoHasta == null) return Duration.ZERO;
        Duration restante = Duration.between(Instant.now(), estado.bloqueadoHasta);
        if (restante.isNegative() || restante.isZero()) {
            ESTADOS.remove(normalizar(correo), estado);
            return Duration.ZERO;
        }
        return restante;
    }

    public static void registrarFallo(String correo) {
        String clave = normalizar(correo);
        ESTADOS.compute(clave, (ignorada, anterior) -> {
            Estado estado = anterior == null ? new Estado(0, null) : anterior;
            if (estado.bloqueadoHasta != null && Instant.now().isBefore(estado.bloqueadoHasta)) return estado;
            int intentos = estado.intentos + 1;
            return intentos >= MAX_INTENTOS
                    ? new Estado(0, Instant.now().plus(BLOQUEO))
                    : new Estado(intentos, null);
        });
    }

    public static void registrarExito(String correo) { ESTADOS.remove(normalizar(correo)); }

    private static String normalizar(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private record Estado(int intentos, Instant bloqueadoHasta) { }
}
