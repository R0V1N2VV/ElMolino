-- ETAPA 2: reservas, señas, saldos, vencimientos y reintegros.
-- Ejecutar una vez sobre una base existente. Es seguro volver a ejecutarlo.
USE `s27_El Molino`;

CREATE TABLE IF NOT EXISTS `reembolso` (
  `idReembolso` INT NOT NULL AUTO_INCREMENT,
  `idPedido` INT NOT NULL,
  `monto` DECIMAL(10,2) NOT NULL,
  `estado` VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
  `motivo` VARCHAR(180) NOT NULL,
  `fechaSolicitud` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fechaResolucion` DATETIME DEFAULT NULL,
  `metodo` VARCHAR(50) DEFAULT NULL,
  `comprobante` VARCHAR(150) DEFAULT NULL,
  PRIMARY KEY (`idReembolso`),
  KEY `idx_reembolso_pedido_estado` (`idPedido`, `estado`),
  CONSTRAINT `fk_reembolso_pedido` FOREIGN KEY (`idPedido`)
    REFERENCES `pedido_alojamiento` (`idPedido`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Normaliza registros viejos para que el recálculo de la aplicación sea predecible.
UPDATE `pago` SET `tipoPago`='SENA' WHERE `tipoPago` IS NULL OR TRIM(`tipoPago`)='';
UPDATE `pago` SET `estado`='CONFIRMADO' WHERE `estado` IS NULL OR TRIM(`estado`)='';
UPDATE `pedido_alojamiento`
   SET `montoSena`=ROUND(`total` * `porcentajeSena` / 100, 2)
 WHERE `montoSena` IS NULL OR `montoSena`=0;

