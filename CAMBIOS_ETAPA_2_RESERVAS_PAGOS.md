# Etapa 2 — Reservas, pagos y alojamiento

Esta etapa deja demostrable el recorrido completo entre la página, MySQL y la aplicación Java. El pago es **manual por transferencia**: el cliente informa el movimiento y administración confirma que el dinero ingresó.

## Indicadores resueltos

- **C-01 — Seña previa:** cada pedido calcula una seña del 30 %, tiene vencimiento y la reserva queda pendiente hasta acreditarla.
- **C-03 — Importe real:** el cliente informa cuánto transfirió. Ya no se guarda automáticamente el valor teórico de la seña.
- **C-04 — Confirmación administrativa:** desde `Pagos` en Java se confirma una seña o un saldo. El pedido y todas sus reservas se recalculan automáticamente.
- **C-05 — Vencimiento y liberación:** si vence el plazo sin alcanzar la seña, el pedido pasa a `VENCIDO`, sus reservas se cancelan y el alojamiento vuelve a estar disponible.
- **C-08 — Cancelaciones y reintegros:** cancelar antes del ingreso recalcula total y seña. Si corresponde devolver dinero, se crea un registro `PENDIENTE` en `Reintegros` para que administración lo confirme.
- **A-01 — Estadía válida para actividades:** las actividades solo permanecen activas si el titular o acompañante tiene una estadía confirmada para ese día.
- **A-02 — Cobro trazable:** pagos de seña y saldo se registran por separado y muestran monto, medio, estado, fecha y comprobante.
- **A-11 — Coherencia al cancelar:** al cancelar una estadía también se revisan participantes, cupos y cargos de actividades.

## Preparación obligatoria de la base

1. Hacer una copia de seguridad.
2. Ejecutar `BD/00_ACTUALIZAR_BASE_EXISTENTE.sql` para actualizar una base anterior, o importar `BD/s27_El_Molino_completa.sql` si se crea desde cero.
3. Como alternativa mínima para esta etapa, ejecutar `BD/02_ACTUALIZAR_RESERVAS_PAGOS.sql` y después `BD/04_ACTUALIZAR_LOGICA_ACTIVIDADES.sql`.

## Demostración sugerida

1. Iniciar sesión como cliente y reservar un alojamiento.
2. Informar una transferencia por el importe real y copiar su referencia.
3. Abrir Java con un usuario de Administración, entrar a `Pagos`, localizar el movimiento y cambiarlo a `CONFIRMADO`.
4. Volver a `Mi cuenta`: la estadía debe aparecer confirmada y mostrar pagado/saldo.
5. Inscribirse a una actividad en un día incluido en la estadía.
6. Cancelar el alojamiento antes del ingreso: la actividad se revisa y, si había dinero acreditado, aparece un reintegro pendiente en Java.
7. En `Reintegros`, completar medio y comprobante y cambiar el estado a `CONFIRMADO`.

## Regla importante

Agregar un alojamiento al carrito no lo bloquea para otros usuarios. Se bloquea al crear el pedido/reserva; si la seña vence, se libera automáticamente. Esto evita que un carrito abandonado quite disponibilidad real.
