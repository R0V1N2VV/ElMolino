-- Inscripción de acompañantes a actividades.
-- Ejecutar una vez sobre una instalación existente. Se puede volver a ejecutar.

USE `s27_El Molino`;

-- Indica si el titular ocupa un cupo. Los datos existentes conservan el
-- comportamiento anterior (el titular participa).
SET @participa_titular_existe := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inscripcionActividad'
      AND COLUMN_NAME = 'participaTitular'
);
SET @agregar_participa_titular := IF(
    @participa_titular_existe = 0,
    'ALTER TABLE inscripcionActividad ADD COLUMN participaTitular TINYINT(1) NOT NULL DEFAULT 1 AFTER idActividad',
    'SELECT 1'
);
PREPARE sentencia_participa_titular FROM @agregar_participa_titular;
EXECUTE sentencia_participa_titular;
DEALLOCATE PREPARE sentencia_participa_titular;

-- Cada acompañante seleccionado ocupa un lugar adicional en la actividad.
CREATE TABLE IF NOT EXISTS inscripcionActividadAcompanante (
    idInscripcionAcompanante INT NOT NULL AUTO_INCREMENT,
    idClienteTitular INT NOT NULL,
    `idAcompañante` INT NOT NULL,
    idActividad INT NOT NULL,
    fechaInscripcion DATE NOT NULL,
    asistencia TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (idInscripcionAcompanante),
    UNIQUE KEY uk_acompanante_actividad (`idAcompañante`, idActividad),
    KEY idx_inscripcion_acomp_titular (idClienteTitular),
    KEY idx_inscripcion_acomp_actividad (idActividad),
    KEY idx_inscripcion_acomp_titular_actividad (idClienteTitular, idActividad),
    CONSTRAINT fk_inscripcion_acomp_persona
        FOREIGN KEY (`idAcompañante`) REFERENCES `acompañante`(`idAcompañante`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_inscripcion_acomp_inscripcion
        FOREIGN KEY (idClienteTitular, idActividad)
        REFERENCES inscripcionActividad(idCliente, idActividad)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

