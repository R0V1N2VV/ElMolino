-- Ejecutar una sola vez, después de 05_ACTUALIZAR_PRE_RESERVAS_Y_REVISION.sql.
-- Corrige el modelo para que cada inscripción sea por día y no por actividad general.
START TRANSACTION;

ALTER TABLE inscripcionActividadAcompanante
  ADD COLUMN fechaActividad DATE NULL AFTER idActividad;

UPDATE inscripcionActividadAcompanante iaa
JOIN inscripcionActividad ia
  ON ia.idCliente=iaa.idClienteTitular AND ia.idActividad=iaa.idActividad
SET iaa.fechaActividad=ia.fechaActividad
WHERE iaa.fechaActividad IS NULL;

ALTER TABLE inscripcionActividadAcompanante MODIFY fechaActividad DATE NOT NULL;
ALTER TABLE inscripcionActividadAcompanante DROP FOREIGN KEY fk_inscripcion_acomp_inscripcion;
ALTER TABLE inscripcionActividad DROP INDEX uk_cliente_actividad;
ALTER TABLE inscripcionActividad ADD UNIQUE KEY uk_cliente_actividad_fecha (idCliente,idActividad,fechaActividad);
-- La FK del acompañante necesita un índice propio antes de quitar el índice único anterior.
ALTER TABLE inscripcionActividadAcompanante ADD KEY idx_iaa_acompanante (idAcompañante);
ALTER TABLE inscripcionActividadAcompanante DROP INDEX uk_acompanante_actividad;
ALTER TABLE inscripcionActividadAcompanante ADD UNIQUE KEY uk_acompanante_actividad_fecha (idAcompañante,idActividad,fechaActividad);
ALTER TABLE inscripcionActividadAcompanante
  ADD CONSTRAINT fk_inscripcion_acomp_inscripcion_fecha
  FOREIGN KEY (idClienteTitular,idActividad,fechaActividad)
  REFERENCES inscripcionActividad(idCliente,idActividad,fechaActividad)
  ON DELETE CASCADE ON UPDATE CASCADE;

-- La salida se registra por recepción; no se infiere solamente desde la fecha.
ALTER TABLE reservas ADD COLUMN checkInReal DATETIME NULL AFTER fechaIngreso;
ALTER TABLE reservas ADD COLUMN checkOutReal DATETIME NULL AFTER fechaEgreso;

COMMIT;
