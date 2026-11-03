const botonMenu = document.querySelector(".boton-menu");
const navegacionPrincipal = document.querySelector(".navegacion-principal");
const textoBotonMenu = botonMenu?.querySelector(".solo-lector");
const menuUsuario = document.querySelector(".menu-usuario");
const botonUsuario = menuUsuario?.querySelector(".boton-usuario");
const panelUsuario = menuUsuario?.querySelector(".panel-usuario");
const interruptorDislexia = document.querySelector(".interruptor-dislexia");
const preferenciaDislexia = "modo-dislexia-el-molino";

/**
 * Ejecuta la lógica de “cambiar modo dislexia” en el comportamiento de la página.
 * @param {*} estaActivo Dato que utiliza esta función.
 */
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

    // Cuando ocurre el evento “click”, ejecuta la acción definida en esta función anónima.
    interruptorDislexia.addEventListener("click", () => {
        const estaActivo = !document.body.classList.contains("modo-dislexia");
        cambiarModoDislexia(estaActivo);
        localStorage.setItem(preferenciaDislexia, estaActivo ? "activo" : "inactivo");
    });
}

const botonesIdioma = [...document.querySelectorAll("[data-idioma]")];
const preferenciaIdioma = "idioma-el-molino";
const textosOriginales = new WeakMap();
const atributosOriginales = new WeakMap();
const traduccionesIngles = {
    "Inicio": "Home",
    "Quiénes somos": "About us",
    "Alojamientos": "Accommodation",
    "Actividades": "Activities",
    "Espacios": "Facilities",
    "Mi estadía": "My stay",
    "Mi cuenta": "My account",
    "Ingresar": "Sign in",
    "Iniciar sesión": "Sign in",
    "Ver mi cuenta": "View my account",
    "Gestionar actividades": "Manage activities",
    "Opciones de accesibilidad": "Accessibility options",
    "Modo dislexia": "Dyslexia mode",
    "Idioma": "Language",
    "Ver sitio": "View site",
    "Ver actividades": "View activities",
    "Gestionar": "Manage",
    "Bienvenido a El Molino": "Welcome to El Molino",
    "Descanso, deporte y tiempo para compartir.": "Rest, sports and time together.",
    "El Molino es un complejo recreativo con alojamientos, canchas y espacios para pasar el día o quedarse unos días.": "El Molino is a recreational complex with accommodation, sports courts and spaces for day visits or longer stays.",
    "Conocer el lugar": "Discover the place",
    "Ver alojamientos": "View accommodation",
    "¿Qué es El Molino?": "What is El Molino?",
    "Es un complejo recreativo para descansar, hacer deporte y compartir.": "It is a recreational complex for resting, playing sports and spending time together.",
    "Descanso": "Rest",
    "Movimiento": "Movement",
    "Encuentro": "Togetherness",
    "Nuestro propósito": "Our purpose",
    "Elegí la estadía que va con vos.": "Choose the stay that suits you.",
    "Habitaciones": "Rooms",
    "Cabañas": "Cabins",
    "Hoteles": "Hotels",
    "Ver opciones": "View options",
    "Cada día puede tener un plan diferente.": "Every day can bring a different plan.",
    "Deportes": "Sports",
    "Bienestar": "Wellness",
    "Talleres": "Workshops",
    "Ver actividades": "View activities",
    "Explorar todas las actividades": "Explore all activities",
    "Todo lo necesario, dentro del complejo.": "Everything you need, inside the complex.",
    "Piscina": "Swimming pool",
    "Restaurante": "Restaurant",
    "Cafetería": "Coffee shop",
    "Salón de eventos": "Events hall",
    "Gimnasio": "Gym",
    "Área infantil": "Children's area",
    "Tu visita": "Your visit",
    "Información para organizar tu estadía.": "Information to plan your stay.",
    "Elegir alojamiento": "Choose accommodation",
    "Elegí cómo querés disfrutar tu día.": "Choose how you want to enjoy your day.",
    "Deporte, naturaleza, talleres y propuestas para compartir durante tu estadía.": "Sports, nature, workshops and activities to enjoy during your stay.",
    "Ver todas las categorías": "View all categories",
    "Tendencias": "Popular now",
    "Los planes más elegidos.": "The most popular activities.",
    "Elegir categoría": "Choose a category",
    "Todas las categorías": "All categories",
    "Encontrá una actividad para vos.": "Find an activity for you.",
    "Volver a actividades": "Back to activities",
    "Información práctica": "Practical information",
    "Días y horarios": "Days and times",
    "Sector": "Location",
    "Responsable": "Coordinator",
    "Cupos": "Availability",
    "Valor": "Price",
    "Actividad completa": "Activity full",
    "No quedan lugares disponibles.": "There are no places available.",
    "¿Quiénes van a participar?": "Who will participate?",
    "Titular de la cuenta": "Account holder",
    "Confirmar inscripción": "Confirm registration",
    "Guardar participantes": "Save participants",
    "Cancelar inscripción": "Cancel registration",
    "Iniciar sesión para inscribirme": "Sign in to register",
    "Necesitás una cuenta para reservar tu lugar.": "You need an account to reserve your place.",
    "ACTIVIDAD PAGA": "PAID ACTIVITY",
    "Antes de inscribirte": "Before registering",
    "Volver": "Back",
    "ALOJAMIENTOS": "ACCOMMODATION",
    "Elegí cómo querés alojarte": "Choose how you want to stay",
    "Primero elegí el tipo de alojamiento. Después vas a poder comparar capacidades, servicios y fechas.": "First choose an accommodation type. Then compare capacities, amenities and dates.",
    "¿Qué estás buscando?": "What are you looking for?",
    "Elegí una opción para ver los alojamientos": "Choose an option to view accommodation",
    "Espacios independientes para 2, 4 o 6 personas.": "Independent spaces for 2, 4 or 6 guests.",
    "Opciones dobles, familiares y grupales dentro del complejo.": "Double, family and group options inside the complex.",
    "Habitaciones con servicios diarios y espacios comunes.": "Rooms with daily services and shared spaces.",
    "Todos los tipos": "All types",
    "TIPO DE ALOJAMIENTO": "ACCOMMODATION TYPE",
    "Por ahora no hay opciones para mostrar": "There are no options to show right now",
    "Podés volver y elegir otro tipo de alojamiento.": "You can go back and choose another accommodation type.",
    "Elegir otro tipo": "Choose another type",
    "Incluye": "Included",
    "Reservar": "Book",
    "Wi-Fi": "Wi-Fi",
    "Baño privado": "Private bathroom",
    "Cocina equipada": "Equipped kitchen",
    "Parrilla": "Barbecue",
    "Ropa de cama": "Bed linen",
    "Estacionamiento": "Parking",
    "Climatización": "Climate control",
    "Guardado personal": "Personal storage",
    "Desayuno": "Breakfast",
    "Limpieza diaria": "Daily housekeeping",
    "Espacios comunes": "Shared spaces",
    "Elegí dónde quedarte": "Choose where to stay",
    "Opciones disponibles": "Available options",
    "Opciones de estadía": "Stay options",
    "Disponible": "Available",
    "No disponible": "Unavailable",
    "Reservada": "Reserved",
    "Reservada para esas fechas": "Reserved for those dates",
    "En limpieza": "Being cleaned",
    "Fuera de servicio": "Out of service",
    "Este alojamiento no está habilitado temporalmente.": "This accommodation is temporarily unavailable.",
    "Volverá a estar disponible cuando finalice la limpieza.": "It will be available again after cleaning is complete.",
    "Este alojamiento ya se encuentra reservado.": "This accommodation is already reserved.",
    "Consultá con el complejo para conocer su disponibilidad.": "Contact the complex to check availability.",
    "Podés probar con otras fechas.": "You can try different dates.",
    "Probar otras fechas": "Try different dates",
    "Ingreso": "Check-in",
    "Egreso": "Check-out",
    "Personas": "Guests",
    "Agregar a mi estadía": "Add to my stay",
    "MI ESTADÍA": "MY STAY",
    "Tu estadía": "Your stay",
    "Empezá eligiendo un alojamiento": "Start by choosing accommodation",
    "Seguir eligiendo": "Keep choosing",
    "Continuar con la compra": "Continue",
    "Total estimado": "Estimated total",
    "Quitar": "Remove",
    "Volver al inicio": "Back to home",
    "Bienvenido": "Welcome",
    "Ingresá con tu cuenta para continuar.": "Sign in with your account to continue.",
    "Correo electrónico": "Email address",
    "Contraseña": "Password",
    "Registrate": "Create an account",
    "MI CUENTA": "MY ACCOUNT",
    "Correo": "Email",
    "Cerrar sesión": "Sign out",
    "Agregar categoría": "Add category",
    "Agregar actividad": "Add activity",
    "Editar actividad": "Edit activity",
    "Guardar actividad": "Save activity",
    "Cancelar": "Cancel",
    "Eliminar": "Delete"
};

const traduccionesAtributos = {
    "El Molino, inicio": "El Molino, home",
    "Navegación principal": "Main navigation",
    "Navegación de administración": "Management navigation",
    "Abrir menú": "Open menu",
    "Activar modo dislexia con tipografía Sarakanda": "Enable dyslexia mode with Sarakanda typeface",
    "Desactivar modo dislexia": "Disable dyslexia mode",
    "Seleccionar idioma": "Select language",
    "Bajar a la sección siguiente": "Go to the next section"
};

/**
 * Ejecuta la lógica de “traducir texto” en el comportamiento de la página.
 * @param {*} nodo Dato que utiliza esta función.
 * @param {*} idioma Dato que utiliza esta función.
 */
function traducirTexto(nodo, idioma) {
    if (!textosOriginales.has(nodo)) textosOriginales.set(nodo, nodo.nodeValue);
    const original = textosOriginales.get(nodo);
    if (idioma === "es") {
        nodo.nodeValue = original;
        return;
    }
    const limpio = original.trim();
    if (!traduccionesIngles[limpio]) return;
    nodo.nodeValue = original.replace(limpio, traduccionesIngles[limpio]);
}

/**
 * Ejecuta la lógica de “aplicar idioma” en el comportamiento de la página.
 * @param {*} idioma Dato que utiliza esta función.
 */
function aplicarIdioma(idioma) {
    const idiomaSeguro = idioma === "en" ? "en" : "es";
    const recorrido = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
    let nodo = recorrido.nextNode();
    while (nodo) {
        const padre = nodo.parentElement;
        if (padre && !["SCRIPT", "STYLE"].includes(padre.tagName)) traducirTexto(nodo, idiomaSeguro);
        nodo = recorrido.nextNode();
    }

    // Recorre cada elemento de la colección y aplica esta función anónima.
    document.querySelectorAll("[aria-label], [placeholder]").forEach((elemento) => {
        if (!atributosOriginales.has(elemento)) {
            atributosOriginales.set(elemento, {
                ariaLabel: elemento.getAttribute("aria-label"),
                placeholder: elemento.getAttribute("placeholder")
            });
        }
        const originales = atributosOriginales.get(elemento);
        for (const atributo of ["aria-label", "placeholder"]) {
            const clave = atributo === "aria-label" ? "ariaLabel" : "placeholder";
            const original = originales[clave];
            if (original === null) continue;
            elemento.setAttribute(atributo, idiomaSeguro === "en" ? (traduccionesAtributos[original] || original) : original);
        }
    });

    document.documentElement.lang = idiomaSeguro === "en" ? "en" : "es-AR";
    // Recorre cada elemento de la colección y aplica esta función anónima.
    botonesIdioma.forEach((boton) => boton.setAttribute("aria-pressed", String(boton.dataset.idioma === idiomaSeguro)));
}

if (botonesIdioma.length) {
    const idiomaGuardado = localStorage.getItem(preferenciaIdioma) === "en" ? "en" : "es";
    aplicarIdioma(idiomaGuardado);
    // Cuando ocurre el evento “click”, ejecuta la acción definida en esta función anónima.
    botonesIdioma.forEach((boton) => boton.addEventListener("click", () => {
        const idioma = boton.dataset.idioma === "en" ? "en" : "es";
        localStorage.setItem(preferenciaIdioma, idioma);
        aplicarIdioma(idioma);
    }));
}

const elementosAnimados = [...document.querySelectorAll(
    ".tarjeta-valor, .tarjeta-alojamiento, .tarjeta-tipo-alojamiento, .producto-alojamiento, .grilla-espacios article"
)];

if (elementosAnimados.length) {
    document.documentElement.classList.add("animaciones-activas");
    // Recorre cada elemento de la colección y aplica esta función anónima.
    elementosAnimados.forEach((elemento, indice) => {
        elemento.classList.add("animacion-entrada");
        elemento.style.setProperty("--demora-animacion", `${(indice % 3) * 70}ms`);
    });

    if ("IntersectionObserver" in window) {
        // Procesa los cambios de visibilidad para iniciar las animaciones al entrar en pantalla.
        const observadorEntrada = new IntersectionObserver((entradas, observador) => {
            // Recorre cada elemento de la colección y aplica esta función anónima.
            entradas.forEach((entrada) => {
                if (!entrada.isIntersecting) return;
                entrada.target.classList.add("animacion-visible");
                observador.unobserve(entrada.target);
            });
        }, { threshold: 0.14, rootMargin: "0px 0px -28px" });
        // Recorre cada elemento de la colección y aplica esta función anónima.
        elementosAnimados.forEach((elemento) => observadorEntrada.observe(elemento));
    } else {
        // Recorre cada elemento de la colección y aplica esta función anónima.
        elementosAnimados.forEach((elemento) => elemento.classList.add("animacion-visible"));
    }
}

// Recorre cada elemento de la colección y aplica esta función anónima.
document.querySelectorAll(".formulario-alojamiento").forEach((formulario) => {
    const ingreso = formulario.querySelector('[name="fechaIngreso"]');
    const egreso = formulario.querySelector('[name="fechaEgreso"]');

    /**
     * Ejecuta la lógica de “dia siguiente” en el comportamiento de la página.
     * @param {*} fecha Dato que utiliza esta función.
     */
    function diaSiguiente(fecha) {
        const partes = fecha.split("-").map(Number);
        // Evalúa cada elemento y conserva o localiza solamente el que cumple esta condición.
        if (partes.length !== 3 || partes.some((parte) => !Number.isFinite(parte))) return "";
        const valor = new Date(Date.UTC(partes[0], partes[1] - 1, partes[2] + 1));
        return valor.toISOString().slice(0, 10);
    }

    /**
     * Ejecuta la lógica de “ajustar egreso” en el comportamiento de la página.
     */
    function ajustarEgreso() {
        if (!ingreso?.value || !egreso) return;
        const minimo = diaSiguiente(ingreso.value);
        if (!minimo) return;
        egreso.min = minimo;
        if (!egreso.value || egreso.value < minimo) egreso.value = minimo;
    }

    ingreso?.addEventListener("change", ajustarEgreso);
    ajustarEgreso();
});

/**
 * Cierra menu y deja el estado consistente.
 */
function cerrarMenu() {
    botonMenu?.setAttribute("aria-expanded", "false");
    if (textoBotonMenu) textoBotonMenu.textContent = "Abrir menú";
    navegacionPrincipal?.classList.remove("abierto");
    document.body.classList.remove("menu-abierto");
}

/**
 * Cierra menu usuario y deja el estado consistente.
 */
function cerrarMenuUsuario() {
    botonUsuario?.setAttribute("aria-expanded", "false");
    if (panelUsuario) panelUsuario.hidden = true;
    menuUsuario?.classList.remove("abierto");
}

// Cuando ocurre el evento “click”, ejecuta la acción definida en esta función anónima.
botonMenu?.addEventListener("click", () => {
    const estaAbierto = botonMenu.getAttribute("aria-expanded") === "true";

    botonMenu.setAttribute("aria-expanded", String(!estaAbierto));
    if (textoBotonMenu) textoBotonMenu.textContent = estaAbierto ? "Abrir menú" : "Cerrar menú";
    navegacionPrincipal?.classList.toggle("abierto", !estaAbierto);
    document.body.classList.toggle("menu-abierto", !estaAbierto);
    if (!estaAbierto) cerrarMenuUsuario();
});

// Cuando ocurre el evento “click”, ejecuta la acción definida en esta función anónima.
navegacionPrincipal?.addEventListener("click", (event) => {
    if (event.target.matches("a")) {
        cerrarMenu();
    }
});

// Cuando ocurre el evento “click”, ejecuta la acción definida en esta función anónima.
botonUsuario?.addEventListener("click", () => {
    const estaAbierto = botonUsuario.getAttribute("aria-expanded") === "true";
    botonUsuario.setAttribute("aria-expanded", String(!estaAbierto));
    if (panelUsuario) panelUsuario.hidden = estaAbierto;
    menuUsuario?.classList.toggle("abierto", !estaAbierto);
    if (!estaAbierto) cerrarMenu();
});

// Cuando ocurre el evento “click”, ejecuta la acción definida en esta función anónima.
document.addEventListener("click", (event) => {
    if (menuUsuario && !menuUsuario.contains(event.target)) cerrarMenuUsuario();
});

// Cuando ocurre el evento “keydown”, ejecuta la acción definida en esta función anónima.
document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") {
        cerrarMenu();
        cerrarMenuUsuario();
    }
});

// Cuando ocurre el evento “resize”, ejecuta la acción definida en esta función anónima.
window.addEventListener("resize", () => {
    if (window.innerWidth > 760) {
        cerrarMenu();
    }
});

const botonBajarSeccion = document.querySelector("[data-bajar-seccion]");

if (botonBajarSeccion) {
    const contenidoPrincipal = document.querySelector("main");
    const secciones = contenidoPrincipal
        // Evalúa cada elemento y conserva o localiza solamente el que cumple esta condición.
        ? [...contenidoPrincipal.children].filter((elemento) => elemento.tagName === "SECTION")
        : [];

    /**
     * Ejecuta la lógica de “siguiente seccion” en el comportamiento de la página.
     */
    function siguienteSeccion() {
        // Evalúa cada elemento y conserva o localiza solamente el que cumple esta condición.
        return secciones.find((seccion) => seccion.getBoundingClientRect().top > 120);
    }

    /**
     * Actualiza boton bajar para reflejar el estado más reciente.
     */
    function actualizarBotonBajar() {
        botonBajarSeccion.classList.toggle("oculto", !siguienteSeccion());
    }

    // Cuando ocurre el evento “click”, ejecuta la acción definida en esta función anónima.
    botonBajarSeccion.addEventListener("click", () => {
        siguienteSeccion()?.scrollIntoView({ behavior: "smooth", block: "start" });
    });

    window.addEventListener("scroll", actualizarBotonBajar, { passive: true });
    window.addEventListener("resize", actualizarBotonBajar);
    actualizarBotonBajar();
}

const formularioConCobro = document.querySelector("[data-confirmar-cobro]");
const dialogoCobro = document.querySelector("#aviso-cobro-actividad");
let cobroConfirmado = false;

// Cuando ocurre el evento “submit”, ejecuta la acción definida en esta función anónima.
formularioConCobro?.addEventListener("submit", (event) => {
    if (cobroConfirmado || !dialogoCobro) return;
    event.preventDefault();
    dialogoCobro.showModal();
});

// Cuando ocurre el evento “close”, ejecuta la acción definida en esta función anónima.
dialogoCobro?.addEventListener("close", () => {
    if (dialogoCobro.returnValue !== "confirmar" || !formularioConCobro) return;
    cobroConfirmado = true;
    formularioConCobro.requestSubmit();
});

const formularioActividad = document.querySelector(".formulario-actividad-redisenado");

if (formularioActividad) {
    const horaInicio = formularioActividad.querySelector('[name="hora_inicio"]');
    const horaFin = formularioActividad.querySelector('[name="hora_fin"]');
    const modalidad = formularioActividad.querySelector('[name="modalidad"]');
    const campoPrecio = formularioActividad.querySelector("[data-campo-precio]");
    const precio = formularioActividad.querySelector('[name="precio"]');

    /**
     * Ejecuta la lógica de “minutos desde medianoche” en el comportamiento de la página.
     * @param {*} hora Dato que utiliza esta función.
     */
    function minutosDesdeMedianoche(hora) {
        const [horas, minutos] = hora.split(":").map(Number);
        return horas * 60 + minutos;
    }

    /**
     * Actualiza rango horario para reflejar el estado más reciente.
     */
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

    /**
     * Actualiza precio para reflejar el estado más reciente.
     */
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
    // Cuando ocurre el evento “submit”, ejecuta la acción definida en esta función anónima.
    formularioActividad.addEventListener("submit", () => {
        const botonGuardar = formularioActividad.querySelector('button[type="submit"]');
        if (!botonGuardar) return;
        botonGuardar.disabled = true;
        botonGuardar.textContent = "Guardando actividad...";
    });
    actualizarRangoHorario();
    actualizarPrecio();
}

const selectorAcompanantesRegistro = document.querySelector("#incluye-acompanantes");
const panelAcompanantesRegistro = document.querySelector("[data-panel-acompanantes]");
const listaAcompanantesRegistro = document.querySelector("[data-lista-acompanantes]");
const botonAgregarAcompanante = document.querySelector("[data-agregar-acompanante]");
const plantillaAcompanante = document.querySelector("#plantilla-acompanante-registro");

if (selectorAcompanantesRegistro && panelAcompanantesRegistro && listaAcompanantesRegistro && plantillaAcompanante) {
    /**
     * Ejecuta la lógica de “filas acompanantes” en el comportamiento de la página.
     */
    function filasAcompanantes() {
        return [...listaAcompanantesRegistro.querySelectorAll("[data-fila-acompanante]")];
    }

    /**
     * Ejecuta la lógica de “reindexar acompanantes” en el comportamiento de la página.
     */
    function reindexarAcompanantes() {
        // Recorre cada elemento de la colección y aplica esta función anónima.
        filasAcompanantes().forEach((fila, indice) => {
            const numero = fila.querySelector(".cabecera-fila-acompanante h3 span");
            if (numero) numero.textContent = String(indice + 1);
            // Recorre cada elemento de la colección y aplica esta función anónima.
            fila.querySelectorAll("[data-campo]").forEach((campo) => {
                campo.name = `acompanantes[${indice}][${campo.dataset.campo}]`;
            });
        });
        if (botonAgregarAcompanante) botonAgregarAcompanante.disabled = filasAcompanantes().length >= 8;
    }

    /**
     * Ejecuta la lógica de “preparar boton quitar” en el comportamiento de la página.
     * @param {*} fila Dato que utiliza esta función.
     */
    function prepararBotonQuitar(fila) {
        // Cuando ocurre el evento “click”, ejecuta la acción definida en esta función anónima.
        fila.querySelector("[data-quitar-acompanante]")?.addEventListener("click", () => {
            fila.remove();
            reindexarAcompanantes();
            if (!filasAcompanantes().length) selectorAcompanantesRegistro.checked = false;
            actualizarPanelAcompanantes();
        });
    }

    /**
     * Agrega fila acompanante a la colección o proceso actual.
     */
    function agregarFilaAcompanante() {
        if (filasAcompanantes().length >= 8) return;
        const fragmento = plantillaAcompanante.content.cloneNode(true);
        const fila = fragmento.querySelector("[data-fila-acompanante]");
        prepararBotonQuitar(fila);
        listaAcompanantesRegistro.append(fragmento);
        reindexarAcompanantes();
        fila.querySelector("input")?.focus();
    }

    /**
     * Actualiza panel acompanantes para reflejar el estado más reciente.
     */
    function actualizarPanelAcompanantes() {
        const activo = selectorAcompanantesRegistro.checked;
        panelAcompanantesRegistro.hidden = !activo;
        // Recorre cada elemento de la colección y aplica esta función anónima.
        panelAcompanantesRegistro.querySelectorAll("input").forEach((campo) => campo.disabled = !activo);
        if (activo && !filasAcompanantes().length) agregarFilaAcompanante();
    }

    filasAcompanantes().forEach(prepararBotonQuitar);
    botonAgregarAcompanante?.addEventListener("click", agregarFilaAcompanante);
    selectorAcompanantesRegistro.addEventListener("change", actualizarPanelAcompanantes);
    reindexarAcompanantes();
    actualizarPanelAcompanantes();
}
