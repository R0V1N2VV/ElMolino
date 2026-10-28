-- Permite registrar acompañantes durante el alta de una cuenta.
-- Ejecutar una vez sobre una instalación existente. Puede volver a ejecutarse.
USE `s27_El Molino`;

CREATE TABLE IF NOT EXISTS `acompañante` (
  `idAcompañante` INT NOT NULL AUTO_INCREMENT,
  `idCliente` INT NOT NULL,
  `dni` VARCHAR(10) NOT NULL,
  `nombre` VARCHAR(50) NOT NULL,
  `apellido` VARCHAR(50) NOT NULL,
  `edad` INT NOT NULL,
  `parentesco` VARCHAR(100) NOT NULL,
  PRIMARY KEY (`idAcompañante`),
  UNIQUE KEY `uk_acompañante_dni` (`dni`),
  KEY `idx_acompañante_cliente` (`idCliente`),
  CONSTRAINT `fk_acompañante_cliente`
    FOREIGN KEY (`idCliente`) REFERENCES `cliente` (`idCliente`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET @acompanantes_json_existe := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'registro_pendiente'
    AND COLUMN_NAME = 'acompanantes_json'
);
SET @agregar_acompanantes_json := IF(
  @acompanantes_json_existe = 0,
  'ALTER TABLE registro_pendiente ADD COLUMN acompanantes_json LONGTEXT DEFAULT NULL AFTER direccion',
  'SELECT 1'
);
PREPARE sentencia_acompanantes_json FROM @agregar_acompanantes_json;
EXECUTE sentencia_acompanantes_json;
DEALLOCATE PREPARE sentencia_acompanantes_json;
