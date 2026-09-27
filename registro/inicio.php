<?php
declare(strict_types=1);

require_once __DIR__ . '/../vendor/autoload.php';

spl_autoload_register(static function (string $clase): void {
    $archivo = __DIR__ . '/clases/' . $clase . '.php';
    if (is_file($archivo)) {
        require_once $archivo;
    }
});
