# Orden para actualizar la base existente

1. Ejecutá solamente `BD/11_REPARAR_BASE_COMPLETA.sql`. Es seguro aunque los
   archivos 06, 07 u 08 hayan quedado a mitad de camino.
2. No hace falta ejecutar 06, 07, 08 ni 09 después de usar 11.

La actualización 11 también agrega el estado de llegada de cada reserva. Una
seña confirmada deja la estadía como `PENDIENTE_LLEGADA`: recepción debe marcar
`EN_ESTADIA` cuando el huésped llega o `NO_SE_PRESENTO` si no llega. El cierre
se realiza desde **Cobrar estadía final**, que registra la salida y envía el
alojamiento a limpieza.

No ejecutes nuevamente 06 sobre esta base ya actualizada. Los archivos 07 y 08
solo son reparadores para la ejecución que se interrumpió; no borran datos.

Después reemplazá todos los archivos web y el proyecto Java con los del ZIP.
Para que los vencimientos de señas se liberen aunque nadie abra el sistema,
configurá la tarea explicada en `CONFIGURAR_TAREA_PROGRAMADA.md`.

Si querés volver a probar desde cero sin borrar clientes, alojamientos ni
actividades, ejecutá `BD/10_REINICIAR_DATOS_DE_PRUEBA.sql`.
