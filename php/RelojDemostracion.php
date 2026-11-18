<?php
/**
 * Reloj compartido exclusivamente para exponer el recorrido del sistema.
 *
 * El valor se guarda en la misma base que usan la web y la aplicación Java.
 * Por eso ambos lados pueden validar una misma fecha y hora sin cambiar el
 * reloj de la computadora. Solo Superadmin puede modificarlo desde la web.
 */

declare(strict_types=1);

final class RelojDemostracion
{
    private const ZONA_HORARIA = 'America/Argentina/Buenos_Aires';

    private function __construct() {}

    /**
     * Obtiene el estado actual del modo demostración.
     * Si todavía no se importó la tabla, informa que falta instalarlo sin
     * interrumpir el resto de la web.
     *
     * @return array{instalado:bool,activo:bool,fecha:?string,hora:?string,actualizadoEn:?string}
     */
    public static function estado(PDO $conexion): array
    {
        try {
            $consulta = $conexion->query(
                'SELECT activo, fecha, hora, actualizadoEn
                   FROM configuracionDemostracion
                  WHERE idConfiguracion=1 LIMIT 1'
            );
            $fila = $consulta->fetch();
            if (!$fila) {
                return ['instalado' => true, 'activo' => false, 'fecha' => null, 'hora' => null, 'actualizadoEn' => null];
            }
            return [
                'instalado' => true,
                'activo' => (bool) $fila['activo'],
                'fecha' => $fila['fecha'] !== null ? (string) $fila['fecha'] : null,
                'hora' => $fila['hora'] !== null ? substr((string) $fila['hora'], 0, 5) : null,
                'actualizadoEn' => $fila['actualizadoEn'] !== null ? (string) $fila['actualizadoEn'] : null,
            ];
        } catch (PDOException) {
            return ['instalado' => false, 'activo' => false, 'fecha' => null, 'hora' => null, 'actualizadoEn' => null];
        }
    }

    /**
     * Devuelve el instante que deben usar las reglas del sitio. Cuando el modo
     * no está activo conserva la fecha y hora reales de Argentina. Centralizar
     * esta decisión evita que la web y la aplicación Java calculen resultados
     * distintos durante una demostración.
     */
    public static function ahora(PDO $conexion): DateTimeImmutable
    {
        $estado = self::estado($conexion);
        if ($estado['activo'] && $estado['fecha'] && $estado['hora']) {
            $momento = DateTimeImmutable::createFromFormat(
                '!Y-m-d H:i',
                $estado['fecha'] . ' ' . $estado['hora'],
                new DateTimeZone(self::ZONA_HORARIA)
            );
            if ($momento !== false) return $momento;
        }
        return new DateTimeImmutable('now', new DateTimeZone(self::ZONA_HORARIA));
    }

    /** Devuelve solo la fecha operativa en formato seguro para las consultas. */
    public static function fecha(PDO $conexion): string
    {
        return self::ahora($conexion)->format('Y-m-d');
    }

    /** Activa el reloj y verifica que fecha y hora tengan formatos reales. */
    public static function activar(PDO $conexion, string $fecha, string $hora, ?int $idUsuario): void
    {
        $fecha = trim($fecha);
        $hora = trim($hora);
        $momento = DateTimeImmutable::createFromFormat(
            '!Y-m-d H:i',
            $fecha . ' ' . $hora,
            new DateTimeZone(self::ZONA_HORARIA)
        );
        $errores = DateTimeImmutable::getLastErrors();
        $valido = $momento
            && $momento->format('Y-m-d H:i') === $fecha . ' ' . $hora
            && ($errores === false || ($errores['warning_count'] === 0 && $errores['error_count'] === 0));
        if (!$valido) {
            throw new InvalidArgumentException('Elegí una fecha y una hora válidas para la demostración.');
        }

        $guardar = $conexion->prepare(
            "INSERT INTO configuracionDemostracion
                    (idConfiguracion, activo, fecha, hora, actualizadoEn, actualizadoPor)
             VALUES (1, 1, :fecha, :hora, NOW(), :usuario)
             ON DUPLICATE KEY UPDATE activo=1, fecha=VALUES(fecha), hora=VALUES(hora),
                 actualizadoEn=NOW(), actualizadoPor=VALUES(actualizadoPor)"
        );
        $guardar->execute([
            'fecha' => $fecha,
            'hora' => $hora . ':00',
            'usuario' => $idUsuario,
        ]);
    }

    /** Desactiva el reloj y hace que web y aplicación usen el tiempo real. */
    public static function desactivar(PDO $conexion, ?int $idUsuario): void
    {
        $guardar = $conexion->prepare(
            "INSERT INTO configuracionDemostracion
                    (idConfiguracion, activo, fecha, hora, actualizadoEn, actualizadoPor)
             VALUES (1, 0, NULL, NULL, NOW(), :usuario)
             ON DUPLICATE KEY UPDATE activo=0, fecha=NULL, hora=NULL,
                 actualizadoEn=NOW(), actualizadoPor=VALUES(actualizadoPor)"
        );
        $guardar->execute(['usuario' => $idUsuario]);
    }
}
