-- El Molino · actualización 13
-- Reloj compartido para una demostración controlada del circuito de estadías.
-- Ejecutar una vez en phpMyAdmin, con la base s27_El Molino seleccionada.
-- No modifica ni borra reservas, pagos, usuarios ni alojamientos.

USE `s27_El Molino`;

CREATE TABLE IF NOT EXISTS `configuracionDemostracion` (
  `idConfiguracion` TINYINT NOT NULL,
  `activo` TINYINT(1) NOT NULL DEFAULT 0,
  `fecha` DATE DEFAULT NULL,
  `hora` TIME DEFAULT NULL,
  `actualizadoEn` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizadoPor` INT DEFAULT NULL,
  PRIMARY KEY (`idConfiguracion`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `configuracionDemostracion`
  (`idConfiguracion`, `activo`, `fecha`, `hora`, `actualizadoEn`, `actualizadoPor`)
VALUES (1, 0, NULL, NULL, NOW(), NULL)
ON DUPLICATE KEY UPDATE idConfiguracion=VALUES(idConfiguracion);

-- La consulta debe mostrar activo = 0 hasta que Superadmin encienda el modo.
SELECT activo, fecha, hora, actualizadoEn
FROM configuracionDemostracion
WHERE idConfiguracion = 1;
