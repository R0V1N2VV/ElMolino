-- Ejecutar una sola vez en la base existente antes de habilitar el nuevo registro.
-- Conserva los datos actuales y agrega el campo para credenciales de clientes.
SET @columna_existe := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'cliente'
    AND COLUMN_NAME = 'password_hash'
);
SET @sql := IF(
  @columna_existe = 0,
  'ALTER TABLE `cliente` ADD COLUMN `password_hash` VARCHAR(255) DEFAULT NULL AFTER `direccion`',
  'SELECT ''La columna cliente.password_hash ya existe'''
);
PREPARE sentencia FROM @sql;
EXECUTE sentencia;
DEALLOCATE PREPARE sentencia;
