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

-- Cinco unidades por tipo y capacidad. La aplicación y la web leen esta misma tabla.
-- Los números son internos y no se muestran al huésped.
INSERT IGNORE INTO `alojamientos` (`numero`, `tipo`, `capacidad`, `precioPorNoche`, `estado`) VALUES
('CAB2-01', 'Cabaña', 2, 65000.00, 'Disponible'), ('CAB2-02', 'Cabaña', 2, 65000.00, 'Disponible'), ('CAB2-03', 'Cabaña', 2, 65000.00, 'Disponible'), ('CAB2-04', 'Cabaña', 2, 65000.00, 'Disponible'), ('CAB2-05', 'Cabaña', 2, 65000.00, 'Disponible'),
('CAB4-01', 'Cabaña', 4, 85000.00, 'Disponible'), ('CAB4-02', 'Cabaña', 4, 85000.00, 'Disponible'), ('CAB4-03', 'Cabaña', 4, 85000.00, 'Disponible'), ('CAB4-04', 'Cabaña', 4, 85000.00, 'Disponible'), ('CAB4-05', 'Cabaña', 4, 85000.00, 'Disponible'),
('CAB6-01', 'Cabaña', 6, 105000.00, 'Disponible'), ('CAB6-02', 'Cabaña', 6, 105000.00, 'Disponible'), ('CAB6-03', 'Cabaña', 6, 105000.00, 'Disponible'), ('CAB6-04', 'Cabaña', 6, 105000.00, 'Disponible'), ('CAB6-05', 'Cabaña', 6, 105000.00, 'Disponible'),
('HAB2-01', 'Habitación', 2, 42000.00, 'Disponible'), ('HAB2-02', 'Habitación', 2, 42000.00, 'Disponible'), ('HAB2-03', 'Habitación', 2, 42000.00, 'Disponible'), ('HAB2-04', 'Habitación', 2, 42000.00, 'Disponible'), ('HAB2-05', 'Habitación', 2, 42000.00, 'Disponible'),
('HAB4-01', 'Habitación', 4, 65000.00, 'Disponible'), ('HAB4-02', 'Habitación', 4, 65000.00, 'Disponible'), ('HAB4-03', 'Habitación', 4, 65000.00, 'Disponible'), ('HAB4-04', 'Habitación', 4, 65000.00, 'Disponible'), ('HAB4-05', 'Habitación', 4, 65000.00, 'Disponible'),
('HAB6-01', 'Habitación', 6, 82000.00, 'Disponible'), ('HAB6-02', 'Habitación', 6, 82000.00, 'Disponible'), ('HAB6-03', 'Habitación', 6, 82000.00, 'Disponible'), ('HAB6-04', 'Habitación', 6, 82000.00, 'Disponible'), ('HAB6-05', 'Habitación', 6, 82000.00, 'Disponible'),
('HOT2-01', 'Hotel', 2, 55000.00, 'Disponible'), ('HOT2-02', 'Hotel', 2, 55000.00, 'Disponible'), ('HOT2-03', 'Hotel', 2, 55000.00, 'Disponible'), ('HOT2-04', 'Hotel', 2, 55000.00, 'Disponible'), ('HOT2-05', 'Hotel', 2, 55000.00, 'Disponible'),
('HOT4-01', 'Hotel', 4, 78000.00, 'Disponible'), ('HOT4-02', 'Hotel', 4, 78000.00, 'Disponible'), ('HOT4-03', 'Hotel', 4, 78000.00, 'Disponible'), ('HOT4-04', 'Hotel', 4, 78000.00, 'Disponible'), ('HOT4-05', 'Hotel', 4, 78000.00, 'Disponible'),
('HOT6-01', 'Hotel', 6, 95000.00, 'Disponible'), ('HOT6-02', 'Hotel', 6, 95000.00, 'Disponible'), ('HOT6-03', 'Hotel', 6, 95000.00, 'Disponible'), ('HOT6-04', 'Hotel', 6, 95000.00, 'Disponible'), ('HOT6-05', 'Hotel', 6, 95000.00, 'Disponible');

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

CREATE TABLE IF NOT EXISTS `pedido_alojamiento` (
  `idPedido` INT NOT NULL AUTO_INCREMENT,
  `idCliente` INT NOT NULL,
  `total` DECIMAL(10,2) NOT NULL,
  `metodoPago` VARCHAR(50) NOT NULL,
  `estadoPedido` VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
  `estadoPago` VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
  `creadoEn` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`idPedido`),
  KEY `idx_pedido_alojamiento_cliente` (`idCliente`),
  CONSTRAINT `fk_pedido_alojamiento_cliente`
    FOREIGN KEY (`idCliente`) REFERENCES `cliente` (`idCliente`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
  KEY `idx_detalle_pedido_alojamiento` (`idPedido`),
  CONSTRAINT `fk_detalle_pedido_alojamiento_pedido`
    FOREIGN KEY (`idPedido`) REFERENCES `pedido_alojamiento` (`idPedido`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_detalle_pedido_alojamiento_reserva`
    FOREIGN KEY (`idReserva`) REFERENCES `reservas` (`idReserva`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `inscripcionActividad` (
  `idInscripcion` INT NOT NULL AUTO_INCREMENT,
  `idCliente` INT NOT NULL,
  `idActividad` INT NOT NULL,
  `participaTitular` TINYINT(1) NOT NULL DEFAULT 1,
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

-- Cada acompañante seleccionado ocupa un lugar adicional en la actividad.
CREATE TABLE IF NOT EXISTS inscripcionActividadAcompanante (
    idInscripcionAcompanante INT NOT NULL AUTO_INCREMENT,
    idClienteTitular INT NOT NULL,
    `idAcompañante` INT NOT NULL,
    idActividad INT NOT NULL,
    fechaInscripcion DATE NOT NULL,
    asistencia TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (idInscripcionAcompanante),
    UNIQUE KEY uk_acompanante_actividad (`idAcompañante`, idActividad),
    KEY idx_inscripcion_acomp_titular (idClienteTitular),
    KEY idx_inscripcion_acomp_actividad (idActividad),
    KEY idx_inscripcion_acomp_titular_actividad (idClienteTitular, idActividad),
    CONSTRAINT fk_inscripcion_acomp_persona
        FOREIGN KEY (`idAcompañante`) REFERENCES `acompañante`(`idAcompañante`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_inscripcion_acomp_inscripcion
        FOREIGN KEY (idClienteTitular, idActividad)
        REFERENCES inscripcionActividad(idCliente, idActividad)
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
