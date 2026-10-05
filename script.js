const botonMenu = document.querySelector(".boton-menu");
const navegacionPrincipal = document.querySelector(".navegacion-principal");
const textoBotonMenu = botonMenu.querySelector(".solo-lector");
const interruptorDislexia = document.querySelector(".interruptor-dislexia");
const preferenciaDislexia = "modo-dislexia-el-molino";

function cambiarModoDislexia(estaActivo) {
    document.body.classList.toggle("modo-dislexia", estaActivo);
    interruptorDislexia?.setAttribute("aria-checked", String(estaActivo));
    interruptorDislexia?.setAttribute(
        "aria-label",
        estaActivo ? "Desactivar modo dislexia" : "Activar modo dislexia con tipografía Sarakanda"
    );
}

if (interruptorDislexia) {
    cambiarModoDislexia(localStorage.getItem(preferenciaDislexia) === "activo");

    interruptorDislexia.addEventListener("click", () => {
        const estaActivo = !document.body.classList.contains("modo-dislexia");
        cambiarModoDislexia(estaActivo);
        localStorage.setItem(preferenciaDislexia, estaActivo ? "activo" : "inactivo");
    });
}

function cerrarMenu() {
    botonMenu.setAttribute("aria-expanded", "false");
    textoBotonMenu.textContent = "Abrir menú";
    navegacionPrincipal.classList.remove("abierto");
    document.body.classList.remove("menu-abierto");
}

botonMenu.addEventListener("click", () => {
    const estaAbierto = botonMenu.getAttribute("aria-expanded") === "true";

    botonMenu.setAttribute("aria-expanded", String(!estaAbierto));
    textoBotonMenu.textContent = estaAbierto ? "Abrir menú" : "Cerrar menú";
    navegacionPrincipal.classList.toggle("abierto", !estaAbierto);
    document.body.classList.toggle("menu-abierto", !estaAbierto);
});

navegacionPrincipal.addEventListener("click", (event) => {
    if (event.target.matches("a")) {
        cerrarMenu();
    }
});

document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") {
        cerrarMenu();
    }
});

window.addEventListener("resize", () => {
    if (window.innerWidth > 760) {
        cerrarMenu();
    }
});

const formularioActividad = document.querySelector(".formulario-actividad-redisenado");

if (formularioActividad) {
    const horaInicio = formularioActividad.querySelector('[name="hora_inicio"]');
    const horaFin = formularioActividad.querySelector('[name="hora_fin"]');
    const modalidad = formularioActividad.querySelector('[name="modalidad"]');
    const campoPrecio = formularioActividad.querySelector("[data-campo-precio]");
    const precio = formularioActividad.querySelector('[name="precio"]');

    function minutosDesdeMedianoche(hora) {
        const [horas, minutos] = hora.split(":").map(Number);
        return horas * 60 + minutos;
    }

    function actualizarRangoHorario() {
        if (!horaInicio?.value || !horaFin) return;

        const inicioEnMinutos = minutosDesdeMedianoche(horaInicio.value);
        let primeraOpcionPosterior = "";
        let opcionSugerida = "";

        for (const opcion of horaFin.options) {
            const minutosOpcion = minutosDesdeMedianoche(opcion.value);
            opcion.disabled = minutosOpcion <= inicioEnMinutos;
            if (!opcion.disabled && primeraOpcionPosterior === "") primeraOpcionPosterior = opcion.value;
            if (!opcion.disabled && minutosOpcion >= inicioEnMinutos + 60 && opcionSugerida === "") opcionSugerida = opcion.value;
        }

        if (!horaFin.value || horaFin.value <= horaInicio.value) {
            horaFin.value = opcionSugerida || primeraOpcionPosterior;
        }
    }

    function actualizarPrecio() {
        if (!modalidad || !campoPrecio || !precio) return;
        const esPaga = modalidad.value === "inscripcion";
        campoPrecio.hidden = !esPaga;
        precio.disabled = !esPaga;
        precio.required = esPaga;
        if (!esPaga) precio.value = "0";
    }

    horaInicio?.addEventListener("change", actualizarRangoHorario);
    modalidad?.addEventListener("change", actualizarPrecio);
    formularioActividad.addEventListener("submit", () => {
        const botonGuardar = formularioActividad.querySelector('button[type="submit"]');
        if (!botonGuardar) return;
        botonGuardar.disabled = true;
        botonGuardar.textContent = "Guardando actividad...";
    });
    actualizarRangoHorario();
    actualizarPrecio();
}
