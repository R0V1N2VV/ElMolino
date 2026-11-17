-- Usar solamente después de que 07 mostró:
-- "Nombre de clave duplicado uk_cliente_actividad_fecha".
-- Esa clave YA fue creada correctamente; aquí se completa lo restante.

ALTER TABLE inscripcionActividadAcompanante
  ADD UNIQUE KEY uk_acompanante_actividad_fecha (idAcompañante,idActividad,fechaActividad);

ALTER TABLE inscripcionActividadAcompanante
  ADD CONSTRAINT fk_inscripcion_acomp_inscripcion_fecha
  FOREIGN KEY (idClienteTitular,idActividad,fechaActividad)
  REFERENCES inscripcionActividad(idCliente,idActividad,fechaActividad)
  ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE reservas ADD COLUMN checkInReal DATETIME NULL AFTER fechaIngreso;
ALTER TABLE reservas ADD COLUMN checkOutReal DATETIME NULL AFTER fechaEgreso;
