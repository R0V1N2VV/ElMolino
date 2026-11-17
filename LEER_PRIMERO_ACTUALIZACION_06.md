# Actualización 06: actividades por día y cierre de estadía

Antes de abrir la web o la aplicación Java actualizadas, ejecutá una única vez
`BD/06_CORREGIR_FECHAS_ACTIVIDADES_Y_SALIDAS.sql` en la misma base MySQL que
usan ambas aplicaciones.

Esta actualización:

1. guarda el día elegido también para cada acompañante;
2. permite que una misma persona se anote a la misma actividad en días distintos;
3. corrige el cálculo de cupos e informes por fecha;
4. agrega el registro de check-in y check-out real para que el cierre deje de
   depender solamente de una fecha.

Hacé una copia de seguridad de la base antes de ejecutarla. No se debe ejecutar
dos veces: MySQL avisará que las columnas o índices ya existen si se repite.

Si phpMyAdmin mostró el error **#1553** al eliminar
`uk_acompanante_actividad`, no repitas 06: ejecutá una única vez
`BD/07_REPARAR_ACTUALIZACION_06_INTERRUMPIDA.sql`. Continúa sin perder datos.

Si después de 07 aparece el mensaje **#1061** sobre
`uk_cliente_actividad_fecha`, significa que esa clave ya se creó bien en 06.
Ejecutá entonces `BD/08_FINALIZAR_REPARACION_06.sql` para completar solamente
las consultas restantes.

Después, reemplazá la carpeta web y el proyecto Java por los de este paquete.
En la app Java, recepción puede editar exclusivamente el estado de una seña
pendiente (confirmarla o anularla); no puede cambiar importe, comprobante ni
la reserva asociada.
