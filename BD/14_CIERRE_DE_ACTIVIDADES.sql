-- El Molino · actualización 14
-- Registra el resultado de CADA jornada de una actividad recurrente.
-- Ejecutar una vez en phpMyAdmin, con la base s27_El Molino seleccionada.
-- No borra actividades, inscripciones ni historial existente.

USE `s27_El Molino`;

CREATE TABLE IF NOT EXISTS `cierreActividad` (
  `idCierreActividad` INT NOT NULL AUTO_INCREMENT,
  `idActividad` INT NOT NULL,
  `fechaActividad` DATE NOT NULL,
  `estado` ENUM('REALIZADA','SUSPENDIDA') NOT NULL,
  `motivo` VARCHAR(500) DEFAULT NULL,
  `cerradoEn` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `cerradoPor` INT DEFAULT NULL,
  PRIMARY KEY (`idCierreActividad`),
  UNIQUE KEY `uk_cierre_actividad_fecha` (`idActividad`, `fechaActividad`),
  KEY `idx_cierre_actividad_fecha` (`idActividad`, `fechaActividad`),
  KEY `idx_cierre_responsable` (`cerradoPor`),
  CONSTRAINT `fk_cierre_actividad`
    FOREIGN KEY (`idActividad`) REFERENCES `Actividad` (`idActividad`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_cierre_usuario`
    FOREIGN KEY (`cerradoPor`) REFERENCES `usuarios` (`idUsuario`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Verificación: en una base sin cierres todavía debe devolver cero filas.
SELECT COUNT(*) AS cierres_registrados FROM `cierreActividad`;
