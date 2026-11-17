<?php
/**
 * ARCHIVO EXPLICADO: registro/mail_config.php
 *
 * Forma parte del flujo “mail config” del sitio El Molino.
 * Las funciones de este archivo están documentadas junto a su declaración.
 */

return [
    // Estas variables se configuran en el hosting. Nunca guardar una clave de
    // aplicación dentro del ZIP o del repositorio.
    'host' => getenv('EL_MOLINO_SMTP_HOST') ?: 'smtp.gmail.com',
    'port' => (int) (getenv('EL_MOLINO_SMTP_PORT') ?: 587),
    'username' => getenv('EL_MOLINO_SMTP_USUARIO') ?: 'centrorecreativoelmolino403@gmail.com',
    'password' => getenv('EL_MOLINO_SMTP_CONTRASENA') ?: 'gmxs hkoy pxbl ybzg',
    'from_email' => getenv('EL_MOLINO_SMTP_DESDE') ?: 'centrorecreativoelmolino403@gmail.com',
    'from_name' => 'Centro Recreativo El Molino',
];
