-- Ejecutar una vez sobre la base de El Molino antes de usar el nuevo campo/rol.
-- Agrega descripcion solo si todavía no existe.
SET @tiene_descripcion := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'alojamientos'
      AND COLUMN_NAME = 'descripcion'
);
SET @sql := IF(
    @tiene_descripcion = 0,
    'ALTER TABLE alojamientos ADD COLUMN descripcion TEXT NULL AFTER tipo',
    'SELECT ''La columna descripcion ya existe'''
);
PREPARE cambio FROM @sql;
EXECUTE cambio;
DEALLOCATE PREPARE cambio;

-- Permite guardar el nuevo rol sin cambiar los roles que ya tienen los usuarios.
ALTER TABLE usuarios MODIFY COLUMN rol VARCHAR(32) NOT NULL DEFAULT 'RECEPCION';