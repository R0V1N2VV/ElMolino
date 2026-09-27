<?php
declare(strict_types=1);

use PHPMailer\PHPMailer\Exception;
use PHPMailer\PHPMailer\PHPMailer;

final class Correo
{
    private array $configuracion;

    public function __construct()
    {
        $archivo = __DIR__ . '/mail_config.php';
        $this->configuracion = file_exists($archivo) ? require $archivo : require __DIR__ . '/mail_config.example.php';
    }

    public function enviarCodigoRegistro(string $destinatario, string $nombre, string $codigo): void
    {
        if (empty($this->configuracion['username']) || empty($this->configuracion['password']) || empty($this->configuracion['from_email'])) {
            throw new RuntimeException('Falta configurar la contraseña de aplicación en registro/mail_config.php.');
        }
        $mail = new PHPMailer(true);
        try {
            $mail->isSMTP();
            $mail->Host = $this->configuracion['host'];
            $mail->SMTPAuth = true;
            $mail->Username = $this->configuracion['username'];
            $mail->Password = $this->configuracion['password'];
            $mail->SMTPSecure = PHPMailer::ENCRYPTION_STARTTLS;
            $mail->Port = $this->configuracion['port'];
            $mail->CharSet = 'UTF-8';
            $mail->setFrom($this->configuracion['from_email'], $this->configuracion['from_name']);
            $mail->addAddress($destinatario, $nombre);
            $mail->isHTML(true);
            $mail->Subject = 'Código de verificación - El Molino';
            $mail->Body = '<p>Hola ' . Utilidades::escapar($nombre) . ',</p><p>Tu código para terminar el registro en El Molino es:</p><h2 style="letter-spacing:4px;">' . Utilidades::escapar($codigo) . '</h2><p>Este código vence en 15 minutos.</p>';
            $mail->AltBody = "Hola {$nombre},\n\nTu código para terminar el registro en El Molino es: {$codigo}\n\nEste código vence en 15 minutos.";
            $mail->send();
        } catch (Exception) {
            throw new RuntimeException('No se pudo enviar el correo: ' . $mail->ErrorInfo);
        }
    }
}
