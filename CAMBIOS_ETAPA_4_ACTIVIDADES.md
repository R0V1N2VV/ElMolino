# Etapa 4 — Arreglos de actividades

## Qué se corrigió

- Las actividades pagas ahora crean un registro en `cargo_actividad`, vinculado al pedido de la estadía.
- El cargo conserva el precio aceptado al inscribirse, aunque después cambie el precio de la actividad.
- Si cambian los participantes, el total se recalcula con ese precio histórico.
- Las inscripciones canceladas ya no se borran: quedan con estado `CANCELADA`.
- Los acompañantes retirados dejan de ocupar cupo, pero su registro histórico se conserva.
- Cancelar una estadía revisa automáticamente las actividades del titular y sus acompañantes.
- Si todavía existe otra estadía válida para ese día, la inscripción se mantiene.
- Si ya no queda ninguna persona válida en la inscripción, se cancela y su cargo pendiente se anula.
- “Mi cuenta” muestra el cargo pendiente de cada actividad paga.
- Los informes Java cuentan titulares y acompañantes por fecha.
- El informe de pagos Java solo suma pagos confirmados y resta los reintegros.
- Java incorpora un informe de cargos de actividades.

## Paso obligatorio para una base existente

Antes de abrir la web o la aplicación Java actualizadas, ejecutar una vez:

`BD/04_ACTUALIZAR_LOGICA_ACTIVIDADES.sql`

El archivo agrega las columnas y la tabla que necesita esta etapa. También
genera cargos pendientes para las inscripciones pagas futuras que ya existían.

## Pruebas recomendadas

1. Inscribir al titular en una actividad paga y comprobar el cargo.
2. Agregar un acompañante y comprobar que aumenta el total y el cupo ocupado.
3. Quitar al acompañante de la reserva y comprobar que baja el total.
4. Cancelar la estadía y comprobar que la inscripción/cargo se anulan si no hay otra estadía válida.
5. Cambiar el precio de la actividad y confirmar que el cargo existente mantiene el precio anterior.
6. Cancelar una inscripción y comprobar que sigue visible en la base como historial.
7. Abrir los informes Java y comparar el cupo de una fecha con los participantes reales.
