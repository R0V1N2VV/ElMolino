-- Ejecutar una vez sobre la base `s27_El Molino`.
-- Guarda compras web pendientes de pago y enlaza cada pedido con sus reservas.

USE `s27_El Molino`;

CREATE TABLE IF NOT EXISTS `pedido_alojamiento` (
  `idPedido` int NOT NULL AUTO_INCREMENT,
  `idCliente` int NOT NULL,
  `total` decimal(10,2) NOT NULL,
  `metodoPago` varchar(50) NOT NULL,
  `estadoPedido` varchar(30) NOT NULL DEFAULT 'PENDIENTE',
  `estadoPago` varchar(30) NOT NULL DEFAULT 'PENDIENTE',
  `creadoEn` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`idPedido`),
  KEY `idx_pedido_alojamiento_cliente` (`idCliente`),
  CONSTRAINT `fk_pedido_alojamiento_cliente`
    FOREIGN KEY (`idCliente`) REFERENCES `cliente` (`idCliente`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `detalle_pedido_alojamiento` (
  `idDetalle` int NOT NULL AUTO_INCREMENT,
  `idPedido` int NOT NULL,
  `idReserva` int NOT NULL,
  `alojamientoNumero` varchar(10) NOT NULL,
  `alojamientoTipo` varchar(20) NOT NULL,
  `fechaIngreso` date NOT NULL,
  `fechaEgreso` date NOT NULL,
  `cantidadPersonas` int NOT NULL,
  `noches` int NOT NULL,
  `precioPorNoche` decimal(10,2) NOT NULL,
  `subtotal` decimal(10,2) NOT NULL,
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
