-- Cupos e inscripciones de actividades.
-- Usar este archivo si BD/actualizacion_actividades.sql ya fue ejecutado antes.
-- Se puede volver a ejecutar: la tabla solo se crea cuando todavía no existe.

USE `s27_El Molino`;

CREATE TABLE IF NOT EXISTS inscripcionActividad (
    idInscripcion INT NOT NULL AUTO_INCREMENT,
    idCliente INT NOT NULL,
    idActividad INT NOT NULL,
    fechaInscripcion DATE NOT NULL,
    asistencia TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (idInscripcion),
    UNIQUE KEY uk_cliente_actividad (idCliente, idActividad),
    KEY idx_inscripcion_cliente (idCliente),
    KEY idx_inscripcion_actividad (idActividad),
    CONSTRAINT fk_inscripcion_cliente
        FOREIGN KEY (idCliente) REFERENCES cliente(idCliente)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_inscripcion_actividad
        FOREIGN KEY (idActividad) REFERENCES Actividad(idActividad)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
