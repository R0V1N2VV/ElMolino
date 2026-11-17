-- REINICIO DE PRUEBAS
-- Borra únicamente movimientos de prueba: señas, pagos, reservas,
-- acompañantes asignados a reservas, inscripciones y cargos de actividades.
-- CONSERVA clientes, usuarios, alojamientos, actividades y categorías.

START TRANSACTION;

DELETE FROM pago;
DELETE FROM reembolso;
DELETE FROM cargo_actividad;
DELETE FROM inscripcionActividadAcompanante;
DELETE FROM inscripcionActividad;
DELETE FROM reservaAcompañante;
DELETE FROM detalle_pedido_alojamiento;
DELETE FROM reservas;
DELETE FROM pedido_alojamiento;

COMMIT;
