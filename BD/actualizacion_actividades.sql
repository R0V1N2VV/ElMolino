-- Sección de actividades para la versión PHP orientada a objetos.
-- Ejecutar una sola vez después de importar BD/s27_El_Molino_completa.sql.

USE `s27_El Molino`;

CREATE TABLE CategoriaActividad (
    idCategoria INT NOT NULL AUTO_INCREMENT,
    slug VARCHAR(70) NOT NULL,
    nombre VARCHAR(60) NOT NULL,
    descripcion VARCHAR(160) NOT NULL,
    imagen VARCHAR(255) DEFAULT NULL,
    activa TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (idCategoria),
    UNIQUE KEY uk_categoria_actividad_slug (slug),
    UNIQUE KEY uk_categoria_actividad_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE Actividad
    MODIFY COLUMN descripcion TEXT NOT NULL,
    MODIFY COLUMN idEspacio INT NULL,
    MODIFY COLUMN idUsuario INT NULL,
    ADD COLUMN slug VARCHAR(100) NOT NULL AFTER idActividad,
    ADD COLUMN idCategoria INT NULL AFTER slug,
    ADD COLUMN descripcionCorta VARCHAR(160) NULL AFTER nombre,
    ADD COLUMN requisitos TEXT NULL AFTER descripcion,
    ADD COLUMN informacionImportante TEXT NULL AFTER requisitos,
    ADD COLUMN dias VARCHAR(100) NULL AFTER horario,
    ADD COLUMN momentoDia ENUM('manana', 'tarde', 'noche') NULL AFTER dias,
    ADD COLUMN sector VARCHAR(120) NULL AFTER Duracion,
    ADD COLUMN responsable VARCHAR(100) NULL AFTER sector,
    ADD COLUMN edadRecomendada ENUM('todas', 'ninos', 'adolescentes', 'adultos') NOT NULL DEFAULT 'todas' AFTER responsable,
    ADD COLUMN modalidad ENUM('incluida', 'inscripcion') NOT NULL DEFAULT 'incluida' AFTER edadRecomendada,
    ADD COLUMN imagen VARCHAR(255) NULL AFTER modalidad,
    ADD COLUMN tendencia TINYINT(1) NOT NULL DEFAULT 0 AFTER imagen,
    ADD COLUMN activa TINYINT(1) NOT NULL DEFAULT 1 AFTER tendencia,
    ADD UNIQUE KEY uk_actividad_slug (slug),
    ADD KEY idx_actividad_categoria (idCategoria),
    ADD CONSTRAINT fk_actividad_categoria
        FOREIGN KEY (idCategoria) REFERENCES CategoriaActividad(idCategoria)
        ON DELETE SET NULL ON UPDATE CASCADE;

-- La base completa ya incluye esta tabla. Se deja también aquí para que las
-- instalaciones anteriores puedan usar cupos e inscripciones.
CREATE TABLE IF NOT EXISTS inscripcionActividad (
    idInscripcion INT NOT NULL AUTO_INCREMENT,
    idCliente INT NOT NULL,
    idActividad INT NOT NULL,
    fechaInscripcion DATE NOT NULL,
    asistencia TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (idInscripcion),
    UNIQUE KEY uk_cliente_actividad (idCliente, idActividad),
    KEY idx_inscripcion_cliente (idCliente),
    KEY idx_inscripcion_actividad (idActividad),
    CONSTRAINT fk_inscripcion_cliente
        FOREIGN KEY (idCliente) REFERENCES cliente(idCliente)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_inscripcion_actividad
        FOREIGN KEY (idActividad) REFERENCES Actividad(idActividad)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO CategoriaActividad (slug, nombre, descripcion, imagen) VALUES
    ('caminatas', 'Caminatas', 'Senderos y recorridos guiados', 'imgs/caminatas.jpg'),
    ('deportes', 'Deportes', 'Fútbol, básquet, natación y más', 'imgs/futb.jpg'),
    ('bienestar', 'Bienestar', 'Yoga y propuestas para bajar el ritmo', 'imgs/bienestar.jpg'),
    ('talleres', 'Talleres', 'Experiencias creativas para compartir', 'imgs/talleres.jpg'),
    ('espectaculos', 'Espectáculos', 'Música y propuestas para toda la familia', 'imgs/espectaculos.jpg'),
    ('naturaleza', 'Naturaleza', 'Actividades conectadas con el entorno', 'imgs/naturaleza.jpg');

INSERT INTO Actividad
    (slug, idCategoria, nombre, descripcionCorta, descripcion, requisitos, informacionImportante,
     horario, dias, momentoDia, Duracion, sector, responsable, cupoMax, edadRecomendada,
     modalidad, precio, pago, imagen, tendencia, activa, idEspacio, idUsuario)
VALUES
    ('futbol-recreativo', (SELECT idCategoria FROM CategoriaActividad WHERE slug='deportes'),
     'Fútbol recreativo', 'Partidos abiertos para compartir y moverse en equipo.',
     'Una propuesta recreativa para disfrutar la cancha, conocer a otras personas y jugar sin exigencias competitivas.',
     'Ropa cómoda, calzado deportivo y botella de agua.', 'Los menores participan acompañados por una persona adulta.',
     '17:00', 'Todos los días', 'tarde', '1 hora y 30 minutos', 'Cancha norte', 'Coordinación deportiva', 20, 'todas', 'incluida', 0, 'Incluida', 'imgs/futb.jpg', 1, 1, NULL, NULL),
    ('yoga-al-aire-libre', (SELECT idCategoria FROM CategoriaActividad WHERE slug='bienestar'),
     'Yoga al aire libre', 'Movilidad, respiración y una pausa en contacto con la naturaleza.',
     'Una clase suave para relajar el cuerpo, mejorar la movilidad y disfrutar un momento tranquilo al aire libre.',
     'Ropa cómoda. Las colchonetas están incluidas.', 'La actividad pasa al salón en caso de lluvia.',
     '09:30', 'Martes, jueves y domingos', 'manana', '50 minutos', 'Jardín central', 'Equipo de bienestar', 16, 'adultos', 'incluida', 0, 'Incluida', '', 1, 1, NULL, NULL),
    ('recorrido-en-bicicleta', (SELECT idCategoria FROM CategoriaActividad WHERE slug='deportes'),
     'Recorrido en bicicleta', 'Senderos, pequeñas paradas y vistas del entorno.',
     'La salida combina caminos tranquilos, pequeñas paradas y vistas del entorno. El guía adapta el ritmo al grupo y explica cada tramo antes de comenzar.',
     'Ropa cómoda, calzado cerrado y botella de agua. Las bicicletas y los cascos están incluidos; también podés traer tu propio equipo.',
     'La actividad puede reprogramarse por condiciones climáticas. Los menores participan acompañados por una persona adulta.',
     '10:00 y 16:30', 'Miércoles y sábados', 'manana', '1 hora y 15 minutos', 'Recepción del área deportiva', 'Guías de recreación', 12, 'todas', 'inscripcion', 3500, 'Con inscripción', '', 1, 1, NULL, NULL),
    ('caminata-por-senderos', (SELECT idCategoria FROM CategoriaActividad WHERE slug='caminatas'),
     'Caminata por senderos', 'Un recorrido guiado para conocer los rincones verdes del complejo.',
     'Caminata de intensidad baja con paradas para observar el paisaje y conocer distintos espacios de El Molino.',
     'Calzado cerrado, protector solar y agua.', 'Se suspende en caso de tormenta.',
     '08:30', 'Viernes y sábados', 'manana', '1 hora', 'Punto de encuentro principal', 'Guías de recreación', 18, 'todas', 'incluida', 0, 'Incluida', '', 0, 1, NULL, NULL),
    ('padel-recreativo', (SELECT idCategoria FROM CategoriaActividad WHERE slug='deportes'),
     'Pádel recreativo', 'Cancha disponible para organizar partidos entre amigos.',
     'Una propuesta para jugar por turnos en la cancha de pádel del complejo, con opciones para quienes recién empiezan y para grupos con experiencia.',
     'Ropa cómoda y calzado deportivo. Se pueden solicitar paletas en recepción.', 'La reserva de cancha depende de la disponibilidad del día.',
     '09:00 a 20:00', 'Todos los días', 'tarde', 'Turnos de 1 hora', 'Cancha de pádel', 'Coordinación deportiva', 4, 'adolescentes', 'inscripcion', 4000, 'Con inscripción', 'imgs/padel.jpg', 0, 1, NULL, NULL),
    ('taller-de-pintura', (SELECT idCategoria FROM CategoriaActividad WHERE slug='talleres'),
     'Taller de pintura', 'Una propuesta creativa para experimentar con colores y materiales.',
     'Un encuentro para crear una obra sencilla inspirada en la naturaleza del complejo. No hace falta experiencia previa.',
     'Los materiales están incluidos.', 'Los niños menores de 8 años deben asistir con una persona adulta.',
     '15:00', 'Sábados', 'tarde', '1 hora y 30 minutos', 'Salón de talleres', 'Equipo cultural', 14, 'todas', 'inscripcion', 2800, 'Con inscripción', '', 0, 1, NULL, NULL),
    ('espectaculo-familiar', (SELECT idCategoria FROM CategoriaActividad WHERE slug='espectaculos'),
     'Espectáculo familiar', 'Música, humor y juegos para cerrar el día en familia.',
     'Una presentación pensada para compartir entre grandes y chicos, con artistas invitados y participación del público.',
     'Llegar 15 minutos antes para ubicarse.', 'La programación puede cambiar según la fecha de la estadía.',
     '20:30', 'Sábados', 'noche', '1 hora', 'Salón de eventos', 'Coordinación cultural', 80, 'todas', 'incluida', 0, 'Incluida', '', 0, 1, NULL, NULL),
    ('natacion-recreativa', (SELECT idCategoria FROM CategoriaActividad WHERE slug='deportes'),
     'Natación recreativa', 'Actividad guiada en la piscina para moverse y divertirse.',
     'Encuentro recreativo en el agua con ejercicios sencillos y juegos adaptados al grupo.',
     'Traje de baño, toalla, ojotas y gorro de natación.', 'Sujeta a condiciones climáticas y disponibilidad de la piscina.',
     '16:00', 'Martes y jueves', 'tarde', '45 minutos', 'Piscina principal', 'Equipo de guardavidas', 12, 'todas', 'inscripcion', 2000, 'Con inscripción', '', 0, 1, NULL, NULL),
    ('basquet-abierto', (SELECT idCategoria FROM CategoriaActividad WHERE slug='deportes'),
     'Básquet abierto', 'Partidos y práctica libre en equipo.',
     'La cancha queda disponible para encuentros recreativos y ejercicios básicos coordinados.',
     'Ropa cómoda, calzado deportivo y agua.', 'La actividad se organiza según la cantidad de participantes.',
     '18:00', 'Lunes, miércoles y viernes', 'tarde', '1 hora', 'Playón deportivo', 'Coordinación deportiva', 16, 'adolescentes', 'incluida', 0, 'Incluida', '', 0, 1, NULL, NULL),
    ('avistaje-de-aves', (SELECT idCategoria FROM CategoriaActividad WHERE slug='naturaleza'),
     'Avistaje de aves', 'Una salida tranquila para observar las especies del entorno.',
     'Recorrido breve con guía por sectores de vegetación y descanso, pensado para reconocer aves sin alterar su ambiente.',
     'Calzado cómodo, protector solar y, si tenés, binoculares.', 'Se recomienda mantener silencio durante algunos tramos del recorrido.',
     '07:30', 'Domingos', 'manana', '1 hora', 'Sendero del bosque', 'Guías de naturaleza', 10, 'todas', 'incluida', 0, 'Incluida', '', 0, 1, NULL, NULL);

-- Para habilitar un coordinador, asignar el rol a un usuario ya registrado:
-- UPDATE usuarios SET rol = 'coordinador_actividades' WHERE email = 'correo@ejemplo.com';
