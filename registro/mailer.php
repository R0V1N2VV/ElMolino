<?php
/**
 * ARCHIVO EXPLICADO: registro/mailer.php
 *
 * Prepara y envía los correos de registro y de confirmación de compra.
 * Las funciones de este archivo están documentadas junto a su declaración.
 */

declare(strict_types=1);

use PHPMailer\PHPMailer\Exception;
use PHPMailer\PHPMailer\PHPMailer;

require_once __DIR__ . '/../vendor/phpmailer/phpmailer/src/Exception.php';
require_once __DIR__ . '/../vendor/phpmailer/phpmailer/src/SMTP.php';
require_once __DIR__ . '/../vendor/phpmailer/phpmailer/src/PHPMailer.php';

final class Correo
{
    private array $configuracion;

    /**
     * Inicializa el objeto y deja preparadas las dependencias que utilizarán sus demás métodos.
     */
    public function __construct()
    {
        $archivo = __DIR__ . '/mail_config.php';
        $this->configuracion = file_exists($archivo) ? require $archivo : require __DIR__ . '/mail_config.example.php';
    }

    /**
     * Envía codigo registro al destinatario correspondiente.
     *
     * @param mixed $destinatario Valor de destinatario que recibe la función.
     * @param mixed $nombre Valor de nombre que recibe la función.
     * @param mixed $codigo Valor de codigo que recibe la función.
     */
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

    /**
     * Envía resumen compra al destinatario correspondiente.
     *
     * @param mixed $pedido Valor de pedido que recibe la función.
     */
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
        $sena = number_format((float) ($pedido['montoSena'] ?? 0), 2, ',', '.');
        $limiteSena = !empty($pedido['fechaLimiteSena'])
            ? (new DateTimeImmutable((string) $pedido['fechaLimiteSena']))->format('d/m/Y H:i')
            : 'a coordinar';
        $filas = '';
        $textoLineas = [];
        foreach (($pedido['lineas'] ?? []) as $linea) {
            $tipo = Utilidades::escapar((string) ($linea['alojamientoTipo'] ?? 'Alojamiento'));
            $entrada = Utilidades::escapar((string) ($linea['fechaIngreso'] ?? ''));
            $salida = Utilidades::escapar((string) ($linea['fechaEgreso'] ?? ''));
            $personas = (int) ($linea['cantidadPersonas'] ?? 0);
            $noches = (int) ($linea['noches'] ?? 0);
            $subtotal = number_format((float) ($linea['subtotal'] ?? 0), 2, ',', '.');
            $filas .= "<tr><td>{$tipo}</td><td>{$entrada} al {$salida}</td><td>{$personas}</td><td>{$noches}</td><td>$ {$subtotal}</td></tr>";
            $textoLineas[] = "{$tipo}: {$entrada} al {$salida}, {$personas} personas, {$noches} noches, subtotal $ {$subtotal}";
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
            $mail->Body = '<h1>Recibimos tu solicitud de reserva</h1><p>Hola ' . Utilidades::escapar($nombre) . ',</p>'
                . '<p>Registramos el pedido <strong>#' . $id . '</strong>. Para confirmar la estadía necesitás abonar una seña de <strong>$ ' . $sena . '</strong> antes del ' . Utilidades::escapar($limiteSena) . '.</p>'
                . '<table border="1" cellpadding="8" cellspacing="0"><thead><tr><th>Alojamiento</th><th>Estadía</th><th>Personas</th><th>Noches</th><th>Subtotal</th></tr></thead><tbody>' . $filas . '</tbody></table>'
                . '<p>Método de pago elegido: <strong>' . Utilidades::escapar($metodo) . '</strong></p>'
                . '<p>Total del pedido: <strong>$ ' . $total . '</strong></p>'
                . '<p>La reserva queda pendiente hasta acreditar la seña. El complejo enviará los datos necesarios para completar el pago.</p>';
            $mail->AltBody = "Hola {$nombre},\n\nRecibimos tu solicitud #{$id}. Para confirmar la estadía necesitás abonar una seña de $ {$sena} antes del {$limiteSena}.\n\n"
                . implode("\n", $textoLineas) . "\n\nMedio elegido: {$metodo}\nTotal: $ {$total}\n\nLa reserva queda pendiente hasta acreditar la seña.";
            $mail->send();
        } catch (Exception) {
            throw new RuntimeException('No se pudo enviar el resumen por correo: ' . $mail->ErrorInfo);
        }
    }
}
