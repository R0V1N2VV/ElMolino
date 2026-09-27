-- Base completa de El Molino
-- Importar en phpMyAdmin de Uniform Server.
-- No elimina bases ni tablas existentes: importarla en una base nueva o vacía.

CREATE DATABASE IF NOT EXISTS `s27_El Molino`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE `s27_El Molino`;

CREATE TABLE IF NOT EXISTS `cliente` (
  `idCliente` INT NOT NULL AUTO_INCREMENT,
  `dni` VARCHAR(10) NOT NULL,
  `nombre` VARCHAR(50) NOT NULL,
  `apellido` VARCHAR(50) NOT NULL,
  `telefono` VARCHAR(20) NOT NULL,
  `email` VARCHAR(100) NOT NULL,
  `direccion` VARCHAR(150) NOT NULL,
  PRIMARY KEY (`idCliente`),
  UNIQUE KEY `uk_cliente_dni` (`dni`),
  UNIQUE KEY `uk_cliente_telefono` (`telefono`),
  UNIQUE KEY `uk_cliente_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `usuarios` (
  `idUsuario` INT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(50) NOT NULL,
  `apellido` VARCHAR(50) NOT NULL,
  `email` VARCHAR(100) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `rol` VARCHAR(75) NOT NULL DEFAULT 'cliente',
  PRIMARY KEY (`idUsuario`),
  UNIQUE KEY `uk_usuario_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Datos temporales del registro antes de validar el código enviado por correo.
CREATE TABLE IF NOT EXISTS `registro_pendiente` (
  `idRegistroPendiente` INT NOT NULL AUTO_INCREMENT,
  `dni` VARCHAR(10) NOT NULL,
  `nombre` VARCHAR(50) NOT NULL,
  `apellido` VARCHAR(50) NOT NULL,
  `telefono` VARCHAR(20) NOT NULL,
  `email` VARCHAR(100) NOT NULL,
  `direccion` VARCHAR(150) NOT NULL,
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

CREATE TABLE IF NOT EXISTS `EspacioRecreativo` (
  `idEspacio` INT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(50) NOT NULL,
  `descripcion` VARCHAR(150) NOT NULL,
  `ubicacion` VARCHAR(100) NOT NULL,
  `horarioApertura` TIME NOT NULL,
  `horarioCierre` TIME NOT NULL,
  `requiereReserva` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`idEspacio`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `alojamientos` (
  `idAlojamiento` INT NOT NULL AUTO_INCREMENT,
  `numero` VARCHAR(10) NOT NULL,
  `tipo` VARCHAR(20) NOT NULL,
  `capacidad` INT NOT NULL,
  `precioPorNoche` DECIMAL(10,2) NOT NULL,
  `estado` VARCHAR(30) NOT NULL,
  PRIMARY KEY (`idAlojamiento`),
  UNIQUE KEY `uk_alojamiento_numero` (`numero`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `Actividad` (
  `idActividad` INT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(50) NOT NULL,
  `descripcion` VARCHAR(150) NOT NULL,
  `horario` VARCHAR(50) NOT NULL,
  `Duracion` VARCHAR(50) NOT NULL,
  `idEspacio` INT NOT NULL,
  `cupoMax` INT NOT NULL,
  `precio` DECIMAL(10,2) NOT NULL,
  `pago` VARCHAR(50) NOT NULL,
  `idUsuario` INT NOT NULL,
  PRIMARY KEY (`idActividad`),
  KEY `idx_actividad_espacio` (`idEspacio`),
  KEY `idx_actividad_usuario` (`idUsuario`),
  CONSTRAINT `fk_actividad_espacio`
    FOREIGN KEY (`idEspacio`) REFERENCES `EspacioRecreativo` (`idEspacio`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_actividad_usuario`
    FOREIGN KEY (`idUsuario`) REFERENCES `usuarios` (`idUsuario`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `reservas` (
  `idReserva` INT NOT NULL AUTO_INCREMENT,
  `idCliente` INT NOT NULL,
  `idAlojamiento` INT NOT NULL,
  `fechaIngreso` DATE NOT NULL,
  `fechaEgreso` DATE NOT NULL,
  `cantidadPersonas` INT NOT NULL,
  `temporada` VARCHAR(30) DEFAULT NULL,
  `estadoReserva` VARCHAR(30) NOT NULL,
  `precioTotal` DECIMAL(10,2) NOT NULL,
  PRIMARY KEY (`idReserva`),
  KEY `idx_reserva_cliente` (`idCliente`),
  KEY `idx_reserva_alojamiento` (`idAlojamiento`),
  CONSTRAINT `fk_reserva_cliente`
    FOREIGN KEY (`idCliente`) REFERENCES `cliente` (`idCliente`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_reserva_alojamiento`
    FOREIGN KEY (`idAlojamiento`) REFERENCES `alojamientos` (`idAlojamiento`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `inscripcionActividad` (
  `idInscripcion` INT NOT NULL AUTO_INCREMENT,
  `idCliente` INT NOT NULL,
  `idActividad` INT NOT NULL,
  `fechaInscripcion` DATE NOT NULL,
  `asistencia` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`idInscripcion`),
  UNIQUE KEY `uk_cliente_actividad` (`idCliente`, `idActividad`),
  KEY `idx_inscripcion_cliente` (`idCliente`),
  KEY `idx_inscripcion_actividad` (`idActividad`),
  CONSTRAINT `fk_inscripcion_cliente`
    FOREIGN KEY (`idCliente`) REFERENCES `cliente` (`idCliente`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_inscripcion_actividad`
    FOREIGN KEY (`idActividad`) REFERENCES `Actividad` (`idActividad`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `pago` (
  `idPago` INT NOT NULL AUTO_INCREMENT,
  `idReserva` INT NOT NULL,
  `montoTotal` DECIMAL(10,2) NOT NULL,
  `metodoPago` VARCHAR(50) NOT NULL,
  `fechaPago` DATE NOT NULL,
  `comprobante` VARCHAR(150) DEFAULT NULL,
  PRIMARY KEY (`idPago`),
  KEY `idx_pago_reserva` (`idReserva`),
  CONSTRAINT `fk_pago_reserva`
    FOREIGN KEY (`idReserva`) REFERENCES `reservas` (`idReserva`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `reservaAcompañante` (
  `idReserva` INT NOT NULL,
  `idAcompañante` INT NOT NULL,
  PRIMARY KEY (`idReserva`, `idAcompañante`),
  KEY `idx_ra_acompañante` (`idAcompañante`),
  CONSTRAINT `fk_ra_reserva`
    FOREIGN KEY (`idReserva`) REFERENCES `reservas` (`idReserva`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_ra_acompañante`
    FOREIGN KEY (`idAcompañante`) REFERENCES `acompañante` (`idAcompañante`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
