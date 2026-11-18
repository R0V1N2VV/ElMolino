<?php
declare(strict_types=1);

/** Guarda avisos dentro de Mi cuenta para no perder cambios de una actividad. */
final class GestorAvisosCliente
{
    public function __construct(private PDO $conexion) {}

    /** Notifica a todos los titulares con una inscripción activa en la actividad. */
    public function avisarCambioActividad(int $idActividad, string $titulo, string $mensaje): void
    {
        try {
            $insertar = $this->conexion->prepare(
                "INSERT INTO notificacionCliente (idCliente,idActividad,tipo,titulo,mensaje,creadoEn)
                 SELECT DISTINCT ia.idCliente,:actividad,'ACTIVIDAD',:titulo,:mensaje,NOW()
                   FROM inscripcionActividad ia
                  WHERE ia.idActividad=:actividad2 AND UPPER(ia.estado)='ACTIVA'"
            );
            $insertar->execute([
                'actividad' => $idActividad,
                'actividad2' => $idActividad,
                'titulo' => $titulo,
                'mensaje' => $mensaje,
            ]);
        } catch (PDOException $error) {
            // La mejora es compatible con bases anteriores: la actividad se guarda
            // igual y solo se omite el aviso hasta importar la actualización 12.
            error_log('No se pudo crear el aviso de actividad: ' . $error->getMessage());
        }
    }

    /** Marca como leído únicamente un aviso que pertenece a la cuenta actual. */
    public function marcarLeido(int $idAviso, int $idCliente): void
    {
        $actualizar = $this->conexion->prepare(
            'UPDATE notificacionCliente SET leidoEn=COALESCE(leidoEn,NOW()) WHERE idNotificacion=:aviso AND idCliente=:cliente'
        );
        $actualizar->execute(['aviso' => $idAviso, 'cliente' => $idCliente]);
    }
}
