-- Usar SOLAMENTE si 06 se detuvo con el error 1553 al borrar
-- uk_acompanante_actividad. No borra inscripciones ni reservas.
-- Continúa desde el punto exacto en el que quedó la actualización.

ALTER TABLE inscripcionActividadAcompanante
  ADD KEY idx_iaa_acompanante (idAcompañante);

ALTER TABLE inscripcionActividadAcompanante
  DROP INDEX uk_acompanante_actividad;

ALTER TABLE inscripcionActividad
  ADD UNIQUE KEY uk_cliente_actividad_fecha (idCliente,idActividad,fechaActividad);

ALTER TABLE inscripcionActividadAcompanante
  ADD UNIQUE KEY uk_acompanante_actividad_fecha (idAcompañante,idActividad,fechaActividad);

ALTER TABLE inscripcionActividadAcompanante
  ADD CONSTRAINT fk_inscripcion_acomp_inscripcion_fecha
  FOREIGN KEY (idClienteTitular,idActividad,fechaActividad)
  REFERENCES inscripcionActividad(idCliente,idActividad,fechaActividad)
  ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE reservas ADD COLUMN checkInReal DATETIME NULL AFTER fechaIngreso;
ALTER TABLE reservas ADD COLUMN checkOutReal DATETIME NULL AFTER fechaEgreso;
