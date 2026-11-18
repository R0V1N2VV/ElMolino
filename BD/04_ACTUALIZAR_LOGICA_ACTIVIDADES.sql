-- ETAPA 4: COHERENCIA, HISTORIAL Y CARGOS DE ACTIVIDADES
-- Ejecutar una sola vez sobre la base actualmente usada por la web y Java.
USE `s27_El Molino`;

DROP PROCEDURE IF EXISTS agregar_columna_actividad_si_falta;
DELIMITER $$
CREATE PROCEDURE agregar_columna_actividad_si_falta(
    IN nombre_tabla VARCHAR(64),
    IN nombre_columna VARCHAR(64),
    IN definicion_columna TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = nombre_tabla
           AND COLUMN_NAME = nombre_columna
    ) THEN
        SET @sql_columna_actividad = CONCAT(
            'ALTER TABLE `', nombre_tabla, '` ADD COLUMN `',
            nombre_columna, '` ', definicion_columna
        );
        PREPARE sentencia_columna_actividad FROM @sql_columna_actividad;
        EXECUTE sentencia_columna_actividad;
        DEALLOCATE PREPARE sentencia_columna_actividad;
    END IF;
END$$
DELIMITER ;

CALL agregar_columna_actividad_si_falta(
    'inscripcionActividad', 'estado',
    'VARCHAR(20) NOT NULL DEFAULT ''ACTIVA'' AFTER `asistencia`'
);
CALL agregar_columna_actividad_si_falta(
    'inscripcionActividad', 'canceladaEn',
    'DATETIME NULL AFTER `estado`'
);
CALL agregar_columna_actividad_si_falta(
    'inscripcionActividad', 'motivoCancelacion',
    'VARCHAR(100) NULL AFTER `canceladaEn`'
);
CALL agregar_columna_actividad_si_falta(
    'inscripcionActividad', 'actualizadaEn',
    'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `motivoCancelacion`'
);
CALL agregar_columna_actividad_si_falta(
    'inscripcionActividadAcompanante', 'activo',
    'TINYINT(1) NOT NULL DEFAULT 1 AFTER `asistencia`'
);
CALL agregar_columna_actividad_si_falta(
    'inscripcionActividadAcompanante', 'canceladaEn',
    'DATETIME NULL AFTER `activo`'
);

CREATE TABLE IF NOT EXISTS `cargo_actividad` (
  `idCargoActividad` INT NOT NULL AUTO_INCREMENT,
  `idInscripcion` INT NOT NULL,
  `idPedido` INT DEFAULT NULL,
  `idCliente` INT NOT NULL,
  `idActividad` INT NOT NULL,
  `fechaActividad` DATE NOT NULL,
  `cantidadParticipantes` INT NOT NULL,
  `precioUnitario` DECIMAL(10,2) NOT NULL,
  `montoTotal` DECIMAL(10,2) NOT NULL,
  `estado` VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
  `creadoEn` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizadoEn` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `anuladoEn` DATETIME DEFAULT NULL,
  PRIMARY KEY (`idCargoActividad`),
  KEY `idx_cargo_actividad_inscripcion` (`idInscripcion`),
  KEY `idx_cargo_actividad_pedido` (`idPedido`),
  KEY `idx_cargo_actividad_cliente_estado` (`idCliente`, `estado`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CALL agregar_columna_actividad_si_falta(
    'cargo_actividad', 'idPedido',
    'INT NULL AFTER `idInscripcion`'
);

-- Genera el cargo de inscripciones pagas futuras que ya existían antes de la actualización.
INSERT INTO `cargo_actividad`
    (`idInscripcion`, `idPedido`, `idCliente`, `idActividad`, `fechaActividad`,
     `cantidadParticipantes`, `precioUnitario`, `montoTotal`, `estado`)
SELECT ia.idInscripcion,
       (SELECT p.idPedido FROM reservas r
         JOIN detalle_pedido_alojamiento d ON d.idReserva=r.idReserva
         JOIN pedido_alojamiento p ON p.idPedido=d.idPedido
        WHERE r.idCliente=ia.idCliente AND UPPER(r.estadoReserva)='CONFIRMADA'
          AND UPPER(p.estadoPago) IN ('SENA_PAGADA','PAGADO')
          AND ia.fechaActividad BETWEEN r.fechaIngreso AND r.fechaEgreso
        ORDER BY r.fechaIngreso DESC, p.idPedido DESC LIMIT 1),
       ia.idCliente, ia.idActividad, ia.fechaActividad,
       ia.participaTitular + COUNT(CASE WHEN iaa.activo = 1 THEN 1 END) AS personas,
       a.precio,
       a.precio * (ia.participaTitular + COUNT(CASE WHEN iaa.activo = 1 THEN 1 END)),
       'PENDIENTE'
  FROM inscripcionActividad ia
  JOIN Actividad a ON a.idActividad = ia.idActividad
  LEFT JOIN inscripcionActividadAcompanante iaa
    ON iaa.idClienteTitular = ia.idCliente AND iaa.idActividad = ia.idActividad
 WHERE UPPER(ia.estado) = 'ACTIVA'
   AND ia.fechaActividad >= CURRENT_DATE
   AND a.modalidad = 'inscripcion' AND a.precio > 0
   AND NOT EXISTS (
       SELECT 1 FROM cargo_actividad ca
        WHERE ca.idInscripcion = ia.idInscripcion AND UPPER(ca.estado) = 'PENDIENTE'
   )
 GROUP BY ia.idInscripcion, ia.idCliente, ia.idActividad, ia.fechaActividad,
          ia.participaTitular, a.precio
HAVING personas > 0;

DROP PROCEDURE IF EXISTS agregar_columna_actividad_si_falta;

-- Resultado:
-- 1) las cancelaciones conservan el historial;
-- 2) acompañantes retirados dejan de ocupar cupo;
-- 3) actividades pagas generan un cargo con precio histórico;
-- 4) esos cargos se anulan si la inscripción deja de ser válida.
