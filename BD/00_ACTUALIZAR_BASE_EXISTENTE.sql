-- ACTUALIZACIÓN UNIFICADA DE EL MOLINO
-- Ejecutar una vez desde phpMyAdmin sobre la base que ya está en uso.
-- Se puede volver a ejecutar: comprueba cada columna antes de agregarla.

USE `s27_El Molino`;

-- Ayudante temporal para agregar columnas sin provocar el error "Duplicate column".
DROP PROCEDURE IF EXISTS agregar_columna_si_falta;
DELIMITER $$
CREATE PROCEDURE agregar_columna_si_falta(
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
        SET @sql_columna = CONCAT('ALTER TABLE `', nombre_tabla, '` ADD COLUMN `', nombre_columna, '` ', definicion_columna);
        PREPARE sentencia_columna FROM @sql_columna;
        EXECUTE sentencia_columna;
        DEALLOCATE PREPARE sentencia_columna;
    END IF;
END$$
DELIMITER ;

-- Registro y acompañantes ----------------------------------------------------
CALL agregar_columna_si_falta('alojamientos', 'descripcion', 'TEXT NULL AFTER `precioPorNoche`');

CREATE TABLE IF NOT EXISTS `registro_pendiente` (
  `idRegistroPendiente` INT NOT NULL AUTO_INCREMENT,
  `dni` VARCHAR(10) NOT NULL,
  `nombre` VARCHAR(50) NOT NULL,
  `apellido` VARCHAR(50) NOT NULL,
  `telefono` VARCHAR(20) NOT NULL,
  `email` VARCHAR(100) NOT NULL,
  `direccion` VARCHAR(150) NOT NULL,
  `acompanantes_json` LONGTEXT DEFAULT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `codigo_hash` VARCHAR(255) NOT NULL,
  `vence_en` DATETIME NOT NULL,
  `intentos` TINYINT UNSIGNED NOT NULL DEFAULT 0,
  `creado_en` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`idRegistroPendiente`),
  UNIQUE KEY `uk_registro_pendiente_email` (`email`),
  UNIQUE KEY `uk_registro_pendiente_dni` (`dni`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL agregar_columna_si_falta('registro_pendiente', 'acompanantes_json', 'LONGTEXT DEFAULT NULL AFTER `direccion`');

CREATE TABLE IF NOT EXISTS `reservaAcompañante` (
  `idReserva` INT NOT NULL,
  `idAcompañante` INT NOT NULL,
  PRIMARY KEY (`idReserva`, `idAcompañante`),
  KEY `idx_ra_acompañante` (`idAcompañante`),
  CONSTRAINT `fk_ra_reserva` FOREIGN KEY (`idReserva`) REFERENCES `reservas` (`idReserva`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_ra_acompañante` FOREIGN KEY (`idAcompañante`) REFERENCES `acompañante` (`idAcompañante`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Actividades completas y selección de fecha --------------------------------
CREATE TABLE IF NOT EXISTS `CategoriaActividad` (
  `idCategoria` INT NOT NULL AUTO_INCREMENT,
  `slug` VARCHAR(70) NOT NULL,
  `nombre` VARCHAR(60) NOT NULL,
  `descripcion` VARCHAR(160) NOT NULL,
  `imagen` VARCHAR(255) DEFAULT NULL,
  `activa` TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`idCategoria`),
  UNIQUE KEY `uk_categoria_actividad_slug` (`slug`),
  UNIQUE KEY `uk_categoria_actividad_nombre` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT IGNORE INTO `CategoriaActividad` (`slug`, `nombre`, `descripcion`, `imagen`) VALUES
('caminatas', 'Caminatas', 'Senderos y recorridos guiados', 'imgs/caminatas.jpg'),
('deportes', 'Deportes', 'Fútbol, básquet, natación y más', 'imgs/futb.jpg'),
('bienestar', 'Bienestar', 'Yoga y propuestas para bajar el ritmo', 'imgs/bienestar.jpg'),
('talleres', 'Talleres', 'Experiencias creativas para compartir', 'imgs/talleres.jpg'),
('espectaculos', 'Espectáculos', 'Música y propuestas para toda la familia', 'imgs/espectaculos.jpg'),
('naturaleza', 'Naturaleza', 'Actividades conectadas con el entorno', 'imgs/naturaleza.jpg');

CALL agregar_columna_si_falta('Actividad', 'slug', 'VARCHAR(100) NULL AFTER `idActividad`');
CALL agregar_columna_si_falta('Actividad', 'idCategoria', 'INT NULL AFTER `nombre`');
CALL agregar_columna_si_falta('Actividad', 'descripcionCorta', 'VARCHAR(160) NULL AFTER `idCategoria`');
CALL agregar_columna_si_falta('Actividad', 'requisitos', 'TEXT NULL AFTER `descripcion`');
CALL agregar_columna_si_falta('Actividad', 'informacionImportante', 'TEXT NULL AFTER `requisitos`');
CALL agregar_columna_si_falta('Actividad', 'dias', 'VARCHAR(100) NULL AFTER `horario`');
CALL agregar_columna_si_falta('Actividad', 'momentoDia', 'ENUM(''manana'',''tarde'',''noche'') NULL AFTER `dias`');
CALL agregar_columna_si_falta('Actividad', 'sector', 'VARCHAR(120) NULL AFTER `Duracion`');
CALL agregar_columna_si_falta('Actividad', 'responsable', 'VARCHAR(100) NULL AFTER `sector`');
CALL agregar_columna_si_falta('Actividad', 'edadRecomendada', 'ENUM(''todas'',''ninos'',''adolescentes'',''adultos'') NOT NULL DEFAULT ''todas'' AFTER `responsable`');
CALL agregar_columna_si_falta('Actividad', 'modalidad', 'ENUM(''incluida'',''inscripcion'') NOT NULL DEFAULT ''incluida'' AFTER `edadRecomendada`');
CALL agregar_columna_si_falta('Actividad', 'imagen', 'VARCHAR(255) NULL AFTER `modalidad`');
CALL agregar_columna_si_falta('Actividad', 'tendencia', 'TINYINT(1) NOT NULL DEFAULT 0 AFTER `imagen`');
CALL agregar_columna_si_falta('Actividad', 'activa', 'TINYINT(1) NOT NULL DEFAULT 1 AFTER `tendencia`');

UPDATE `Actividad`
   SET `slug` = CONCAT('actividad-', `idActividad`)
 WHERE `slug` IS NULL OR TRIM(`slug`) = '';
UPDATE `Actividad`
   SET `idCategoria` = (SELECT `idCategoria` FROM `CategoriaActividad` WHERE `slug` = 'deportes' LIMIT 1)
 WHERE `idCategoria` IS NULL;
UPDATE `Actividad` SET `dias` = 'Todos los días' WHERE `dias` IS NULL OR TRIM(`dias`) = '';
ALTER TABLE `Actividad` MODIFY COLUMN `descripcion` TEXT NOT NULL;
ALTER TABLE `Actividad` MODIFY COLUMN `idEspacio` INT NULL;
ALTER TABLE `Actividad` MODIFY COLUMN `idUsuario` INT NULL;

CREATE TABLE IF NOT EXISTS `inscripcionActividad` (
  `idInscripcion` INT NOT NULL AUTO_INCREMENT,
  `idCliente` INT NOT NULL,
  `idActividad` INT NOT NULL,
  `participaTitular` TINYINT(1) NOT NULL DEFAULT 1,
  `fechaActividad` DATE NULL,
  `fechaInscripcion` DATE NOT NULL,
  `asistencia` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`idInscripcion`),
  UNIQUE KEY `uk_cliente_actividad` (`idCliente`, `idActividad`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL agregar_columna_si_falta('inscripcionActividad', 'participaTitular', 'TINYINT(1) NOT NULL DEFAULT 1 AFTER `idActividad`');
CALL agregar_columna_si_falta('inscripcionActividad', 'fechaActividad', 'DATE NULL AFTER `participaTitular`');
UPDATE `inscripcionActividad` SET `fechaActividad` = CURRENT_DATE WHERE `fechaActividad` IS NULL;
ALTER TABLE `inscripcionActividad` MODIFY COLUMN `fechaActividad` DATE NOT NULL;

CREATE TABLE IF NOT EXISTS `inscripcionActividadAcompanante` (
  `idInscripcionAcompanante` INT NOT NULL AUTO_INCREMENT,
  `idClienteTitular` INT NOT NULL,
  `idAcompañante` INT NOT NULL,
  `idActividad` INT NOT NULL,
  `fechaInscripcion` DATE NOT NULL,
  `asistencia` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`idInscripcionAcompanante`),
  UNIQUE KEY `uk_acompanante_actividad` (`idAcompañante`, `idActividad`),
  KEY `idx_inscripcion_acomp_titular_actividad` (`idClienteTitular`, `idActividad`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Pedidos, seña, cancelación y pagos ----------------------------------------
CREATE TABLE IF NOT EXISTS `pedido_alojamiento` (
  `idPedido` INT NOT NULL AUTO_INCREMENT,
  `idCliente` INT NOT NULL,
  `total` DECIMAL(10,2) NOT NULL,
  `metodoPago` VARCHAR(50) NOT NULL,
  `estadoPedido` VARCHAR(30) NOT NULL DEFAULT 'ESPERANDO_SENA',
  `estadoPago` VARCHAR(30) NOT NULL DEFAULT 'SENA_PENDIENTE',
  `porcentajeSena` DECIMAL(5,2) NOT NULL DEFAULT 30.00,
  `montoSena` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `fechaLimiteSena` DATETIME DEFAULT NULL,
  `senaPagadaEn` DATETIME DEFAULT NULL,
  `canceladoEn` DATETIME DEFAULT NULL,
  `creadoEn` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`idPedido`),
  KEY `idx_pedido_alojamiento_cliente` (`idCliente`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL agregar_columna_si_falta('pedido_alojamiento', 'porcentajeSena', 'DECIMAL(5,2) NOT NULL DEFAULT 30.00 AFTER `estadoPago`');
CALL agregar_columna_si_falta('pedido_alojamiento', 'montoSena', 'DECIMAL(10,2) NOT NULL DEFAULT 0.00 AFTER `porcentajeSena`');
CALL agregar_columna_si_falta('pedido_alojamiento', 'fechaLimiteSena', 'DATETIME NULL AFTER `montoSena`');
CALL agregar_columna_si_falta('pedido_alojamiento', 'senaPagadaEn', 'DATETIME NULL AFTER `fechaLimiteSena`');
CALL agregar_columna_si_falta('pedido_alojamiento', 'canceladoEn', 'DATETIME NULL AFTER `senaPagadaEn`');
UPDATE `pedido_alojamiento`
   SET `montoSena` = ROUND(`total` * `porcentajeSena` / 100, 2)
 WHERE `montoSena` = 0;
UPDATE `pedido_alojamiento`
   SET `estadoPedido` = 'ESPERANDO_SENA'
 WHERE UPPER(`estadoPedido`) = 'PENDIENTE';
UPDATE `pedido_alojamiento`
   SET `estadoPago` = 'SENA_PENDIENTE'
 WHERE UPPER(`estadoPago`) = 'PENDIENTE';

CREATE TABLE IF NOT EXISTS `detalle_pedido_alojamiento` (
  `idDetalle` INT NOT NULL AUTO_INCREMENT,
  `idPedido` INT NOT NULL,
  `idReserva` INT NOT NULL,
  `alojamientoNumero` VARCHAR(10) NOT NULL,
  `alojamientoTipo` VARCHAR(20) NOT NULL,
  `fechaIngreso` DATE NOT NULL,
  `fechaEgreso` DATE NOT NULL,
  `cantidadPersonas` INT NOT NULL,
  `noches` INT NOT NULL,
  `precioPorNoche` DECIMAL(10,2) NOT NULL,
  `subtotal` DECIMAL(10,2) NOT NULL,
  PRIMARY KEY (`idDetalle`),
  UNIQUE KEY `uk_detalle_pedido_reserva` (`idReserva`),
  KEY `idx_detalle_pedido_alojamiento` (`idPedido`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `pago` (
  `idPago` INT NOT NULL AUTO_INCREMENT,
  `idReserva` INT NOT NULL,
  `montoTotal` DECIMAL(10,2) NOT NULL,
  `metodoPago` VARCHAR(50) NOT NULL,
  `tipoPago` VARCHAR(20) NOT NULL DEFAULT 'SENA',
  `estado` VARCHAR(20) NOT NULL DEFAULT 'CONFIRMADO',
  `fechaPago` DATE NOT NULL,
  `comprobante` VARCHAR(150) DEFAULT NULL,
  PRIMARY KEY (`idPago`),
  KEY `idx_pago_reserva` (`idReserva`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CALL agregar_columna_si_falta('pago', 'tipoPago', 'VARCHAR(20) NOT NULL DEFAULT ''SENA'' AFTER `metodoPago`');
CALL agregar_columna_si_falta('pago', 'estado', 'VARCHAR(20) NOT NULL DEFAULT ''CONFIRMADO'' AFTER `tipoPago`');

DROP PROCEDURE IF EXISTS agregar_columna_si_falta;

-- Resultado esperado:
-- 1) el registro conserva acompañantes sin el error acompanantes_json;
-- 2) las reservas guardan acompañantes y una seña del 30%;
-- 3) el cliente puede cancelar antes del ingreso;
-- 4) cada inscripción a actividad conserva la fecha elegida.
