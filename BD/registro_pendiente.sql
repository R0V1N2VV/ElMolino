-- Ejecutar en la base `s27_El Molino` después de importar la base principal.
-- Conserva los datos de registro y el código hasta que la persona confirme su correo.
CREATE TABLE IF NOT EXISTS `registro_pendiente` (
  `idRegistroPendiente` int NOT NULL AUTO_INCREMENT,
  `dni` varchar(10) NOT NULL,
  `nombre` varchar(50) NOT NULL,
  `apellido` varchar(50) NOT NULL,
  `telefono` varchar(20) NOT NULL,
  `email` varchar(100) NOT NULL,
  `direccion` varchar(150) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `codigo_hash` varchar(255) NOT NULL,
  `vence_en` datetime NOT NULL,
  `intentos` tinyint unsigned NOT NULL DEFAULT 0,
  `creado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`idRegistroPendiente`),
  UNIQUE KEY `uk_registro_pendiente_email` (`email`),
  UNIQUE KEY `uk_registro_pendiente_dni` (`dni`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- CREATE TABLE IF NOT EXISTS no actualiza tablas ya creadas anteriormente.
ALTER TABLE `registro_pendiente`
  MODIFY COLUMN `dni` varchar(10) NOT NULL,
  MODIFY COLUMN `telefono` varchar(20) NOT NULL;
