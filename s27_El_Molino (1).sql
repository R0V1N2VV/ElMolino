-- phpMyAdmin SQL Dump
-- version 5.2.2
-- https://www.phpmyadmin.net/
--
-- Servidor: localhost
-- Tiempo de generación: 16-09-2026 a las 22:18:22
-- Versión del servidor: 8.0.46
-- Versión de PHP: 8.5.10

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de datos: `s27_El Molino`
--

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `acompañante`
--

CREATE TABLE `acompañante` (
  `idAcompañante` int NOT NULL,
  `idCliente` int NOT NULL,
  `dni` int NOT NULL,
  `nombre` varchar(50) NOT NULL,
  `apellido` varchar(50) NOT NULL,
  `edad` int NOT NULL,
  `parentesco` varchar(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `Actividad`
--

CREATE TABLE `Actividad` (
  `idActividad` int NOT NULL,
  `nombre` varchar(50) NOT NULL,
  `descripcion` varchar(150) NOT NULL,
  `horario` varchar(50) NOT NULL,
  `Duracion` varchar(50) NOT NULL,
  `idEspacio` int NOT NULL,
  `cupoMax` int NOT NULL,
  `precio` decimal(10,2) NOT NULL,
  `pago` varchar(50) NOT NULL,
  `idUsuario` int NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `alojamientos`
--

CREATE TABLE `alojamientos` (
  `idAlojamiento` int NOT NULL,
  `numero` varchar(10) NOT NULL,
  `tipo` varchar(20) NOT NULL,
  `capacidad` int NOT NULL,
  `precioPorNoche` decimal(10,2) NOT NULL,
  `estado` varchar(30) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `cliente`
--

CREATE TABLE `cliente` (
  `idCliente` int NOT NULL,
  `dni` int NOT NULL,
  `nombre` varchar(50) NOT NULL,
  `apellido` varchar(50) NOT NULL,
  `telefono` int NOT NULL,
  `email` varchar(100) NOT NULL,
  `direccion` varchar(150) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `EspacioRecreativo`
--

CREATE TABLE `EspacioRecreativo` (
  `idEspacio` int NOT NULL,
  `nombre` varchar(50) NOT NULL,
  `descripcion` varchar(150) NOT NULL,
  `ubicacion` varchar(100) NOT NULL,
  `horarioApertura` time NOT NULL,
  `horarioCierre` time NOT NULL,
  `requiereReserva` tinyint(1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `inscripcionActividad`
--

CREATE TABLE `inscripcionActividad` (
  `idInscripcion` int NOT NULL,
  `idCliente` int NOT NULL,
  `idActividad` int NOT NULL,
  `fechaInscripcion` date NOT NULL,
  `asistencia` tinyint(1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `pago`
--

CREATE TABLE `pago` (
  `idPago` int NOT NULL,
  `idReserva` int NOT NULL,
  `montoTotal` decimal(10,2) NOT NULL,
  `metodoPago` varchar(50) NOT NULL,
  `fechaPago` date NOT NULL,
  `comprobante` varchar(150) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `reservaAcompañante`
--

CREATE TABLE `reservaAcompañante` (
  `idReserva` int NOT NULL,
  `idAcompañante` int NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `reservas`
--

CREATE TABLE `reservas` (
  `idReserva` int NOT NULL,
  `idCliente` int NOT NULL,
  `idAlojamiento` int NOT NULL,
  `fechaIngreso` date NOT NULL,
  `fechaEgreso` date NOT NULL,
  `cantidadPersonas` int NOT NULL,
  `temporada` varchar(30) DEFAULT NULL,
  `estadoReserva` varchar(30) NOT NULL,
  `precioTotal` decimal(10,2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `usuarios`
--

CREATE TABLE `usuarios` (
  `idUsuario` int NOT NULL,
  `nombre` varchar(50) NOT NULL,
  `apellido` varchar(50) NOT NULL,
  `email` varchar(100) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `rol` varchar(75) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Índices para tablas volcadas
--

--
-- Indices de la tabla `acompañante`
--
ALTER TABLE `acompañante`
  ADD PRIMARY KEY (`idAcompañante`),
  ADD UNIQUE KEY `uk_acompañante_dni` (`dni`),
  ADD KEY `idx_acompañante_cliente` (`idCliente`);

--
-- Indices de la tabla `Actividad`
--
ALTER TABLE `Actividad`
  ADD PRIMARY KEY (`idActividad`),
  ADD KEY `idx_actividad_espacio` (`idEspacio`),
  ADD KEY `idx_actividad_usuario` (`idUsuario`);

--
-- Indices de la tabla `alojamientos`
--
ALTER TABLE `alojamientos`
  ADD PRIMARY KEY (`idAlojamiento`),
  ADD UNIQUE KEY `uk_alojamiento_numero` (`numero`);

--
-- Indices de la tabla `cliente`
--
ALTER TABLE `cliente`
  ADD PRIMARY KEY (`idCliente`),
  ADD UNIQUE KEY `uk_cliente_dni` (`dni`),
  ADD UNIQUE KEY `uk_cliente_telefono` (`telefono`),
  ADD UNIQUE KEY `uk_cliente_email` (`email`);

--
-- Indices de la tabla `EspacioRecreativo`
--
ALTER TABLE `EspacioRecreativo`
  ADD PRIMARY KEY (`idEspacio`);

--
-- Indices de la tabla `inscripcionActividad`
--
ALTER TABLE `inscripcionActividad`
  ADD PRIMARY KEY (`idInscripcion`),
  ADD UNIQUE KEY `uk_cliente_actividad` (`idCliente`,`idActividad`),
  ADD KEY `idx_inscripcion_cliente` (`idCliente`),
  ADD KEY `idx_inscripcion_actividad` (`idActividad`);

--
-- Indices de la tabla `pago`
--
ALTER TABLE `pago`
  ADD PRIMARY KEY (`idPago`),
  ADD KEY `idx_pago_reserva` (`idReserva`);

--
-- Indices de la tabla `reservaAcompañante`
--
ALTER TABLE `reservaAcompañante`
  ADD PRIMARY KEY (`idReserva`,`idAcompañante`),
  ADD KEY `idx_ra_acompañante` (`idAcompañante`);

--
-- Indices de la tabla `reservas`
--
ALTER TABLE `reservas`
  ADD PRIMARY KEY (`idReserva`),
  ADD KEY `idx_reserva_cliente` (`idCliente`),
  ADD KEY `idx_reserva_alojamiento` (`idAlojamiento`);

--
-- Indices de la tabla `usuarios`
--
ALTER TABLE `usuarios`
  ADD PRIMARY KEY (`idUsuario`),
  ADD UNIQUE KEY `uk_usuario_email` (`email`);

--
-- AUTO_INCREMENT de las tablas volcadas
--

--
-- AUTO_INCREMENT de la tabla `acompañante`
--
ALTER TABLE `acompañante`
  MODIFY `idAcompañante` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `Actividad`
--
ALTER TABLE `Actividad`
  MODIFY `idActividad` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `alojamientos`
--
ALTER TABLE `alojamientos`
  MODIFY `idAlojamiento` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `cliente`
--
ALTER TABLE `cliente`
  MODIFY `idCliente` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `EspacioRecreativo`
--
ALTER TABLE `EspacioRecreativo`
  MODIFY `idEspacio` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `inscripcionActividad`
--
ALTER TABLE `inscripcionActividad`
  MODIFY `idInscripcion` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `pago`
--
ALTER TABLE `pago`
  MODIFY `idPago` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `reservas`
--
ALTER TABLE `reservas`
  MODIFY `idReserva` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `usuarios`
--
ALTER TABLE `usuarios`
  MODIFY `idUsuario` int NOT NULL AUTO_INCREMENT;

--
-- Restricciones para tablas volcadas
--

--
-- Filtros para la tabla `acompañante`
--
ALTER TABLE `acompañante`
  ADD CONSTRAINT `fk_acompañante_cliente` FOREIGN KEY (`idCliente`) REFERENCES `cliente` (`idCliente`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `Actividad`
--
ALTER TABLE `Actividad`
  ADD CONSTRAINT `fk_actividad_espacio` FOREIGN KEY (`idEspacio`) REFERENCES `EspacioRecreativo` (`idEspacio`) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_actividad_usuario` FOREIGN KEY (`idUsuario`) REFERENCES `usuarios` (`idUsuario`) ON DELETE RESTRICT ON UPDATE CASCADE;

--
-- Filtros para la tabla `inscripcionActividad`
--
ALTER TABLE `inscripcionActividad`
  ADD CONSTRAINT `fk_inscripcion_actividad` FOREIGN KEY (`idActividad`) REFERENCES `Actividad` (`idActividad`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_inscripcion_cliente` FOREIGN KEY (`idCliente`) REFERENCES `cliente` (`idCliente`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `pago`
--
ALTER TABLE `pago`
  ADD CONSTRAINT `fk_pago_reserva` FOREIGN KEY (`idReserva`) REFERENCES `reservas` (`idReserva`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `reservaAcompañante`
--
ALTER TABLE `reservaAcompañante`
  ADD CONSTRAINT `fk_ra_acompañante` FOREIGN KEY (`idAcompañante`) REFERENCES `acompañante` (`idAcompañante`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_ra_reserva` FOREIGN KEY (`idReserva`) REFERENCES `reservas` (`idReserva`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `reservas`
--
ALTER TABLE `reservas`
  ADD CONSTRAINT `fk_reserva_alojamiento` FOREIGN KEY (`idAlojamiento`) REFERENCES `alojamientos` (`idAlojamiento`) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_reserva_cliente` FOREIGN KEY (`idCliente`) REFERENCES `cliente` (`idCliente`) ON DELETE RESTRICT ON UPDATE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
