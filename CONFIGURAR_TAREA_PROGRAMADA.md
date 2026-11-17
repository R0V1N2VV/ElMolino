# Vencimiento automático de señas

En el panel del hosting creá una tarea cron que se ejecute cada 5 minutos:

`php /ruta/completa/del/proyecto/php/tarea_vencimientos.php`

Así las pre-reservas vencidas se liberan aunque nadie entre a la web o abra la
aplicación Java. Ajustá la ruta según la carpeta real publicada por el hosting.
