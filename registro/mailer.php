<?php
declare(strict_types=1);

use PHPMailer\PHPMailer\Exception;
use PHPMailer\PHPMailer\PHPMailer;

require_once __DIR__ . '/../vendor/phpmailer/phpmailer/src/Exception.php';
require_once __DIR__ . '/../vendor/phpmailer/phpmailer/src/SMTP.php';
require_once __DIR__ . '/../vendor/phpmailer/phpmailer/src/PHPMailer.php';

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

    public function enviarResumenCompra(array $pedido): void
    {
        if (empty($this->configuracion['username']) || empty($this->configuracion['password']) || empty($this->configuracion['from_email'])) {
            throw new RuntimeException('Falta configurar el correo emisor en registro/mail_config.php.');
        }

        $id = (int) ($pedido['idPedido'] ?? 0);
        $nombre = trim((string) ($pedido['nombre'] ?? '') . ' ' . (string) ($pedido['apellido'] ?? ''));
        $destinatario = (string) ($pedido['email'] ?? '');
        $metodo = (string) ($pedido['metodoPago'] ?? '');
        $total = number_format((float) ($pedido['total'] ?? 0), 2, ',', '.');
        $filas = '';
        $textoLineas = [];
        foreach (($pedido['lineas'] ?? []) as $linea) {
            $tipo = Utilidades::escapar((string) ($linea['alojamientoTipo'] ?? 'Alojamiento'));
            $numero = Utilidades::escapar((string) ($linea['alojamientoNumero'] ?? ''));
            $entrada = Utilidades::escapar((string) ($linea['fechaIngreso'] ?? ''));
            $salida = Utilidades::escapar((string) ($linea['fechaEgreso'] ?? ''));
            $personas = (int) ($linea['cantidadPersonas'] ?? 0);
            $noches = (int) ($linea['noches'] ?? 0);
            $subtotal = number_format((float) ($linea['subtotal'] ?? 0), 2, ',', '.');
            $filas .= "<tr><td>{$tipo} · {$numero}</td><td>{$entrada} al {$salida}</td><td>{$personas}</td><td>{$noches}</td><td>$ {$subtotal}</td></tr>";
            $textoLineas[] = "{$tipo} {$numero}: {$entrada} al {$salida}, {$personas} personas, {$noches} noches, subtotal $ {$subtotal}";
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
            $mail->Subject = "Resumen del pedido #{$id} - El Molino";
            $mail->Body = '<h1>Recibimos tu pedido</h1><p>Hola ' . Utilidades::escapar($nombre) . ',</p>'
                . '<p>Registramos tu pedido <strong>#' . $id . '</strong>. El pago todavía está pendiente; no se realizó ningún cobro en línea.</p>'
                . '<table border="1" cellpadding="8" cellspacing="0"><thead><tr><th>Alojamiento</th><th>Estadía</th><th>Personas</th><th>Noches</th><th>Subtotal</th></tr></thead><tbody>' . $filas . '</tbody></table>'
                . '<p>Método de pago elegido: <strong>' . Utilidades::escapar($metodo) . '</strong></p>'
                . '<p>Total del pedido: <strong>$ ' . $total . '</strong></p>'
                . '<p>El complejo coordinará la confirmación del pago y la estadía.</p>';
            $mail->AltBody = "Hola {$nombre},\n\nRecibimos tu pedido #{$id}. El pago está pendiente y no se realizó ningún cobro en línea.\n\n"
                . implode("\n", $textoLineas) . "\n\nMétodo de pago: {$metodo}\nTotal: $ {$total}\n\nEl complejo coordinará la confirmación.";
            $mail->send();
        } catch (Exception) {
            throw new RuntimeException('No se pudo enviar el resumen por correo: ' . $mail->ErrorInfo);
        }
    }
}
