-- Ejecutar una sola vez sobre una base ya existente.
-- Agrega las tablas que usa el flujo de confirmacion de estadias.

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
