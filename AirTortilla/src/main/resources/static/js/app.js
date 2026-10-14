/* =========================================================
   AirTortilla · lógica de la página
   Pide los datos a la API REST (/api/...) y los pinta.
   ========================================================= */

// Nombres de los países que aparecen en los datos (código ISO → nombre)
const PAISES = {
  DE: "Alemania", ES: "España", FR: "Francia", GB: "Reino Unido", IT: "Italia",
  MX: "México", US: "Estados Unidos", PT: "Portugal", NL: "Países Bajos", BE: "Bélgica",
  CH: "Suiza", AR: "Argentina", BR: "Brasil", CA: "Canadá", JP: "Japón", CN: "China"
};
const ALGORITMO = {
  ID: "Id", FECHA: "Fecha", PASAJERO: "Pasajero", PAIS_DESTINO: "País destino"
};

const $ = (sel) => document.querySelector(sel);
const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

// Estado de la página
let reservas = [];          // última lista recibida del servidor
let grupos = [];            // localizadores
let vista = null;           // null = todas; si no, lista filtrada (búsqueda o localizador)
let visor = { nombre: null, modo: "total", linea: 0, fin: false };

/* ---------------------------------------------------------
   Llamadas a la API
   --------------------------------------------------------- */

/** Llama a la API y devuelve el JSON. Si hay error, lanza el mensaje del servidor. */
async function api(url, opciones = {}) {
  const resp = await fetch(url, opciones);
  const datos = await resp.json().catch(() => ({}));
  if (!resp.ok) throw new Error(datos.error || `Error ${resp.status}`);
  return datos;
}

/* ---------------------------------------------------------
   Efecto split-flap (letras que giran como en los aeropuertos)
   --------------------------------------------------------- */
const CARACTERES = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

/** Escribe un texto en el elemento con la animación de paletas. */
function flap(el, texto) {
  texto = String(texto);
  const anterior = el.dataset.valor || "";
  if (anterior === texto && el.children.length) return;
  el.dataset.valor = texto;
  el.innerHTML = "";
  [...texto].forEach((ch, i) => {
    const span = document.createElement("span");
    span.className = "flap-ch";
    span.textContent = ch;
    el.appendChild(span);
    if (reduceMotion || ch === " ") return;
    let vueltas = 4 + i * 2 + Math.floor(Math.random() * 3);
    const girar = () => {
      span.classList.remove("spin");
      void span.offsetWidth; // reinicia la animación
      span.classList.add("spin");
      if (vueltas-- > 0) {
        span.textContent = CARACTERES[Math.floor(Math.random() * CARACTERES.length)];
        setTimeout(girar, 55);
      } else {
        span.textContent = ch;
      }
    };
    girar();
  });
}

/** Número con ceros a la izquierda: 7 → "07". */
const relleno = (n, cifras) => String(n).padStart(cifras, "0");

/* ---------------------------------------------------------
   Torre de control (feedback)
   --------------------------------------------------------- */
function registrar(mensaje, tipo = "ok") {
  const hora = new Date().toLocaleTimeString("es-ES");
  const li = document.createElement("li");
  li.className = tipo;
  li.innerHTML = `<time>${hora}</time>`;
  li.append(mensaje);
  $("#tower-log").prepend(li);
  $("#tower-last").textContent = mensaje;
  $("#tower").classList.toggle("error", tipo === "err");
}

function error(e) {
  registrar(e.message || String(e), "err");
}

/* ---------------------------------------------------------
   Estado y carga
   --------------------------------------------------------- */
function pintarEstado(estado) {
  flap($("#n-reservas"), relleno(estado.reservas, 3));
  flap($("#n-localizadores"), relleno(estado.localizadores, 2));
  flap($("#n-ficheros"), relleno(estado.ficheros, 2));
  flap($("#n-errores"), relleno(estado.errores.length, 2));
  $("#btn-generar").disabled = estado.reservas === 0;
  const chip = $("#chip-fichero");
  chip.hidden = !estado.fichero;
  chip.textContent = estado.fichero;
}

async function trasCargar(estado) {
  pintarEstado(estado);
  registrar(`CSV «${estado.fichero}» cargado: ${estado.reservas} reservas, ` +
    `${estado.localizadores} grupos con localizador, ${estado.errores.length} líneas descartadas.`);
  estado.errores.forEach((e) => registrar(e, "err"));
  vista = null;
  await Promise.all([cargarReservas(), cargarGrupos()]);
}

async function cargarPorDefecto() {
  try {
    await trasCargar(await api("/api/cargar/defecto", { method: "POST" }));
  } catch (e) { error(e); }
}

async function subirFichero(fichero) {
  if (!fichero) return;
  const datos = new FormData();
  datos.append("fichero", fichero);
  try {
    registrar(`Subiendo «${fichero.name}»…`);
    await trasCargar(await api("/api/cargar", { method: "POST", body: datos }));
  } catch (e) { error(e); }
}

/* ---------------------------------------------------------
   Pasajeros
   --------------------------------------------------------- */
async function cargarReservas() {
  const orden = $("#sel-orden").value;
  reservas = await api(`/api/reservas?orden=${orden}`);
  $("#algo-note").innerHTML = `Ordenado por <b>${ALGORITMO[orden]}</b> con <b>MergeSort</b> · O(n log n)`;
  pintarPasajeros();
  dibujarRutas();
}

/** Agrupa las reservas por trayecto y las dibuja en el globo 3D. */
function dibujarRutas() {
  const rutas = new Map();
  for (const r of reservas) {
    const clave = `${r.origen}|${r.destino}`;
    const ruta = rutas.get(clave) || { origen: r.origen, paisOrigen: r.paisOrigen, destino: r.destino, paisDestino: r.paisDestino, n: 0 };
    ruta.n++;
    rutas.set(clave, ruta);
  }
  if (rutas.size) window.Globo.setRutas([...rutas.values()].sort((a, b) => b.n - a.n));
}

/** Animación del avión 3D cruzando la pantalla (js/despegue.js). */
function despegar() {
  if (window.Despegue) window.Despegue.lanzar();
}

const flecha = `<svg viewBox="0 0 24 24"><path d="M5 12h14m0 0-5-5m5 5-5 5"/></svg>`;

function pintarPasajeros() {
  const filtro = $("#input-filtro").value.trim().toLowerCase();
  let lista = vista || reservas;
  if (filtro) {
    lista = lista.filter((r) =>
      [r.nombrePasajero, r.origen, r.destino, r.paisOrigen, r.paisDestino, PAISES[r.paisDestino] || ""]
        .some((v) => v.toLowerCase().includes(filtro)));
  }
  const tbody = $("#tbody-pax");
  tbody.innerHTML = "";
  for (const r of lista) {
    const tr = document.createElement("tr");
    if (vista) tr.className = "destacada";
    tr.innerHTML = `
      <td class="t-id">${r.id}</td>
      <td class="t-name"></td>
      <td><span class="route"><span class="o"></span>${flecha}<span class="d"></span></span></td>
      <td class="t-fecha">${r.fechaReserva}</td>
      <td class="t-vuelo">${r.numeroVuelo}</td>
      <td>${r.localizador ? `<button class="loc-chip" data-loc="${r.localizador}">${r.localizador}</button>` : `<span class="dash">—</span>`}</td>`;
    // Los textos del CSV se insertan como texto, no como HTML
    tr.querySelector(".t-name").textContent = r.nombrePasajero;
    tr.querySelector(".o").innerHTML = `<span></span><span class="iso">${r.paisOrigen}</span>`;
    tr.querySelector(".o span").textContent = r.origen;
    tr.querySelector(".d").innerHTML = `<span></span><span class="iso">${r.paisDestino}</span>`;
    tr.querySelector(".d span").textContent = r.destino;
    tbody.appendChild(tr);
  }
  $("#pax-empty").hidden = lista.length > 0;
  $("#btn-todas").hidden = !vista;
}

async function buscarPorId(evento) {
  evento.preventDefault();
  const id = $("#input-id").value.trim();
  if (!id) return;
  try {
    const r = await api(`/api/reservas/${encodeURIComponent(id)}`);
    vista = [r];
    pintarPasajeros();
    registrar(`Reserva ${r.id} encontrada con búsqueda binaria · O(log n).`);
  } catch (e) { error(e); }
}

function mostrarLocalizador(loc) {
  const grupo = grupos.find((g) => g.localizador === loc);
  if (!grupo) return;
  vista = grupo.reservas;
  $("#input-filtro").value = "";
  abrirPestana("pasajeros");
  pintarPasajeros();
  registrar(`Localizador ${loc}: ${grupo.reservas.length} reservas de ${grupo.pasajero}.`);
}

function mostrarTodas() {
  vista = null;
  $("#input-id").value = "";
  pintarPasajeros();
}

/* ---------------------------------------------------------
   Localizadores (tarjetas de embarque)
   --------------------------------------------------------- */
async function cargarGrupos() {
  grupos = await api("/api/localizadores");
  const cont = $("#passes");
  cont.innerHTML = "";
  for (const g of grupos) {
    const r0 = g.reservas[0];
    const art = document.createElement("article");
    art.className = "pass";
    art.innerHTML = `
      <div class="pass-main">
        <div class="pass-top"><span>AirTortilla · Grupo</span><b>${g.localizador}</b></div>
        <div class="pass-route">
          <div class="city"><small>${r0.paisOrigen}</small><span class="c-o"></span></div>
          <svg viewBox="0 0 24 24"><path d="M2 16l20-6-5 9-4-3-3 4-1-6z"/></svg>
          <div class="city"><small>${r0.paisDestino}</small><span class="c-d"></span></div>
        </div>
        <div class="pass-meta">
          <div><span>Pasajero</span><b class="p-n"></b></div>
          <div><span>Fecha</span><b>${r0.fechaReserva}</b></div>
          <div><span>Vuelo</span><b>${r0.numeroVuelo}</b></div>
        </div>
        <div class="pass-ids">${g.reservas.map((r) => `<span>${r.id}</span>`).join("")}</div>
      </div>
      <div class="pass-stub">
        <span class="pax-n">${g.reservas.length}</span>
        <span class="pax-l">Pasajeros</span>
        <span class="loc">${g.localizador}</span>
        <span class="barcode" aria-hidden="true"></span>
      </div>
      <button class="ver" aria-label="Ver las reservas de ${g.localizador}"></button>`;
    art.querySelector(".c-o").textContent = g.origen;
    art.querySelector(".c-d").textContent = g.destino;
    art.querySelector(".p-n").textContent = g.pasajero;
    art.querySelector(".ver").addEventListener("click", () => mostrarLocalizador(g.localizador));
    cont.appendChild(art);
  }
  $("#grupos-empty").hidden = grupos.length > 0;
}

/* ---------------------------------------------------------
   Panel de salidas (ficheros por país)
   --------------------------------------------------------- */
async function generarFicheros() {
  try {
    const ficheros = await api("/api/ficheros/generar", { method: "POST" });
    despegar();
    pintarSalidas(ficheros);
    flap($("#n-ficheros"), relleno(ficheros.length, 2));
    registrar(`${ficheros.length} ficheros creados en la carpeta output/.`);
    abrirPestana("salidas");
  } catch (e) { error(e); }
}

async function cargarSalidas() {
  try {
    pintarSalidas(await api("/api/ficheros"));
  } catch (e) { error(e); }
}

function pintarSalidas(ficheros) {
  const cuerpo = $("#board-body");
  cuerpo.querySelectorAll(".board-row").forEach((f) => f.remove());
  $("#board-empty").hidden = ficheros.length > 0;
  ficheros.forEach((f, i) => {
    const fila = document.createElement("button");
    fila.className = "board-row entra";
    fila.style.animationDelay = `${Math.min(i, 30) * 35}ms`;
    fila.innerHTML = `
      <span class="b-fecha">${f.fecha}</span>
      <span class="b-destino">${PAISES[f.pais] || f.pais}</span>
      <span class="b-pais">${f.pais}</span>
      <span class="b-num">${relleno(f.reservas, 3)}</span>
      <span class="b-fichero">${f.nombre}</span>
      <span class="b-estado">GENERADO</span>`;
    fila.addEventListener("click", () => abrirVisor(f.nombre));
    cuerpo.appendChild(fila);
  });
}

/* ---------------------------------------------------------
   Visor de ficheros: completo o línea a línea
   --------------------------------------------------------- */
function abrirVisor(nombre) {
  visor = { nombre, modo: "total", linea: 0, fin: false };
  $("#viewer-title").textContent = nombre;
  const pais = nombre.split("_")[1];
  $("#viewer-eyebrow").textContent = `Destino · ${PAISES[pais] || pais}`;
  $("#viewer").hidden = false;
  $("#viewer-backdrop").hidden = false;
  cambiarModo("total");
}

function cerrarVisor() {
  $("#viewer").hidden = true;
  $("#viewer-backdrop").hidden = true;
}

async function cambiarModo(modo) {
  visor.modo = modo;
  visor.linea = 0;
  visor.fin = false;
  $("#mode-total").setAttribute("aria-pressed", modo === "total");
  $("#mode-linea").setAttribute("aria-pressed", modo === "linea");
  $("#viewer-foot").hidden = modo !== "linea";
  $("#btn-siguiente").disabled = false;
  const ol = $("#viewer-lines");
  ol.innerHTML = "";
  if (modo === "total") {
    try {
      const lineas = await api(`/api/ficheros/${encodeURIComponent(visor.nombre)}`);
      lineas.forEach((t) => { const li = document.createElement("li"); li.textContent = t; ol.appendChild(li); });
      registrar(`Modalidad total: ${visor.nombre} (${lineas.length} líneas).`);
    } catch (e) { error(e); }
  } else {
    registrar(`Modalidad línea a línea: ${visor.nombre}.`);
    await siguienteLinea();
  }
}

async function siguienteLinea() {
  if (visor.modo !== "linea" || visor.fin) return;
  try {
    const l = await api(`/api/ficheros/${encodeURIComponent(visor.nombre)}/lineas/${visor.linea}`);
    const ol = $("#viewer-lines");
    if (l.texto !== "" || !l.fin) {
      const li = document.createElement("li");
      li.className = "nueva";
      li.textContent = l.texto;
      ol.appendChild(li);
      li.scrollIntoView({ block: "nearest" });
    }
    visor.linea++;
    $("#viewer-progress").textContent = `Línea ${visor.linea}`;
    if (l.fin) {
      visor.fin = true;
      const li = document.createElement("li");
      li.className = "fin";
      li.textContent = "— fin del fichero —";
      ol.appendChild(li);
      $("#btn-siguiente").disabled = true;
    }
  } catch (e) { error(e); }
}

/* ---------------------------------------------------------
   Pestañas
   --------------------------------------------------------- */
function abrirPestana(nombre) {
  for (const t of ["salidas", "pasajeros", "grupos"]) {
    $(`#tab-${t}`).setAttribute("aria-selected", t === nombre);
    $(`#panel-${t}`).hidden = t !== nombre;
  }
}

/* ---------------------------------------------------------
   Arranque
   --------------------------------------------------------- */
function reloj() {
  $("#reloj").textContent = new Date().toLocaleTimeString("es-ES", { hour: "2-digit", minute: "2-digit" });
}

async function iniciar() {
  reloj();
  setInterval(reloj, 10000);

  // Eventos
  $("#btn-defecto").addEventListener("click", cargarPorDefecto);
  $("#btn-generar").addEventListener("click", generarFicheros);
  $("#input-fichero").addEventListener("change", (e) => subirFichero(e.target.files[0]));
  const zona = $("#dropzone");
  ["dragenter", "dragover"].forEach((ev) => zona.addEventListener(ev, (e) => { e.preventDefault(); zona.classList.add("over"); }));
  ["dragleave", "drop"].forEach((ev) => zona.addEventListener(ev, () => zona.classList.remove("over")));
  zona.addEventListener("drop", (e) => { e.preventDefault(); subirFichero(e.dataTransfer.files[0]); });

  $("#sel-orden").addEventListener("change", async () => {
    vista = null;
    try { await cargarReservas(); registrar(`Reservas ordenadas por ${ALGORITMO[$("#sel-orden").value]} con MergeSort.`); }
    catch (e) { error(e); }
  });
  $("#form-id").addEventListener("submit", buscarPorId);
  $("#input-filtro").addEventListener("input", pintarPasajeros);
  $("#btn-todas").addEventListener("click", mostrarTodas);
  $("#tbody-pax").addEventListener("click", (e) => {
    const chip = e.target.closest(".loc-chip");
    if (chip) mostrarLocalizador(chip.dataset.loc);
  });

  ["salidas", "pasajeros", "grupos"].forEach((t) => $(`#tab-${t}`).addEventListener("click", () => abrirPestana(t)));

  $("#viewer-close").addEventListener("click", cerrarVisor);
  $("#viewer-backdrop").addEventListener("click", cerrarVisor);
  $("#mode-total").addEventListener("click", () => cambiarModo("total"));
  $("#mode-linea").addEventListener("click", () => cambiarModo("linea"));
  $("#btn-siguiente").addEventListener("click", siguienteLinea);
  document.addEventListener("keydown", (e) => {
    if ($("#viewer").hidden) return;
    if (e.key === "Escape") cerrarVisor();
    if (e.key === " " && visor.modo === "linea") { e.preventDefault(); siguienteLinea(); }
  });

  $("#tower-toggle").addEventListener("click", () => {
    const log = $("#tower-log");
    log.hidden = !log.hidden;
    $("#tower-toggle").setAttribute("aria-expanded", !log.hidden);
  });

  // Estado inicial (por si el servidor ya tenía datos cargados)
  try {
    const estado = await api("/api/estado");
    pintarEstado(estado);
    if (estado.reservas > 0) await Promise.all([cargarReservas(), cargarGrupos()]);
    else pintarPasajeros();
    await cargarSalidas();
    registrar(estado.reservas > 0
      ? `Sistema listo. ${estado.reservas} reservas cargadas de «${estado.fichero}».`
      : "Sistema listo. Carga un CSV para empezar.");
  } catch (e) {
    registrar("No se puede conectar con el servidor. ¿Está arrancado?", "err");
  }
}

iniciar();
