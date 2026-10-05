-- Alojamiento de ejemplo para probar el módulo Alojamientos.
-- Puede ejecutarse más de una vez: no duplica el número de demostración.
INSERT INTO `alojamientos` (`numero`, `tipo`, `capacidad`, `precioPorNoche`, `estado`)
SELECT 'D-ALOJ-01', 'Cabaña', 4, 85000.00, 'Disponible'
WHERE NOT EXISTS (
    SELECT 1
    FROM `alojamientos`
    WHERE `numero` = 'D-ALOJ-01'
);
