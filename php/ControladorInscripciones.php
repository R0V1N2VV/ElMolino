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
        if (!in_array($accion, ['guardar_inscripcion', 'inscribirse', 'actualizar_acompanantes'], true)) {
            throw new InvalidArgumentException('La acción solicitada no es válida.');
        }

        $acompanantes = is_array($entrada['acompanantes'] ?? null) ? $entrada['acompanantes'] : [];
        $participaTitular = isset($entrada['participa_titular']);
        // Compatibilidad con formularios anteriores, donde el titular siempre participaba.
        if ($accion === 'inscribirse' || $accion === 'actualizar_acompanantes') $participaTitular = true;

        $yaEstabaInscripto = $this->repositorio->estaInscripto($actividad->id, $idCliente);
        $this->repositorio->guardarParticipantes(
            $actividad->id,
            $idCliente,
            $participaTitular,
            $acompanantes
        );
        if ($actividad->modalidad === 'inscripcion' && $actividad->precio > 0) {
            return ($yaEstabaInscripto ? 'Actualizamos las personas que participan.' : 'La inscripción quedó registrada.')
                . ' El importe se cobrará al finalizar la estadía.';
        }
        return $yaEstabaInscripto
            ? 'Actualizamos las personas que participan en esta actividad.'
            : 'La inscripción quedó registrada.';
    }
}
