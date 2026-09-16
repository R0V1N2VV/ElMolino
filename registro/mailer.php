<?php
require_once __DIR__ . '/../vendor/autoload.php';

use PHPMailer\PHPMailer\Exception;
use PHPMailer\PHPMailer\PHPMailer;

function enviarCodigoRegistro(string $destinatario, string $nombre, string $codigo, ?string &$error = null): bool
{
    $config = require __DIR__ . '/mail_config.php';

    if (empty($config['username']) || empty($config['password']) || empty($config['from_email'])) {
        $error = 'Falta configurar el correo emisor en registro/mail_config.php.';
        return false;
    }

    $mail = new PHPMailer(true);

    try {
        $mail->isSMTP();
        $mail->Host = $config['host'];
        $mail->SMTPAuth = true;
        $mail->Username = $config['username'];
        $mail->Password = $config['password'];
        $mail->SMTPSecure = PHPMailer::ENCRYPTION_STARTTLS;
        $mail->Port = $config['port'];
        $mail->CharSet = 'UTF-8';

        $mail->setFrom($config['from_email'], $config['from_name']);
        $mail->addAddress($destinatario, $nombre);

        $mail->isHTML(true);
        $mail->Subject = 'Codigo de verificacion - El Molino';
        $mail->Body = '<p>Hola ' . htmlspecialchars($nombre, ENT_QUOTES, 'UTF-8') . ',</p>'
            . '<p>Tu codigo para terminar el registro en El Molino es:</p>'
            . '<h2 style="letter-spacing:4px;">' . htmlspecialchars($codigo, ENT_QUOTES, 'UTF-8') . '</h2>'
            . '<p>Este codigo vence en 15 minutos.</p>';
        $mail->AltBody = "Hola {$nombre},\n\nTu codigo para terminar el registro en El Molino es: {$codigo}\n\nEste codigo vence en 15 minutos.";

        return $mail->send();
    } catch (Exception $e) {
        $error = 'No se pudo enviar el correo: ' . $mail->ErrorInfo;
        return false;
    }
}
