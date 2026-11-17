CREATE TABLE IF NOT EXISTS recuperacion_contrasena (
  idRecuperacion INT NOT NULL AUTO_INCREMENT,
  idCliente INT NOT NULL,
  codigo_hash VARCHAR(255) NOT NULL,
  venceEn DATETIME NOT NULL,
  intentos TINYINT UNSIGNED NOT NULL DEFAULT 0,
  creadoEn DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (idRecuperacion),
  UNIQUE KEY uk_recuperacion_cliente (idCliente),
  CONSTRAINT fk_recuperacion_cliente FOREIGN KEY (idCliente)
    REFERENCES cliente(idCliente) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
