# El Molino

## Registro e inicio de sesión

1. Importá la base principal y luego ejecutá `BD/registro_pendiente.sql` en la misma base (`s27_El Molino`). Esta tabla guarda temporalmente los datos y el código hasta verificar el correo.
2. Copiá `registro/mail_config.example.php` como `registro/mail_config.php` y completá `password` con una contraseña de aplicación de Gmail para `centrorecreativoelmolino403@gmail.com`.
3. Si tu MySQL no usa `root` sin contraseña, definí las variables de entorno `EL_MOLINO_DB_HOST`, `EL_MOLINO_DB_PUERTO`, `EL_MOLINO_DB_NOMBRE`, `EL_MOLINO_DB_USUARIO` y `EL_MOLINO_DB_CONTRASENA`.

El alta ocurre en tres pasos: datos personales, contraseña y verificación por correo. Al confirmar el código, se crean los registros de `cliente` y `usuarios` dentro de una misma transacción.
