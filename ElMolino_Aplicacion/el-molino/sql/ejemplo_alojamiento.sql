-- Tres alojamientos ficticios para probar la grilla de packs.
-- Antes de usar datos reales, reemplazá estos nombres, capacidades y precios.
INSERT INTO `alojamientos` (`numero`, `tipo`, `capacidad`, `precioPorNoche`, `estado`)
SELECT ejemplos.numero, ejemplos.tipo, ejemplos.capacidad, ejemplos.precio, 'Disponible'
FROM (
    SELECT 'D-ALOJ-01' AS numero, 'Cabaña' AS tipo, 4 AS capacidad, 85000.00 AS precio
    UNION ALL SELECT 'D-ALOJ-02', 'Habitación', 2, 55000.00
    UNION ALL SELECT 'D-ALOJ-03', 'Departamento', 6, 125000.00
) AS ejemplos
WHERE NOT EXISTS (
    SELECT 1 FROM `alojamientos` existente WHERE existente.`numero` = ejemplos.numero
);
