-- Ejecutar después de importar s27_El_Molino.sql.
-- Conserva los datos de registro y el código hasta que la persona confirme su correo.
CREATE TABLE IF NOT EXISTS `registro_pendiente` (
  `idRegistroPendiente` int NOT NULL AUTO_INCREMENT,
  `dni` int NOT NULL,
  `nombre` varchar(50) NOT NULL,
  `apellido` varchar(50) NOT NULL,
  `telefono` int NOT NULL,
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
