-- Pre-reserva de 30 minutos, comprobantes y revisión administrativa.
-- Ejecutar después de 00_ACTUALIZAR_BASE_EXISTENTE.sql / 02_ACTUALIZAR_RESERVAS_PAGOS.sql.
USE `s27_El Molino`;

SET @columna_revision := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pedido_alojamiento' AND COLUMN_NAME='fechaLimiteRevisionSena');
SET @sql_revision := IF(@columna_revision=0, 'ALTER TABLE pedido_alojamiento ADD COLUMN fechaLimiteRevisionSena DATETIME NULL AFTER fechaLimiteSena', 'SELECT 1');
PREPARE sentencia_revision FROM @sql_revision; EXECUTE sentencia_revision; DEALLOCATE PREPARE sentencia_revision;

SET @columna_archivo := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pago' AND COLUMN_NAME='archivoComprobante');
SET @sql_archivo := IF(@columna_archivo=0, 'ALTER TABLE pago ADD COLUMN archivoComprobante VARCHAR(255) NULL AFTER comprobante', 'SELECT 1');
PREPARE sentencia_archivo FROM @sql_archivo; EXECUTE sentencia_archivo; DEALLOCATE PREPARE sentencia_archivo;

SET @columna_observacion := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pago' AND COLUMN_NAME='observacion');
SET @sql_observacion := IF(@columna_observacion=0, 'ALTER TABLE pago ADD COLUMN observacion VARCHAR(255) NULL AFTER archivoComprobante', 'SELECT 1');
PREPARE sentencia_observacion FROM @sql_observacion; EXECUTE sentencia_observacion; DEALLOCATE PREPARE sentencia_observacion;
