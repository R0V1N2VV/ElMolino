<?php
declare(strict_types=1);

require_once __DIR__ . '/Conexion.php';
require_once __DIR__ . '/Sesion.php';
require_once __DIR__ . '/Utilidades.php';
require_once __DIR__ . '/Autorizacion.php';
require_once __DIR__ . '/CategoriaActividad.php';
require_once __DIR__ . '/Actividad.php';
require_once __DIR__ . '/FuenteActividades.php';
require_once __DIR__ . '/RepositorioActividades.php';
require_once __DIR__ . '/CatalogoActividadesPredeterminadas.php';
require_once __DIR__ . '/GestorImagenActividad.php';
require_once __DIR__ . '/ControladorActividades.php';
require_once __DIR__ . '/RepositorioInscripciones.php';
require_once __DIR__ . '/ControladorInscripciones.php';
require_once __DIR__ . '/Vista.php';

Sesion::iniciar();

$modoCatalogoPredeterminado = false;
$repositorioInscripciones = null;
$controladorInscripciones = null;
try {
    $conexionActividades = Conexion::obtener();
    $fuenteActividades = new RepositorioActividades($conexionActividades);
    $categoriasDisponibles = $fuenteActividades->listarCategorias();
    $fuenteActividades->listarTendencias(1);
    if ($categoriasDisponibles === []) {
        throw new RuntimeException('La base todavía no tiene categorías de actividades.');
    }
    $repositorioInscripciones = new RepositorioInscripciones($conexionActividades);
    $controladorInscripciones = new ControladorInscripciones($repositorioInscripciones);
} catch (Throwable $excepcionBase) {
    $fuenteActividades = new CatalogoActividadesPredeterminadas();
    $modoCatalogoPredeterminado = true;
}

$controladorActividades = new ControladorActividades(
    $fuenteActividades,
    new GestorImagenActividad(__DIR__ . '/../imgs/actividades')
);
