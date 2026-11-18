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

  -- En bases antiguas la única clave de idAcompañante también sostenía la
  -- FK hacia acompañante. Agregar otro índice no cambia automáticamente la
  -- FK en MySQL, por eso se la recrea antes de retirar la clave vieja.
  SELECT COUNT(*) INTO existe FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='inscripcionActividadAcompanante'
     AND INDEX_NAME='uk_acompanante_actividad';
  IF existe>0 THEN
    SET clave=NULL;
    SELECT k.CONSTRAINT_NAME INTO clave
      FROM information_schema.KEY_COLUMN_USAGE k
     WHERE k.CONSTRAINT_SCHEMA=DATABASE() AND k.TABLE_NAME='inscripcionActividadAcompanante'
       AND k.REFERENCED_TABLE_NAME='acompañante'
     LIMIT 1;
    IF clave IS NOT NULL THEN
      SET @sql=CONCAT('ALTER TABLE inscripcionActividadAcompanante DROP FOREIGN KEY `', clave, '`');
      PREPARE sentencia FROM @sql; EXECUTE sentencia; DEALLOCATE PREPARE sentencia;
    END IF;
  END IF;
  SELECT COUNT(*) INTO existe FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='inscripcionActividadAcompanante' AND INDEX_NAME='uk_acompanante_actividad';
  IF existe>0 THEN ALTER TABLE inscripcionActividadAcompanante DROP INDEX uk_acompanante_actividad; END IF;
  SELECT COUNT(*) INTO existe FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='inscripcionActividadAcompanante' AND INDEX_NAME='uk_acompanante_actividad_fecha';
  IF existe=0 THEN ALTER TABLE inscripcionActividadAcompanante ADD UNIQUE KEY uk_acompanante_actividad_fecha (idAcompañante,idActividad,fechaActividad); END IF;

  SELECT COUNT(*) INTO existe FROM information_schema.TABLE_CONSTRAINTS
   WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='inscripcionActividadAcompanante'
     AND CONSTRAINT_NAME='fk_inscripcion_acomp_persona';
  IF existe=0 THEN
    ALTER TABLE inscripcionActividadAcompanante ADD CONSTRAINT fk_inscripcion_acomp_persona
      FOREIGN KEY (idAcompañante) REFERENCES `acompañante`(idAcompañante)
      ON DELETE CASCADE ON UPDATE CASCADE;
  END IF;

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

  -- Campos que habilitan registro, comprobantes y el estado "seña en revisión".
  -- Se agregan por separado para que el reparador también sirva en una base que
  -- ya tenía las tablas creadas antes de estas funcionalidades.
  SELECT COUNT(*) INTO existe FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='cliente' AND COLUMN_NAME='password_hash';
  IF existe=0 THEN ALTER TABLE cliente ADD COLUMN password_hash VARCHAR(255) NULL AFTER direccion; END IF;

  SELECT COUNT(*) INTO existe FROM information_schema.TABLES
   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='registro_pendiente';
  IF existe=0 THEN
    CREATE TABLE registro_pendiente (
      idRegistroPendiente INT NOT NULL AUTO_INCREMENT, dni VARCHAR(10) NOT NULL,
      nombre VARCHAR(50) NOT NULL, apellido VARCHAR(50) NOT NULL,
      telefono VARCHAR(20) NOT NULL, email VARCHAR(100) NOT NULL,
      direccion VARCHAR(150) NOT NULL, acompanantes_json LONGTEXT NULL,
      password_hash VARCHAR(255) NOT NULL, codigo_hash VARCHAR(255) NOT NULL,
      vence_en DATETIME NOT NULL, intentos TINYINT UNSIGNED NOT NULL DEFAULT 0,
      creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
      actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      PRIMARY KEY (idRegistroPendiente), UNIQUE KEY uk_registro_pendiente_email (email),
      UNIQUE KEY uk_registro_pendiente_dni (dni)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
  ELSE
    SELECT COUNT(*) INTO existe FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='registro_pendiente' AND COLUMN_NAME='acompanantes_json';
    IF existe=0 THEN ALTER TABLE registro_pendiente ADD COLUMN acompanantes_json LONGTEXT NULL AFTER direccion; END IF;
  END IF;

  SELECT COUNT(*) INTO existe FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pedido_alojamiento' AND COLUMN_NAME='estadoPedido';
  IF existe=0 THEN ALTER TABLE pedido_alojamiento ADD COLUMN estadoPedido VARCHAR(30) NOT NULL DEFAULT 'ESPERANDO_SENA' AFTER metodoPago; END IF;
  SELECT COUNT(*) INTO existe FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pedido_alojamiento' AND COLUMN_NAME='estadoPago';
  IF existe=0 THEN ALTER TABLE pedido_alojamiento ADD COLUMN estadoPago VARCHAR(30) NOT NULL DEFAULT 'SENA_PENDIENTE' AFTER estadoPedido; END IF;
  SELECT COUNT(*) INTO existe FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pedido_alojamiento' AND COLUMN_NAME='porcentajeSena';
  IF existe=0 THEN ALTER TABLE pedido_alojamiento ADD COLUMN porcentajeSena DECIMAL(5,2) NOT NULL DEFAULT 30.00 AFTER estadoPago; END IF;
  SELECT COUNT(*) INTO existe FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pedido_alojamiento' AND COLUMN_NAME='montoSena';
  IF existe=0 THEN ALTER TABLE pedido_alojamiento ADD COLUMN montoSena DECIMAL(10,2) NOT NULL DEFAULT 0.00 AFTER porcentajeSena; END IF;
  SELECT COUNT(*) INTO existe FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pedido_alojamiento' AND COLUMN_NAME='fechaLimiteSena';
  IF existe=0 THEN ALTER TABLE pedido_alojamiento ADD COLUMN fechaLimiteSena DATETIME NULL AFTER montoSena; END IF;
  SELECT COUNT(*) INTO existe FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pedido_alojamiento' AND COLUMN_NAME='fechaLimiteRevisionSena';
  IF existe=0 THEN ALTER TABLE pedido_alojamiento ADD COLUMN fechaLimiteRevisionSena DATETIME NULL AFTER fechaLimiteSena; END IF;
  SELECT COUNT(*) INTO existe FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pedido_alojamiento' AND COLUMN_NAME='senaPagadaEn';
  IF existe=0 THEN ALTER TABLE pedido_alojamiento ADD COLUMN senaPagadaEn DATETIME NULL AFTER fechaLimiteRevisionSena; END IF;
  SELECT COUNT(*) INTO existe FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pedido_alojamiento' AND COLUMN_NAME='canceladoEn';
  IF existe=0 THEN ALTER TABLE pedido_alojamiento ADD COLUMN canceladoEn DATETIME NULL AFTER senaPagadaEn; END IF;
  UPDATE pedido_alojamiento SET montoSena=ROUND(total*porcentajeSena/100,2) WHERE montoSena=0;

  SELECT COUNT(*) INTO existe FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pago' AND COLUMN_NAME='tipoPago';
  IF existe=0 THEN ALTER TABLE pago ADD COLUMN tipoPago VARCHAR(20) NOT NULL DEFAULT 'SENA' AFTER metodoPago; END IF;
  SELECT COUNT(*) INTO existe FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pago' AND COLUMN_NAME='estado';
  IF existe=0 THEN ALTER TABLE pago ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'CONFIRMADO' AFTER tipoPago; END IF;
  SELECT COUNT(*) INTO existe FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pago' AND COLUMN_NAME='archivoComprobante';
  IF existe=0 THEN ALTER TABLE pago ADD COLUMN archivoComprobante VARCHAR(255) NULL AFTER comprobante; END IF;
  SELECT COUNT(*) INTO existe FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pago' AND COLUMN_NAME='observacion';
  IF existe=0 THEN ALTER TABLE pago ADD COLUMN observacion VARCHAR(255) NULL AFTER archivoComprobante; END IF;

  CREATE TABLE IF NOT EXISTS recuperacion_contrasena (
    idRecuperacion INT NOT NULL AUTO_INCREMENT, idCliente INT NOT NULL,
    codigo_hash VARCHAR(255) NOT NULL, venceEn DATETIME NOT NULL,
    intentos TINYINT UNSIGNED NOT NULL DEFAULT 0, creadoEn DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (idRecuperacion), UNIQUE KEY uk_recuperacion_cliente (idCliente),
    CONSTRAINT fk_recuperacion_cliente FOREIGN KEY (idCliente) REFERENCES cliente(idCliente)
      ON DELETE CASCADE ON UPDATE CASCADE
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

  CREATE TABLE IF NOT EXISTS configuracionDemostracion (
    idConfiguracion TINYINT NOT NULL, activo TINYINT(1) NOT NULL DEFAULT 0,
    fecha DATE NULL, hora TIME NULL, actualizadoEn DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizadoPor INT NULL, PRIMARY KEY (idConfiguracion)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
  INSERT INTO configuracionDemostracion (idConfiguracion,activo,fecha,hora,actualizadoEn,actualizadoPor)
  VALUES (1,0,NULL,NULL,NOW(),NULL)
  ON DUPLICATE KEY UPDATE idConfiguracion=VALUES(idConfiguracion);
END//
DELIMITER ;
CALL reparar_base_el_molino();
DROP PROCEDURE reparar_base_el_molino;
