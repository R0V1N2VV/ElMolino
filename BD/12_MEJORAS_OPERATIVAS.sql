-- El Molino · actualización 12
-- Ejecutar UNA sola vez en phpMyAdmin, seleccionando la base s27_El Molino.
-- Esta actualización agrega la lista de espera de actividades. No borra
-- reservas, pagos, usuarios ni inscripciones existentes.

USE `s27_El Molino`;

CREATE TABLE IF NOT EXISTS `listaEsperaActividad` (
  `idListaEspera` INT NOT NULL AUTO_INCREMENT,
  `idActividad` INT NOT NULL,
  `idCliente` INT NOT NULL,
  `fechaActividad` DATE NOT NULL,
  `cantidadPersonas` INT NOT NULL DEFAULT 1,
  `estado` VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
  `creadoEn` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `notificadoEn` DATETIME DEFAULT NULL,
  `canceladaEn` DATETIME DEFAULT NULL,
  PRIMARY KEY (`idListaEspera`),
  UNIQUE KEY `uk_lista_espera_cliente_actividad_fecha` (`idCliente`, `idActividad`, `fechaActividad`),
  KEY `idx_lista_espera_actividad_fecha` (`idActividad`, `fechaActividad`, `estado`),
  CONSTRAINT `fk_lista_espera_actividad`
    FOREIGN KEY (`idActividad`) REFERENCES `Actividad` (`idActividad`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_lista_espera_cliente`
    FOREIGN KEY (`idCliente`) REFERENCES `cliente` (`idCliente`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Avisos internos: se muestran en Mi cuenta cuando coordinación modifica
-- horario, día, sector o responsable de una actividad ya reservada.
CREATE TABLE IF NOT EXISTS `notificacionCliente` (
  `idNotificacion` INT NOT NULL AUTO_INCREMENT,
  `idCliente` INT NOT NULL,
  `idActividad` INT DEFAULT NULL,
  `tipo` VARCHAR(30) NOT NULL,
  `titulo` VARCHAR(120) NOT NULL,
  `mensaje` VARCHAR(500) NOT NULL,
  `creadoEn` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `leidoEn` DATETIME DEFAULT NULL,
  PRIMARY KEY (`idNotificacion`),
  KEY `idx_notificacion_cliente` (`idCliente`, `leidoEn`, `creadoEn`),
  CONSTRAINT `fk_notificacion_cliente`
    FOREIGN KEY (`idCliente`) REFERENCES `cliente` (`idCliente`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_notificacion_actividad`
    FOREIGN KEY (`idActividad`) REFERENCES `Actividad` (`idActividad`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Verificación rápida: debe devolver cero filas si todavía no hay solicitudes.
SELECT COUNT(*) AS solicitudes_en_espera FROM `listaEsperaActividad`;
