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
