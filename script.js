const menuButton = document.querySelector(".menu-button");
const mainNav = document.querySelector(".main-nav");
const menuLabel = menuButton.querySelector(".sr-only");

function closeMenu() {
    menuButton.setAttribute("aria-expanded", "false");
    menuLabel.textContent = "Abrir menú";
    mainNav.classList.remove("open");
    document.body.classList.remove("menu-open");
}

menuButton.addEventListener("click", () => {
    const isOpen = menuButton.getAttribute("aria-expanded") === "true";

    menuButton.setAttribute("aria-expanded", String(!isOpen));
    menuLabel.textContent = isOpen ? "Abrir menú" : "Cerrar menú";
    mainNav.classList.toggle("open", !isOpen);
    document.body.classList.toggle("menu-open", !isOpen);
});

mainNav.addEventListener("click", (event) => {
    if (event.target.matches("a")) {
        closeMenu();
    }
});

document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") {
        closeMenu();
    }
});

window.addEventListener("resize", () => {
    if (window.innerWidth > 760) {
        closeMenu();
    }
});
