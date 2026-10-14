/* =========================================================
   Tortilla de patatas 3D (Three.js), servida en un plato y con
   un trozo cortado para ver el interior. Las texturas (costra
   dorada e interior con capas de patata) se pintan con canvas.
   Se dibuja dentro del elemento #tortilla. Arrastrar para girar.
   ========================================================= */
(function () {
  const cont = document.getElementById("tortilla");
  if (!cont || typeof THREE === "undefined") return;

  // ---------- Texturas pintadas a mano con canvas ----------
  // Generador pseudoaleatorio con semilla (la tortilla sale igual siempre)
  let semilla = 7;
  const azar = () => ((semilla = (semilla * 16807) % 2147483647) / 2147483647);

  /** Costra: dorado con zonas tostadas, burbujas y brillo de aceite. */
  function texturaCostra() {
    const c = document.createElement("canvas");
    c.width = 1024; c.height = 512;
    const ctx = c.getContext("2d");
    ctx.fillStyle = "#c98b2c";
    ctx.fillRect(0, 0, c.width, c.height);
    // Manchas tostadas de distintos tamaños
    for (let i = 0; i < 900; i++) {
      const x = azar() * c.width, y = azar() * c.height, r = 4 + azar() * 38;
      const g = ctx.createRadialGradient(x, y, 0, x, y, r);
      const tono = azar();
      const color = tono < 0.45 ? "150,82,24" : tono < 0.8 ? "196,128,44" : "236,190,92";
      g.addColorStop(0, `rgba(${color},${0.25 + azar() * 0.4})`);
      g.addColorStop(1, `rgba(${color},0)`);
      ctx.fillStyle = g;
      ctx.beginPath(); ctx.arc(x, y, r, 0, Math.PI * 2); ctx.fill();
    }
    // Zonas muy hechas (casi marrón oscuro)
    for (let i = 0; i < 120; i++) {
      const x = azar() * c.width, y = azar() * c.height, r = 2 + azar() * 9;
      ctx.fillStyle = `rgba(110,55,15,${0.25 + azar() * 0.35})`;
      ctx.beginPath(); ctx.ellipse(x, y, r * 1.6, r, azar() * 3, 0, Math.PI * 2); ctx.fill();
    }
    // Burbujitas brillantes
    for (let i = 0; i < 500; i++) {
      const x = azar() * c.width, y = azar() * c.height, r = 0.8 + azar() * 2.4;
      ctx.fillStyle = `rgba(255,230,160,${0.25 + azar() * 0.5})`;
      ctx.beginPath(); ctx.arc(x, y, r, 0, Math.PI * 2); ctx.fill();
    }
    return c;
  }

  /** Interior: huevo cuajado con láminas de patata superpuestas y algo de cebolla. */
  function texturaInterior() {
    const c = document.createElement("canvas");
    c.width = 512; c.height = 256;
    const ctx = c.getContext("2d");
    ctx.fillStyle = "#f0cd5c";
    ctx.fillRect(0, 0, c.width, c.height);
    // Huevo jugoso con variaciones
    for (let i = 0; i < 300; i++) {
      const x = azar() * c.width, y = azar() * c.height, r = 6 + azar() * 24;
      const g = ctx.createRadialGradient(x, y, 0, x, y, r);
      g.addColorStop(0, `rgba(${azar() < 0.5 ? "232,182,60" : "250,222,120"},0.45)`);
      g.addColorStop(1, "rgba(240,205,92,0)");
      ctx.fillStyle = g;
      ctx.fillRect(x - r, y - r, r * 2, r * 2);
    }
    // Láminas de patata: elipses alargadas casi horizontales, en capas
    for (let capa = 0; capa < 9; capa++) {
      const yBase = 30 + capa * 24;
      for (let i = 0; i < 9; i++) {
        const x = azar() * c.width, y = yBase + (azar() - 0.5) * 16;
        const w = 30 + azar() * 60, h = 5 + azar() * 6;
        ctx.save();
        ctx.translate(x, y);
        ctx.rotate((azar() - 0.5) * 0.35);
        ctx.fillStyle = `rgba(${248 - azar() * 12},${234 - azar() * 18},${170 - azar() * 30},0.95)`;
        ctx.beginPath(); ctx.ellipse(0, 0, w, h, 0, 0, Math.PI * 2); ctx.fill();
        ctx.strokeStyle = "rgba(200,160,70,0.5)";
        ctx.lineWidth = 1.2;
        ctx.stroke();
        ctx.restore();
      }
    }
    // Cebolla pochada: tiras finas translúcidas
    for (let i = 0; i < 40; i++) {
      const x = azar() * c.width, y = 20 + azar() * (c.height - 40);
      ctx.strokeStyle = `rgba(255,248,215,${0.4 + azar() * 0.3})`;
      ctx.lineWidth = 1.5 + azar() * 1.5;
      ctx.beginPath();
      ctx.moveTo(x, y);
      ctx.quadraticCurveTo(x + 10 + azar() * 10, y - 6 + azar() * 12, x + 20 + azar() * 20, y + (azar() - 0.5) * 8);
      ctx.stroke();
    }
    // Costra arriba y abajo del corte
    const borde = (y0, y1) => {
      const g = ctx.createLinearGradient(0, y0, 0, y1);
      g.addColorStop(0, "rgba(176,104,32,1)");
      g.addColorStop(1, "rgba(176,104,32,0)");
      ctx.fillStyle = g;
      ctx.fillRect(0, Math.min(y0, y1), c.width, Math.abs(y1 - y0));
    };
    borde(0, 18);
    borde(c.height, c.height - 18);
    return c;
  }

  // ---------- Escena ----------
  const escena = new THREE.Scene();
  const camara = new THREE.PerspectiveCamera(30, 1, 0.1, 50);
  camara.position.set(0, 2.0, 3.2);
  camara.lookAt(0, -0.05, 0);
  const render = new THREE.WebGLRenderer({ antialias: true, alpha: true });
  render.setPixelRatio(Math.min(window.devicePixelRatio, 2));
  render.outputEncoding = THREE.sRGBEncoding;
  render.toneMapping = THREE.ACESFilmicToneMapping;
  render.toneMappingExposure = 1.05;
  cont.appendChild(render.domElement);

  // Sin mapa de entorno (PMREM falla en algunas gráficas): los brillos salen de las luces
  const env = null;

  escena.add(new THREE.HemisphereLight(0xfff3e0, 0x3a2a1a, 0.6));
  const foco = new THREE.DirectionalLight(0xffe2b8, 1.9);
  foco.position.set(-2.5, 4, 2.5);
  escena.add(foco);
  const contra = new THREE.DirectionalLight(0x9fc0ff, 0.9);
  const brilloPlato = new THREE.PointLight(0xffffff, 0.8, 8);
  brilloPlato.position.set(1.5, 2.2, 2.5);
  escena.add(brilloPlato);
  contra.position.set(3, 2, -3);
  escena.add(contra);

  const conjunto = new THREE.Group();
  escena.add(conjunto);

  // ---------- Plato ----------
  const perfilPlato = [[0, 0], [1.05, 0], [1.25, 0.03], [1.45, 0.12], [1.55, 0.16], [1.57, 0.14], [1.47, 0.08], [1.27, 0.0], [1.05, -0.04], [0, -0.04]]
    .map(([r, y]) => new THREE.Vector2(r, y));
  const plato = new THREE.Mesh(new THREE.LatheGeometry(perfilPlato, 96),
    new THREE.MeshStandardMaterial({ color: 0xf7f5f0, roughness: 0.18, metalness: 0.05, envMap: env, side: THREE.DoubleSide }));
  plato.position.y = -0.27;
  conjunto.add(plato);
  const filete = new THREE.Mesh(new THREE.TorusGeometry(1.5, 0.008, 8, 128),
    new THREE.MeshStandardMaterial({ color: 0xffb627, metalness: 0.9, roughness: 0.25, envMap: env }));
  filete.rotation.x = Math.PI / 2;
  filete.position.y = -0.135;
  conjunto.add(filete);

  // ---------- Tortilla ----------
  // Perfil: de arriba (centro) al borde redondeado y a la base
  const R = 1, H = 0.24;
  const perfil = [];
  perfil.push(new THREE.Vector2(0, H + 0.03));
  for (let i = 1; i <= 8; i++) perfil.push(new THREE.Vector2((R - 0.2) * i / 8, H + 0.03 * (1 - (i / 8) ** 2)));
  for (let i = 1; i <= 14; i++) { // borde redondeado
    const a = Math.PI / 2 - (i / 14) * Math.PI;
    perfil.push(new THREE.Vector2(R - 0.2 + 0.2 * Math.cos(a), H * Math.sin(a) * 1.0));
  }
  for (let i = 1; i <= 6; i++) perfil.push(new THREE.Vector2((R - 0.2) * (1 - i / 6), -H));

  const costra = new THREE.CanvasTexture(texturaCostra());
  costra.wrapS = costra.wrapT = THREE.RepeatWrapping;
  costra.encoding = THREE.sRGBEncoding;
  costra.anisotropy = render.capabilities.getMaxAnisotropy();
  const interior = new THREE.CanvasTexture(texturaInterior());
  interior.encoding = THREE.sRGBEncoding;

  const matCostra = new THREE.MeshPhysicalMaterial({
    map: costra, bumpMap: costra, bumpScale: 0.012, roughness: 0.45, metalness: 0,
    clearcoat: 0.35, clearcoatRoughness: 0.35, envMap: env, envMapIntensity: 0.6
  });
  const matInterior = new THREE.MeshStandardMaterial({
    map: interior, bumpMap: interior, bumpScale: 0.006, roughness: 0.55, envMap: env, envMapIntensity: 0.4, side: THREE.DoubleSide
  });

  /** Cara del corte: la sección del perfil, colocada en el ángulo indicado. */
  function caraCorte(angulo) {
    const s = new THREE.Shape();
    s.moveTo(0, perfil[0].y);
    perfil.forEach((p) => s.lineTo(p.x, p.y));
    s.lineTo(0, perfil[perfil.length - 1].y);
    const g = new THREE.ShapeGeometry(s, 24);
    // Coordenadas UV para que la textura interior cubra la cara
    const pos = g.attributes.position, uv = g.attributes.uv;
    for (let i = 0; i < pos.count; i++) uv.setXY(i, pos.getX(i) / R, (pos.getY(i) + H) / (2 * H + 0.03));
    g.rotateY(angulo - Math.PI / 2);
    return new THREE.Mesh(g, matInterior);
  }

  /** Una porción de tortilla entre dos ángulos, con sus dos caras de corte. */
  function porcion(desde, abertura) {
    const grupo = new THREE.Group();
    grupo.add(new THREE.Mesh(new THREE.LatheGeometry(perfil, 128, desde, abertura), matCostra));
    grupo.add(caraCorte(desde));
    grupo.add(caraCorte(desde + abertura));
    return grupo;
  }

  const HUECO = 0.95; // ángulo del trozo cortado
  const tortilla = porcion(HUECO / 2, Math.PI * 2 - HUECO);
  conjunto.add(tortilla);
  // El trozo servido, un poco separado
  const trozo = porcion(-HUECO / 2, HUECO);
  trozo.position.set(0, 0.0, 0.42);
  trozo.rotation.y = 0.12;
  conjunto.add(trozo);

  // Sombra de contacto bajo el plato
  const cs = document.createElement("canvas");
  cs.width = cs.height = 128;
  const cctx = cs.getContext("2d");
  const grad = cctx.createRadialGradient(64, 64, 10, 64, 64, 64);
  grad.addColorStop(0, "rgba(0,0,0,0.55)");
  grad.addColorStop(1, "rgba(0,0,0,0)");
  cctx.fillStyle = grad;
  cctx.fillRect(0, 0, 128, 128);
  const sombra = new THREE.Mesh(new THREE.PlaneGeometry(4, 4), new THREE.MeshBasicMaterial({ map: new THREE.CanvasTexture(cs), transparent: true, depthWrite: false }));
  sombra.rotation.x = -Math.PI / 2;
  sombra.position.y = -0.32;
  escena.add(sombra);

  // ---------- Vapor ----------
  const cv = document.createElement("canvas");
  cv.width = cv.height = 64;
  const vctx = cv.getContext("2d");
  const vg = vctx.createRadialGradient(32, 32, 0, 32, 32, 32);
  vg.addColorStop(0, "rgba(255,255,255,0.5)");
  vg.addColorStop(1, "rgba(255,255,255,0)");
  vctx.fillStyle = vg;
  vctx.fillRect(0, 0, 64, 64);
  const texVapor = new THREE.CanvasTexture(cv);
  const vapores = [];
  for (let i = 0; i < 14; i++) {
    const s = new THREE.Sprite(new THREE.SpriteMaterial({ map: texVapor, transparent: true, depthWrite: false, opacity: 0 }));
    s.userData = { fase: i / 14, x: (azar() - 0.5) * 1.2, z: (azar() - 0.5) * 1.2 };
    escena.add(s);
    vapores.push(s);
  }

  // ---------- Interacción y animación ----------
  let arrastrando = false, ultX = 0, inercia = 0;
  cont.addEventListener("pointerdown", (e) => { arrastrando = true; ultX = e.clientX; cont.setPointerCapture(e.pointerId); });
  cont.addEventListener("pointerup", () => { arrastrando = false; });
  cont.addEventListener("pointermove", (e) => {
    if (!arrastrando) return;
    inercia = (e.clientX - ultX) * 0.01;
    conjunto.rotation.y += inercia;
    ultX = e.clientX;
  });

  function ajustar() {
    const w = cont.clientWidth, h = cont.clientHeight;
    if (!w || !h) return;
    render.setSize(w, h, false);
    camara.aspect = w / h;
    camara.updateProjectionMatrix();
  }
  new ResizeObserver(ajustar).observe(cont);
  ajustar();

  const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  conjunto.rotation.y = 0.5;
  let t = 0;
  function animar() {
    t += 0.016;
    if (!arrastrando && !reduce) {
      inercia *= 0.94;
      conjunto.rotation.y += 0.004 + inercia;
    }
    // El vapor sube, se ensancha y se desvanece
    vapores.forEach((s) => {
      const f = (t * 0.18 + s.userData.fase) % 1;
      s.position.set(s.userData.x + Math.sin(t + s.userData.fase * 9) * 0.1, 0.35 + f * 1.4, s.userData.z);
      s.scale.setScalar(0.35 + f * 0.9);
      s.material.opacity = reduce ? 0 : Math.sin(f * Math.PI) * 0.22;
    });
    render.render(escena, camara);
    requestAnimationFrame(animar);
  }
  animar();
})();
