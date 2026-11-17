-- REPARADOR ÚNICO E IDEMPOTENTE
-- Podés ejecutarlo aunque 06, 07 u 08 se hayan aplicado parcialmente.
-- Detecta columnas, índices y claves ya existentes antes de modificar la base.

DROP PROCEDURE IF EXISTS reparar_base_el_molino;
DELIMITER //
CREATE PROCEDURE reparar_base_el_molino()
BEGIN
  DECLARE existe INT DEFAULT 0;
  DECLARE clave VARCHAR(128) DEFAULT NULL;

  SELECT COUNT(*) INTO existe FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='inscripcionActividadAcompanante' AND COLUMN_NAME='fechaActividad';
  IF existe=0 THEN
    ALTER TABLE inscripcionActividadAcompanante ADD COLUMN fechaActividad DATE NULL AFTER idActividad;
  END IF;
  UPDATE inscripcionActividadAcompanante iaa
    JOIN inscripcionActividad ia ON ia.idCliente=iaa.idClienteTitular AND ia.idActividad=iaa.idActividad
    SET iaa.fechaActividad=ia.fechaActividad WHERE iaa.fechaActividad IS NULL;
  ALTER TABLE inscripcionActividadAcompanante MODIFY fechaActividad DATE NOT NULL;

  -- Quita solo la FK vieja de dos columnas, si todavía existe.
  SELECT k.CONSTRAINT_NAME INTO clave
    FROM information_schema.KEY_COLUMN_USAGE k
   WHERE k.CONSTRAINT_SCHEMA=DATABASE() AND k.TABLE_NAME='inscripcionActividadAcompanante'
     AND k.REFERENCED_TABLE_NAME='inscripcionActividad'
     AND k.CONSTRAINT_NAME <> 'fk_inscripcion_acomp_inscripcion_fecha'
   LIMIT 1;
  IF clave IS NOT NULL THEN
    SET @sql=CONCAT('ALTER TABLE inscripcionActividadAcompanante DROP FOREIGN KEY `', clave, '`');
    PREPARE sentencia FROM @sql; EXECUTE sentencia; DEALLOCATE PREPARE sentencia;
  END IF;

  SELECT COUNT(*) INTO existe FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='inscripcionActividad' AND INDEX_NAME='uk_cliente_actividad';
  IF existe>0 THEN ALTER TABLE inscripcionActividad DROP INDEX uk_cliente_actividad; END IF;
  SELECT COUNT(*) INTO existe FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='inscripcionActividad' AND INDEX_NAME='uk_cliente_actividad_fecha';
  IF existe=0 THEN ALTER TABLE inscripcionActividad ADD UNIQUE KEY uk_cliente_actividad_fecha (idCliente,idActividad,fechaActividad); END IF;

  SELECT COUNT(*) INTO existe FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='inscripcionActividadAcompanante' AND INDEX_NAME='idx_iaa_acompanante';
  IF existe=0 THEN ALTER TABLE inscripcionActividadAcompanante ADD KEY idx_iaa_acompanante (idAcompañante); END IF;
  SELECT COUNT(*) INTO existe FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='inscripcionActividadAcompanante' AND INDEX_NAME='uk_acompanante_actividad';
  IF existe>0 THEN ALTER TABLE inscripcionActividadAcompanante DROP INDEX uk_acompanante_actividad; END IF;
  SELECT COUNT(*) INTO existe FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='inscripcionActividadAcompanante' AND INDEX_NAME='uk_acompanante_actividad_fecha';
  IF existe=0 THEN ALTER TABLE inscripcionActividadAcompanante ADD UNIQUE KEY uk_acompanante_actividad_fecha (idAcompañante,idActividad,fechaActividad); END IF;

  SELECT COUNT(*) INTO existe FROM information_schema.TABLE_CONSTRAINTS
   WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='inscripcionActividadAcompanante'
     AND CONSTRAINT_NAME='fk_inscripcion_acomp_inscripcion_fecha';
  IF existe=0 THEN
    ALTER TABLE inscripcionActividadAcompanante ADD CONSTRAINT fk_inscripcion_acomp_inscripcion_fecha
      FOREIGN KEY (idClienteTitular,idActividad,fechaActividad)
      REFERENCES inscripcionActividad(idCliente,idActividad,fechaActividad)
      ON DELETE CASCADE ON UPDATE CASCADE;
  END IF;

  SELECT COUNT(*) INTO existe FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='reservas' AND COLUMN_NAME='checkInReal';
  IF existe=0 THEN ALTER TABLE reservas ADD COLUMN checkInReal DATETIME NULL AFTER fechaIngreso; END IF;
  SELECT COUNT(*) INTO existe FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='reservas' AND COLUMN_NAME='checkOutReal';
  IF existe=0 THEN ALTER TABLE reservas ADD COLUMN checkOutReal DATETIME NULL AFTER fechaEgreso; END IF;
  SELECT COUNT(*) INTO existe FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='reservas' AND COLUMN_NAME='estadoLlegada';
  IF existe=0 THEN ALTER TABLE reservas ADD COLUMN estadoLlegada VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE_LLEGADA' AFTER checkOutReal; END IF;

  CREATE TABLE IF NOT EXISTS recuperacion_contrasena (
    idRecuperacion INT NOT NULL AUTO_INCREMENT, idCliente INT NOT NULL,
    codigo_hash VARCHAR(255) NOT NULL, venceEn DATETIME NOT NULL,
    intentos TINYINT UNSIGNED NOT NULL DEFAULT 0, creadoEn DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (idRecuperacion), UNIQUE KEY uk_recuperacion_cliente (idCliente),
    CONSTRAINT fk_recuperacion_cliente FOREIGN KEY (idCliente) REFERENCES cliente(idCliente)
      ON DELETE CASCADE ON UPDATE CASCADE
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
END//
DELIMITER ;
CALL reparar_base_el_molino();
DROP PROCEDURE reparar_base_el_molino;
