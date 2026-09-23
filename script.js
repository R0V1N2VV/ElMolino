const botonMenu = document.querySelector(".boton-menu");
const navegacionPrincipal = document.querySelector(".navegacion-principal");
const textoBotonMenu = botonMenu.querySelector(".solo-lector");

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
