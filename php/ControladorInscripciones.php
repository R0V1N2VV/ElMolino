<?php
declare(strict_types=1);

final class ControladorInscripciones
{
    public function __construct(private RepositorioInscripciones $repositorio) {}

    public function procesar(array $entrada, ?array $usuario, Actividad $actividad): string
    {
        if (!Sesion::validarCsrf($entrada['token_csrf'] ?? null)) {
            throw new RuntimeException('La sesión del formulario venció. Volvé a intentarlo.');
        }
        $idCliente = (int) ($usuario['id_cliente'] ?? 0);
        if ($idCliente <= 0) {
            throw new RuntimeException('Iniciá sesión con una cuenta de cliente para inscribirte.');
        }

        $accion = (string) ($entrada['accion'] ?? '');
        if ($accion === 'cancelar_inscripcion') {
            $this->repositorio->cancelar($actividad->id, $idCliente);
            return 'Tu inscripción fue cancelada y el lugar volvió a quedar disponible.';
        }
        if ($accion !== 'inscribirse') {
            throw new InvalidArgumentException('La acción solicitada no es válida.');
        }

        $this->repositorio->inscribir($actividad->id, $idCliente);
        if ($actividad->modalidad === 'inscripcion' && $actividad->precio > 0) {
            return 'Tu inscripción quedó registrada. Esta actividad tiene un valor de ' . Vista::precio($actividad) . '.';
        }
        return 'Tu inscripción quedó registrada.';
    }
}
